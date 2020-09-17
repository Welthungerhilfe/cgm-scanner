package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactList;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_FLEXTIME;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_INTERVAL;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String CHANNEL_ID = "CGM_Result_Generation_Notification";

    private static final Object lock = new Object();

    private long prevTimestamp;
    private long currentTimestamp;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;
    private DeviceRepository deviceRepository;
    private MeasureResultRepository measureResultRepository;

    private SessionManager session;
    private AsyncTask<Void, Void, Void> syncTask;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
        fileLogRepository = FileLogRepository.getInstance(context);
        deviceRepository = DeviceRepository.getInstance(context);
        measureResultRepository = MeasureResultRepository.getInstance(context);

        session = new SessionManager(context);
    }

    public static Object getLock() {
        return lock;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        UploadService.forceResume();

        startSyncing();
    }

    private synchronized void startSyncing() {
        if (syncTask == null) {
            prevTimestamp = session.getSyncTimestamp();
            currentTimestamp = System.currentTimeMillis();

            syncTask = new SyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private static void syncImmediately(Account account, Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, context.getString(R.string.sync_authority), bundle);
    }

    private static void configurePeriodicSync(Account account, Context context) {

        String authority = context.getString(R.string.sync_authority);

        SyncRequest request = new SyncRequest.Builder().
                syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME).
                setSyncAdapter(account, authority).
                setExtras(new Bundle()).build();

        ContentResolver.requestSync(request);
    }

    public static void startPeriodicSync(Account newAccount, Context context) {

        if (!AppController.getInstance().isUploadRunning()) {
            context.startService(new Intent(context, UploadService.class));
        }

        configurePeriodicSync(newAccount, context);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);

    }

    @SuppressLint("StaticFieldLeak")
    class SyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            boolean wifiOnly = LocalPersistency.getBoolean(getContext(), SettingsActivity.KEY_UPLOAD_WIFI);
            if (wifiOnly && !Utils.isWifiConnected(getContext())) {
                Log.d("SyncAdapter", "skipped due to missing WiFi connection");
                return null;
            }

            if (!Utils.isNetworkAvailable(getContext())) {
                Log.d("SyncAdapter", "skipped due to missing network connection");
                return null;
            }

            Log.d("SyncAdapter", "start updating");
            synchronized (getLock()) {
                try {
                    CloudStorageAccount storageAccount = CloudStorageAccount.parse(AppController.getInstance().getAzureConnection());
                    CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

                    processMeasureResultQueue(queueClient);
                    processPersonQueue(queueClient);
                    processMeasureQueue(queueClient);
                    processDeviceQueue(queueClient);
                    processCachedMeasures();

                    session.setSyncTimestamp(currentTimestamp);
                } catch (URISyntaxException | InvalidKeyException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            Log.d("SyncAdapter", "end updating");
            syncTask = null;
            return null;
        }

        private String getQrCode(String measureId) {
            String qrCode = measureId;
            try {
                qrCode = measureRepository.getMeasureById(measureId).getQrCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return qrCode;
        }

        private void processMeasureResultQueue(CloudQueueClient queueClient) throws URISyntaxException {
            HashMap<String, Pair<Float, Long>> heightNotification = new HashMap<>();
            HashMap<String, Pair<Float, Long>> weightNotification = new HashMap<>();

            try {
                CloudQueue measureResultQueue = queueClient.getQueueReference(Utils.getAndroidID(getContext().getContentResolver()) + "-measure-result");

                if (measureResultQueue.exists()) {
                    Iterable<CloudQueueMessage> retrievedMessages;

                    measureResultQueue.setShouldEncodeMessage(false);
                    retrievedMessages = measureResultQueue.retrieveMessages(30);
                    Gson gson = new Gson();

                    Iterator<CloudQueueMessage> iterator = retrievedMessages.iterator();
                    if (iterator.hasNext()) {
                        Log.d("SyncAdapter", "has at least one message");
                    }
                    while (iterator.hasNext()) {
                        CloudQueueMessage message = iterator.next();

                        try {
                            String messageStr = message.getMessageContentAsString();
                            Log.d("SyncAdapter", messageStr);
                            MeasureResult result = gson.fromJson(messageStr, MeasureResult.class);

                            float keyMaxConfident = measureResultRepository.getConfidence(result.getMeasure_id(), result.getKey());
                            if (result.getConfidence_value() > keyMaxConfident) {
                                measureResultRepository.insertMeasureResult(result);
                            }

                            Pair<Float, Long> value = new Pair<>(result.getFloat_value(), System.currentTimeMillis());
                            if (result.getKey().contains("weight")) {
                                float fieldMaxConfidence = measureResultRepository.getMaxConfidence(result.getMeasure_id(), "weight%");

                                if (result.getConfidence_value() >= fieldMaxConfidence) {
                                    JsonObject object = new Gson().fromJson(result.getJson_value(), JsonObject.class);
                                    long timestamp = object.get("timestamp").getAsLong();

                                    Measure measure = measureRepository.getMeasureById(result.getMeasure_id());
                                    if (measure != null) {
                                        measure.setWeight(result.getFloat_value());
                                        measure.setWeightConfidence(result.getConfidence_value());
                                        measure.setResulted_at(timestamp);
                                        measure.setReceived_at(System.currentTimeMillis());
                                        measureRepository.updateMeasure(measure);

                                        if ((measure.getHeight() > 0) && (measure.getWeight() > 0)) {
                                            onResultReceived(result);
                                        }

                                        weightNotification.remove(result.getMeasure_id());
                                        weightNotification.put(result.getMeasure_id(), value);
                                    }
                                } else if (!weightNotification.containsKey(result.getMeasure_id())) {
                                    weightNotification.put(result.getMeasure_id(), value);
                                }
                            } else if (result.getKey().contains("height")) {
                                float fieldMaxConfidence = measureResultRepository.getMaxConfidence(result.getMeasure_id(), "height%");

                                if (result.getConfidence_value() >= fieldMaxConfidence) {
                                    JsonObject object = new Gson().fromJson(result.getJson_value(), JsonObject.class);
                                    long timestamp = object.get("timestamp").getAsLong();

                                    Measure measure = measureRepository.getMeasureById(result.getMeasure_id());
                                    if (measure != null) {
                                        measure.setHeight(result.getFloat_value());
                                        measure.setHeightConfidence(result.getConfidence_value());
                                        measure.setResulted_at(timestamp);
                                        measure.setReceived_at(System.currentTimeMillis());
                                        measureRepository.updateMeasure(measure);

                                        if ((measure.getHeight() > 0) && (measure.getWeight() > 0)) {
                                            onResultReceived(result);
                                        }

                                        heightNotification.remove(result.getMeasure_id());
                                        heightNotification.put(result.getMeasure_id(), value);
                                    }
                                } else if (!heightNotification.containsKey(result.getMeasure_id())) {
                                    heightNotification.put(result.getMeasure_id(), value);
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }

                        measureResultQueue.deleteMessage(message);
                    }
                }
            } catch (StorageException e) {
                e.printStackTrace();
            }

            int notificationID = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(new Date()));
            for (String id : heightNotification.keySet()) {
                Pair<Float, Long> value = heightNotification.get(id);
                if (value != null) {
                    showNotification(getContext(), notificationID++, getQrCode(id), "height", value.first, value.second);
                }
            }
            for (String id : weightNotification.keySet()) {
                Pair<Float, Long> value = weightNotification.get(id);
                if (value != null) {
                    showNotification(getContext(), notificationID++, getQrCode(id), "weight", value.first, value.second);
                }
            }
        }

        private void processPersonQueue(CloudQueueClient queueClient) throws URISyntaxException {
            try {
                CloudQueue personQueue = queueClient.getQueueReference("person");
                personQueue.createIfNotExists();

                Gson gson = new Gson();
                List<Person> syncablePersons = personRepository.getSyncablePerson(prevTimestamp);
                for (int i = 0; i < syncablePersons.size(); i++) {
                    String content = gson.toJson(syncablePersons.get(i));
                    CloudQueueMessage message = new CloudQueueMessage(syncablePersons.get(i).getId());
                    message.setMessageContent(content);

                    personQueue.addMessage(message);

                    syncablePersons.get(i).setTimestamp(prevTimestamp);

                    personRepository.updatePerson(syncablePersons.get(i));
                }
            } catch (StorageException e) {
                currentTimestamp = prevTimestamp;
            }
        }

        private void processMeasureQueue(CloudQueueClient queueClient) throws URISyntaxException {
            try {
                CloudQueue measureQueue = queueClient.getQueueReference("measure");
                measureQueue.createIfNotExists();

                Gson gson = new Gson();
                List<Measure> syncableMeasures = measureRepository.getSyncableMeasure(prevTimestamp);
                for (int i = 0; i < syncableMeasures.size(); i++) {
                    String content = gson.toJson(syncableMeasures.get(i));
                    CloudQueueMessage message = new CloudQueueMessage(syncableMeasures.get(i).getId());
                    message.setMessageContent(content);

                    measureQueue.addMessage(message);

                    if (!syncableMeasures.get(i).isArtifact_synced()) {
                        CloudQueue measureArtifactsQueue = queueClient.getQueueReference("artifact-list");
                        measureArtifactsQueue.createIfNotExists();

                        long totalNumbers  = fileLogRepository.getTotalArtifactCountForMeasure(syncableMeasures.get(i).getId());
                        final int size = 50;
                        int offset = 0;

                        while (offset + 1 < totalNumbers) {
                            List<FileLog> measureArtifacts = fileLogRepository.getArtifactsForMeasure(syncableMeasures.get(i).getId(), offset, size);

                            ArtifactList artifactList = new ArtifactList();
                            artifactList.setMeasure_id(syncableMeasures.get(i).getId());
                            artifactList.setStart(offset + 1);
                            artifactList.setEnd(offset + measureArtifacts.size());
                            artifactList.setArtifacts(measureArtifacts);
                            artifactList.setTotal(totalNumbers);

                            offset += measureArtifacts.size();

                            CloudQueueMessage measureArtifactsMessage = new CloudQueueMessage(syncableMeasures.get(i).getId());
                            measureArtifactsMessage.setMessageContent(gson.toJson(artifactList));
                            measureArtifactsQueue.addMessage(measureArtifactsMessage);
                        }

                        syncableMeasures.get(i).setUploaded_at(System.currentTimeMillis());
                        syncableMeasures.get(i).setArtifact_synced(true);
                    }

                    syncableMeasures.get(i).setTimestamp(prevTimestamp);
                    measureRepository.updateMeasure(syncableMeasures.get(i));
                }
            } catch (StorageException e) {
                currentTimestamp = prevTimestamp;
            }
        }

        private void processDeviceQueue(CloudQueueClient queueClient) throws URISyntaxException {
            try {
                CloudQueue deviceQueue = queueClient.getQueueReference("device");
                deviceQueue.createIfNotExists();

                Gson gson = new Gson();
                List<Device> syncableDevices = deviceRepository.getSyncableDevice(prevTimestamp);
                for (int i = 0; i < syncableDevices.size(); i++) {
                    String content = gson.toJson(syncableDevices.get(i));
                    CloudQueueMessage message = new CloudQueueMessage(syncableDevices.get(i).getId());
                    message.setMessageContent(content);

                    deviceQueue.addMessage(message);

                    syncableDevices.get(i).setSync_timestamp(prevTimestamp);

                    deviceRepository.updateDevice(syncableDevices.get(i));
                }
            } catch (StorageException e) {
                currentTimestamp = prevTimestamp;
            }
        }

        private void processCachedMeasures() {

            Context context = getContext();
            long measureCount = LocalPersistency.getLong(context, ScanModeActivity.KEY_MEASURE + ScanModeActivity.SUBFIX_COUNT);
            for (long i = 0; i < measureCount; i++) {
                try {
                    String id = LocalPersistency.getString(context, ScanModeActivity.KEY_MEASURE + i);
                    Measure measure = measureRepository.getMeasureById(id);
                    measureRepository.uploadMeasure(context, measure);
                } catch (Exception e) {
                }
            }
            LocalPersistency.setLong(context, ScanModeActivity.KEY_MEASURE + ScanModeActivity.SUBFIX_COUNT, 0);
        }

        private void onResultReceived(MeasureResult result) {

            Context c = getContext();
            if (!LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT))
                return;
            if (LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID).compareTo(result.getMeasure_id()) != 0)
                return;

            if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_RECEIVE) == 0) {

                //set receive timestamp
                LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_RECEIVE, System.currentTimeMillis());

                //update average time
                long diff = 0;
                diff += LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_RECEIVE);
                diff -= LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_SCAN);
                ArrayList<Long> last = LocalPersistency.getLongArray(c, SettingsPerformanceActivity.KEY_TEST_RESULT_AVERAGE);
                last.add(diff);
                if (last.size() > 10) {
                    last.remove(0);
                }
                LocalPersistency.setLongArray(c, SettingsPerformanceActivity.KEY_TEST_RESULT_AVERAGE, last);
            }
        }
    }


    private void showNotification(Context context, int notificationID, String qrCode, String type, double value, long timestamp) {
        Notification.Builder notificationBuilder;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "CGM Result Generation", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new Notification.Builder(context, CHANNEL_ID);

        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        notificationBuilder = notificationBuilder
                .setSmallIcon(R.drawable.icon_notif)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0, 500, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOngoing(false);

        notificationBuilder.setContentTitle(String.format(context.getString(R.string.result_generation_at) + " %s", DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE, timestamp)));
        notificationBuilder.setContentText(String.format(Locale.US, "%s " + context.getString(R.string.result_for) + " %s : %.2f%s", type, qrCode, value, type.equals("weight") ? "kg" : "cm"));

        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
