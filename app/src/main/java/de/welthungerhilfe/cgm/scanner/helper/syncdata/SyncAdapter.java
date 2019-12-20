package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.common.collect.Iterables;
import com.google.firebase.perf.metrics.AddTrace;

import com.google.gson.Gson;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
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
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnDevicesLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasuresLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonsLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_FLEXTIME;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_INTERVAL;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements OnPersonsLoad, OnMeasuresLoad, OnFileLogsLoad, OnDevicesLoad {
    private long prevTimestamp;

    private CloudQueue personQueue;
    private CloudQueue measureQueue;
    private CloudQueue artifactQueue;
    private CloudQueue deviceQueue;
    private CloudQueue measureResultQueue;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;
    private DeviceRepository deviceRepository;
    private MeasureResultRepository measureResultRepository;

    private SessionManager session;

    private List<Person> personList = new ArrayList<>();
    private List<Measure> measureList = new ArrayList<>();
    private List<FileLog> fileLogList = new ArrayList<>();
    private List<Device> deviceList = new ArrayList<>();

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
        fileLogRepository = FileLogRepository.getInstance(context);
        deviceRepository = DeviceRepository.getInstance(context);
        measureResultRepository = MeasureResultRepository.getInstance(context);

        session = new SessionManager(context);
    }

    @Override
    @AddTrace(name = "onPerformSync", enabled = true)
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        if (!isServiceRunning(UploadService.class)) {
            getContext().startService(new Intent(getContext(), UploadService.class));
        }

        if (personList.size() == 0 && measureList.size() == 0 && fileLogList.size() == 0 && deviceList.size() == 0)
            new MessageTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void startSyncing() {
        /*
        prevTimestamp = session.getSyncTimestamp();

        personRepository.getSyncablePerson(this, prevTimestamp);
        measureRepository.getSyncableMeasure(this, prevTimestamp);
        fileLogRepository.getSyncableLog(this, prevTimestamp);
        deviceRepository.getSyncablePerson(this, prevTimestamp);
         */

        try {
            if (measureResultQueue.exists()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Iterable<CloudQueueMessage> retrievedMessages;

                        try {
                            retrievedMessages = measureResultQueue.retrieveMessages(30);

                            Gson gson = new Gson();

                            if (retrievedMessages.iterator().hasNext()) {
                                CloudQueueMessage message = retrievedMessages.iterator().next();

                                String content = message.getMessageContentAsString();
                                MeasureResult result = gson.fromJson(content, MeasureResult.class);

                                measureResultRepository.insertMeasureResult(result);
                            }
                        } catch (StorageException e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    @AddTrace(name = "syncImmediately", enabled = true)
    private static void syncImmediately(Account account, Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, context.getString(R.string.sync_authority), bundle);
    }

    @AddTrace(name = "configurePeriodicSync", enabled = true)
    private static void configurePeriodicSync(Account account, Context context) {

        String authority = context.getString(R.string.sync_authority);

        SyncRequest request = new SyncRequest.Builder().
                syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME).
                setSyncAdapter(account, authority).
                setExtras(new Bundle()).build();

        ContentResolver.requestSync(request);
    }

    @AddTrace(name = "startPeriodicSync", enabled = true)
    public static void startPeriodicSync(Account newAccount, Context context) {

        configurePeriodicSync(newAccount, context);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);

    }

    @AddTrace(name = "startImmediateSync", enabled = true)
    public static void startImmediateSync(Account newAccount, Context context) {

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);
    }

    @Override
    @AddTrace(name = "onPersonLoaded", enabled = true)
    public void onPersonsLoaded(List<Person> pList) {
        personList = pList;

        for (int i = 0; i < personList.size(); i++) {
            new PersonWriteTask(personList.get(i)).execute();
        }
    }

    @Override
    @AddTrace(name = "onMeasureLoaded", enabled = true)
    public void onMeasuresLoaded(List<Measure> mList) {
        measureList = mList;

        for (int i = 0; i < measureList.size(); i++) {
            new MeasureWriteTask(measureList.get(i)).execute();
        }
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> fList) {
        fileLogList = fList;

        for (int i = 0; i < fileLogList.size(); i++) {
            new ArtifactWriteTask(fileLogList.get(i)).execute();
        }
    }

    @Override
    public void onDevicesLoaded(List<Device> dList) {
        deviceList = dList;

        for (int i = 0; i < deviceList.size(); i++) {
            new DeviceWriteTask(deviceList.get(i)).execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class MessageTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(AppController.getInstance().getAzureConnection());
                CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

                personQueue = queueClient.getQueueReference("person");
                personQueue.createIfNotExists();

                measureQueue = queueClient.getQueueReference("measure");
                measureQueue.createIfNotExists();

                artifactQueue = queueClient.getQueueReference("artifact");
                artifactQueue.createIfNotExists();

                deviceQueue = queueClient.getQueueReference("device");
                deviceQueue.createIfNotExists();

                // measureResultQueue = queueClient.getQueueReference(Utils.getAndroidID(getContext().getContentResolver()) + "-measure-result");
                measureResultQueue = queueClient.getQueueReference("bbd1783abcb8d42f-measure-result");

                return true;
            } catch (StorageException | URISyntaxException | InvalidKeyException e) {
                e.printStackTrace();

                return false;
            }
        }

        public void onPostExecute(Boolean result) {
            if (result)
                startSyncing();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class PersonWriteTask extends AsyncTask<Void, Void, Boolean> {
        private Person person;

        PersonWriteTask(Person person) {
            this.person = person;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Gson gson = new Gson();

                String content = gson.toJson(person);
                CloudQueueMessage message = new CloudQueueMessage(person.getId());
                message.setMessageContent(content);

                personQueue.addMessage(message);

                return true;
            } catch (StorageException e) {
                e.printStackTrace();

                return false;
            }
        }

        public void onPostExecute(Boolean result) {
            if (result) {
                session.setSyncTimestamp(person.getTimestamp());

                personList.remove(person);

                person.setTimestamp(0);
                personRepository.updatePerson(person);
            } else {
                session.setSyncTimestamp(prevTimestamp);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class MeasureWriteTask extends AsyncTask<Void, Void, Boolean> {
        private Measure measure;

        MeasureWriteTask(Measure measure) {
            this.measure = measure;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Gson gson = new Gson();

                String content = gson.toJson(measure);
                CloudQueueMessage message = new CloudQueueMessage(measure.getId());

                message.setMessageContent(content);

                measureQueue.addMessage(message);

                return true;
            } catch (StorageException e) {
                e.printStackTrace();

                return false;
            }
        }

        public void onPostExecute(Boolean result) {
            if (result) {
                session.setSyncTimestamp(measure.getTimestamp());

                measureList.remove(measure);

                measure.setTimestamp(0);
                measureRepository.updateMeasure(measure);
            } else {
                session.setSyncTimestamp(prevTimestamp);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class ArtifactWriteTask extends AsyncTask<Void, Void, Boolean> {
        private FileLog artifact;

        ArtifactWriteTask(FileLog artifact) {
            this.artifact = artifact;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Gson gson = new Gson();

                String content = gson.toJson(artifact);
                CloudQueueMessage message = new CloudQueueMessage(artifact.getId());
                message.setMessageContent(content);

                artifactQueue.addMessage(message);

                return true;
            } catch (StorageException e) {
                e.printStackTrace();

                return false;
            }
        }

        public void onPostExecute(Boolean result) {
            if (result) {
                session.setSyncTimestamp(artifact.getCreateDate());

                fileLogList.remove(artifact);
            } else {
                session.setSyncTimestamp(prevTimestamp);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class DeviceWriteTask extends AsyncTask<Void, Void, Boolean> {
        private Device device;

        DeviceWriteTask(Device device) {
            this.device = device;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Gson gson = new Gson();

                String content = gson.toJson(device);
                CloudQueueMessage message = new CloudQueueMessage(device.getId());
                message.setMessageContent(content);

                deviceQueue.addMessage(message);

                return true;
            } catch (StorageException e) {
                e.printStackTrace();

                return false;
            }
        }

        public void onPostExecute(Boolean result) {
            if (result) {
                session.setSyncTimestamp(device.getSync_timestamp());

                deviceList.remove(device);

                device.setSync_timestamp(0);
                deviceRepository.updateDevice(device);
            } else {
                session.setSyncTimestamp(prevTimestamp);
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
