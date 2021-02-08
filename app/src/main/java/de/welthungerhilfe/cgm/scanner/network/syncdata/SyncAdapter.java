/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.syncdata;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.utils.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.network.service.ApiService;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.RequestBody;
import retrofit2.Retrofit;

import static de.welthungerhilfe.cgm.scanner.AppConstants.SYNC_FLEXTIME;
import static de.welthungerhilfe.cgm.scanner.AppConstants.SYNC_INTERVAL;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Object lock = new Object();

    private String TAG = SyncAdapter.class.getSimpleName();

    private Integer activeThreads;
    private boolean updated;

    private long prevTimestamp;
    private long currentTimestamp;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private DeviceRepository deviceRepository;
    private FileLogRepository fileLogRepository;
    private MeasureResultRepository measureResultRepository;

    private SessionManager session;
    private AsyncTask<Void, Void, Void> syncTask;

    private Retrofit retrofit;

    public SyncAdapter(Context context, boolean autoInitialize, Retrofit retrofit) {
        super(context, autoInitialize);
        this.retrofit = retrofit;
        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
        deviceRepository = DeviceRepository.getInstance(context);
        fileLogRepository = FileLogRepository.getInstance(context);
        measureResultRepository = MeasureResultRepository.getInstance(context);

        activeThreads = 0;
        session = new SessionManager(context);
    }

    public static Object getLock() {
        return lock;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "this is inside onPerformSync");

        initUploadService();
        startSyncing();
    }

    private void initUploadService() {
        if (!UploadService.isInitialized()) {
            try {
                getContext().startService(new Intent(getContext(), UploadService.class));

            } catch (IllegalStateException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(getContext(), UploadService.class);
                    intent.putExtra(AppConstants.IS_FOREGROUND, true);
                    getContext().startForegroundService(intent);
                }
            }
        } else {
            UploadService.forceResume();
        }
    }

    private synchronized void startSyncing() {
        Log.i(TAG, "this is inside startSyncing ");

        if (!session.isSigned()) {
            return;
        }

        synchronized (activeThreads) {
            if (activeThreads > 0) {
                return;
            }
        }

        if (syncTask == null) {
            prevTimestamp = session.getSyncTimestamp();
            currentTimestamp = System.currentTimeMillis();

            syncTask = new SyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            Log.i(TAG,"this is inside end startSynching");
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

        configurePeriodicSync(newAccount, context);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);

    }


    @SuppressLint("StaticFieldLeak")
    class SyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!Utils.isUploadAllowed(getContext())) {
                Log.d(TAG, "skipped due to missing connection");
                return null;
            }

            Log.i(TAG, "this is inside before restApi ");
            Log.d(TAG, "start updating");
            //REST API implementation
            updated = false;
            synchronized (getLock()) {
                try {
                    processPersonQueue();
                    processMeasureQueue();
                    processConsentSheet();
                 /*  Things to yet implemented
                    processMeasureResultQueue(queueClient);
                    processDeviceQueue(queueClient);
                    processCachedMeasures();*/


                    //     measureRepository.uploadMeasures(getContext());
                    session.setSyncTimestamp(currentTimestamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "end updating");
            syncTask = null;
            return null;
        }

        private String getQrCode(String measureId) {
            try {
                return measureRepository.getMeasureById(measureId).getQrCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void processMeasureResultQueue(CloudQueueClient queueClient) throws URISyntaxException {

            try {
                CloudQueue measureResultQueue = queueClient.getQueueReference(Utils.getAndroidID(getContext().getContentResolver()) + "-measure-result");

                if (measureResultQueue.exists()) {
                    Iterable<CloudQueueMessage> retrievedMessages;

                    measureResultQueue.setShouldEncodeMessage(false);
                    retrievedMessages = measureResultQueue.retrieveMessages(30);
                    Gson gson = new Gson();

                    Iterator<CloudQueueMessage> iterator = retrievedMessages.iterator();
                    if (iterator.hasNext()) {
                        Log.d(TAG, "has at least one message");
                    }
                    while (iterator.hasNext()) {
                        CloudQueueMessage message = iterator.next();

                        try {
                            String messageStr = message.getMessageContentAsString();
                            Log.d(TAG, messageStr);
                            MeasureResult result = gson.fromJson(messageStr, MeasureResult.class);
                            String qrCode = getQrCode(result.getMeasure_id());
                            MeasureNotification notification = MeasureNotification.get(qrCode);

                            float keyMaxConfident = measureResultRepository.getConfidence(result.getMeasure_id(), result.getKey());
                            if (result.getConfidence_value() > keyMaxConfident) {
                                measureResultRepository.insertMeasureResult(result);
                            }

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

                                        if (notification != null) {
                                            notification.setWeight(result.getFloat_value());
                                        }
                                    }
                                } else if ((notification != null) && !notification.hasWeight()) {
                                    notification.setWeight(result.getFloat_value());
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

                                        if (notification != null) {
                                            notification.setHeight(result.getFloat_value());
                                        }
                                    }
                                } else if ((notification != null) && notification.hasHeight()) {
                                    notification.setHeight(result.getFloat_value());
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }

                        measureResultQueue.deleteMessage(message);
                    }
                    MeasureNotification.showNotification(getContext());
                }
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }

        private void processPersonQueue() {
            try {
                List<Person> syncablePersons = personRepository.getSyncablePerson(session.getEnvironment());
                for (int i = 0; i < syncablePersons.size(); i++) {

                    Person person = syncablePersons.get(i);
                    Log.i("Syncadapter", "this is inside processPerson Queue " + person);
                    if (person.getServerId() == null || person.getServerId().isEmpty()) {
                        postPerson(person);
                    } else {
                        putPerson(person);
                    }
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
            }
        }


        private void processMeasureQueue() {
            try {
                Log.i("Syncadapter", "this is inside value of prevTimeStamp " + prevTimestamp);

                List<Measure> syncableMeasures = measureRepository.getSyncableMeasure(session.getEnvironment());

                for (int i = 0; i < syncableMeasures.size(); i++) {
                    Measure measure = syncableMeasures.get(i);
                    String localPersonId = measure.getPersonId();
                    Person person = personRepository.getPersonById(localPersonId);
                    String backendPersonId = person.getServerId();
                    if (backendPersonId == null) {
                        continue;
                    }
                    measure.setPersonServerKey(backendPersonId);

                    if (measure.getType().compareTo(AppConstants.VAL_MEASURE_MANUAL) == 0) {
                        postMeasurement(measure);
                    } else {
                        HashMap<Integer, Scan> scans = measure.split(fileLogRepository, session.getEnvironment());
                        if (!scans.isEmpty()) {
                            Log.i(TAG, "this is values of scan " + scans);
                            postScans(scans, measure);
                        }
                    }
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
            }
        }

        private void processConsentSheet() {
            try {

                List<FileLog> syncableConsent = fileLogRepository.loadConsentFile(session.getEnvironment());

                for (int i = 0; i < syncableConsent.size(); i++) {
                    FileLog fileLog = syncableConsent.get(i);
                    if (!fileLog.isDeleted()) {
                        continue;
                    }
                    Person person = personRepository.findPersonByQr(fileLog.getQrCode());
                    String backendPersonId = person.getServerId();
                    if (backendPersonId == null) {
                        continue;
                    }
                    postConsentSheet(fileLog, backendPersonId);

                }
            } catch (Exception e) {
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
                    Log.i("Syncadapter", "this is inside processDevice Queue " + content);
                    message.setMessageContent(content);

                    deviceQueue.addMessage(message);

                    syncableDevices.get(i).setSync_timestamp(prevTimestamp);

                    deviceRepository.updateDevice(syncableDevices.get(i));
                }
            } catch (StorageException e) {
                currentTimestamp = prevTimestamp;
            }
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

    private void postScans(HashMap<Integer, Scan> scans, Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            final int[] count = {scans.values().size()};
            for (Scan scan : scans.values()) {

                Log.i(TAG, "this is data of postScan " + (new JSONObject(gson.toJson(scan))).toString());

                onThreadChange(1);
                RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(scan))).toString());
                retrofit.create(ApiService.class).postScans(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Scan>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull Scan scan) {
                                Log.i(TAG, "this is inside onNext artifactsList " + scan.toString());
                                count[0]--;
                                if (count[0] == 0) {
                                    measure.setArtifact_synced(true);
                                    measure.setTimestamp(session.getSyncTimestamp());
                                    measure.setUploaded_at(System.currentTimeMillis());
                                    measure.setSynced(true);
                                    updated = true;
                                    measureRepository.updateMeasure(measure);
                                }
                                onThreadChange(-1);
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.i(TAG, "this is value of post " + e.getMessage());
                                if (Utils.isExpiredToken(e.getMessage())) {
                                    AuthenticationHandler.restoreToken(getContext());
                                }
                                onThreadChange(-1);
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }
        } catch (Exception e) {
            Log.i(TAG, "this is value of exception " + e.getMessage());
        }
    }

    public void postPerson(Person person1) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            person1.setBirthdayString(DataFormat.convertTimestampToDate(person1.getBirthday()));
            person1.setQr_scanned(DataFormat.convertTimestampToDate(person1.getCreated()));


            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(person1))).toString());

            Log.i(TAG, "this is data of person " + (new JSONObject(gson.toJson(person1))).toString());

            onThreadChange(1);
            retrofit.create(ApiService.class).postPerson(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Person>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Person person) {
                            Log.i(TAG, "this is inside of person on next  " + person);
                            person.setTimestamp(prevTimestamp);
                            person.setId(person1.getId());
                            person.setCreatedBy(person1.getCreatedBy());
                            person.setCreated(person1.getCreated());
                            person.setSynced(true);
                            person.setEnvironment(person1.getEnvironment());
                            Loc location = new Loc();
                            person.setLastLocation(location);
                            if (person1.getLastLocation() != null) {
                                person.getLastLocation().setAddress(person1.getLastLocation().getAddress());
                                person.getLastLocation().setLatitude(person1.getLastLocation().getLatitude());
                                person.getLastLocation().setLongitude(person1.getLastLocation().getLongitude());
                            }
                            person.setBirthday(person1.getBirthday());
                            updatePersonOnDatabase(person);
                            updated = true;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is value of post " + e.getMessage());
                            if (Utils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(getContext());
                            }
                            onThreadChange(-1);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i(TAG, "this is value of exception " + e.getMessage());
        }
    }

    public void putPerson(Person person1) {
        try {

            Person putPerson = new Person();

            putPerson.setBirthdayString(DataFormat.convertTimestampToDate(person1.getBirthday()));
            putPerson.setGuardian(person1.getGuardian());
            putPerson.setAgeEstimated(person1.isAgeEstimated());
            putPerson.setName(person1.getName());
            putPerson.setSex(person1.getSex());

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(putPerson))).toString());

            Log.i(TAG, "this is data of person " + (new JSONObject(gson.toJson(putPerson))).toString());

            onThreadChange(1);
            retrofit.create(ApiService.class).putPerson(session.getAuthTokenWithBearer(), body, person1.getServerId()).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Person>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Person person) {
                            Log.i(TAG, "this is inside of update person on next  " + person);
                            person.setTimestamp(prevTimestamp);
                            person.setId(person1.getId());
                            person.setCreatedBy(person1.getCreatedBy());
                            person.setCreated(person1.getCreated());
                            person.setSynced(true);
                            Loc location = new Loc();
                            person.setLastLocation(location);
                            person.getLastLocation().setAddress(person1.getLastLocation().getAddress());
                            person.getLastLocation().setLatitude(person1.getLastLocation().getLatitude());
                            person.getLastLocation().setLongitude(person1.getLastLocation().getLongitude());
                            person.setBirthday(person1.getBirthday());
                            updatePersonOnDatabase(person);
                            updated = true;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is value of update person" +
                                    "" +
                                    " " + e.getMessage());
                            if (Utils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(getContext());
                            }
                            onThreadChange(-1);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i(TAG, "this is value of exception " + e.getMessage());
        }
    }


    @SuppressLint("StaticFieldLeak")
    public void updatePersonOnDatabase(Person person) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                personRepository.updatePerson(person);
                return null;
            }

            public void onPostExecute(Void result) {


            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void postMeasurement(Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            measure.setMeasured(DataFormat.convertTimestampToDate(measure.getDate()));

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(measure))).toString());
            Log.i(TAG, "this is data of measure " + (new JSONObject(gson.toJson(measure))).toString());

            onThreadChange(1);
            retrofit.create(ApiService.class).postMeasure(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Measure>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Measure measure1) {
                            Log.i(TAG, "this is inside of measure on next  " + measure);                            measure1.setTimestamp(prevTimestamp);
                            measure1.setId(measure.getId());
                            measure1.setPersonId(measure.getPersonId());
                            measure1.setType(AppConstants.VAL_MEASURE_MANUAL);
                            measure1.setCreatedBy(measure.getCreatedBy());
                            measure1.setDate(measure.getDate());
                            measure1.setUploaded_at(session.getSyncTimestamp());
                            measure1.setSynced(true);
                            measureRepository.updateMeasure(measure1);
                            updated = true;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is value of post " + e.getMessage());
                            if (Utils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(getContext());
                            }
                            onThreadChange(-1);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i(TAG, "this is value of exception " + e.getMessage());
        }
    }

    public void postConsentSheet(FileLog fileLog, String personId) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            Consent consent = new Consent();
            consent.setFile(fileLog.getServerId());
            consent.setScanned(DataFormat.convertTimestampToDate(fileLog.getCreateDate()));

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(consent))).toString());
            Log.i(TAG, "this is data of postConsent " + (new JSONObject(gson.toJson(consent))).toString());

            onThreadChange(1);
            retrofit.create(ApiService.class).postConsent(session.getAuthTokenWithBearer(), body, personId).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Consent>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Consent consent) {
                            Log.i(TAG, "this is inside of postConsent on next  " + consent);
                            fileLog.setStatus(AppConstants.CONSENT_UPLOADED);
                            fileLogRepository.updateFileLog(fileLog);
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is value of post " + e.getMessage());
                            if (Utils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(getContext());
                            }
                            onThreadChange(-1);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i(TAG, "this is value of exception " + e.getMessage());
        }
    }

    private void onThreadChange(int diff) {
        int count;
        synchronized (activeThreads) {
            activeThreads += diff;
            count = activeThreads;
        }

        if (updated && (count == 0)) {
            new Thread(() -> startSyncing()).start();
        }
    }
}
