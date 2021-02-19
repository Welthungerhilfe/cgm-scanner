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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.EstimatesResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.PostScanResult;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PostScanResultrepository;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.utils.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
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

    private final String TAG = SyncAdapter.class.getSimpleName();

    private Integer activeThreads;
    private boolean updated;
    private int updateDelay;

    private long prevTimestamp;
    private long currentTimestamp;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private DeviceRepository deviceRepository;
    private FileLogRepository fileLogRepository;
    private PostScanResultrepository postScanResultrepository;

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
        postScanResultrepository = PostScanResultrepository.getInstance(context);

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
            Log.i(TAG, "this is inside end startSynching");
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
            updateDelay = 60 * 60 * 1000;
            synchronized (getLock()) {
                try {
                    processPersonQueue();
                    processMeasureQueue();
                    processDeviceQueue();
                    processConsentSheet();
                    processMeasureReasult();

                    session.setSyncTimestamp(currentTimestamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "end updating");
            syncTask = null;
            return null;
        }

        private void processMeasureReasult() {

            try {
                List<PostScanResult> syncablePostScanResult = postScanResultrepository.getSyncablePostScanResult(session.getEnvironment());
                for (int i = 0; i < syncablePostScanResult.size(); i++) {

                    PostScanResult postScanResult = syncablePostScanResult.get(i);
                    Log.i("Syncadapter", "this is inside processMeasureReasult Queue " + postScanResult);
                    getEstimates(postScanResult);
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
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
                e.printStackTrace();
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
                        if (measure.getMeasureServerKey() != null) {
                            putMeasurement(measure);
                        } else {
                            postMeasurement(measure);
                        }
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
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }


        private void processDeviceQueue()  {
            try {
                List<Device> syncableDevices = deviceRepository.getSyncableDevice(prevTimestamp);
                for (int i = 0; i < syncableDevices.size(); i++) {

                    //TODO:process by REST API
                    syncableDevices.get(i).setSync_timestamp(prevTimestamp);
                    deviceRepository.updateDevice(syncableDevices.get(i));
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                e.printStackTrace();
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
                                Log.i(TAG, "this is response success postScan " + scan.toString());
                                PostScanResult postScanResult = new PostScanResult();
                                postScanResult.setEnvironment(measure.getEnvironment());
                                postScanResult.setId(scan.getId());
                                postScanResult.setMeasure_id(measure.getId());
                                postScanResult.setTimestamp(prevTimestamp);
                                postScanResultrepository.insertPostScanResult(postScanResult);

                                count[0]--;
                                if (count[0] == 0) {
                                    measure.setArtifact_synced(true);
                                    measure.setTimestamp(session.getSyncTimestamp());
                                    measure.setUploaded_at(System.currentTimeMillis());
                                    measure.setSynced(true);
                                    updated = true;
                                    updateDelay = 0;
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
            e.printStackTrace();
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
                            Log.i(TAG, "this is response success postPerson " +person);
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
                            personRepository.updatePerson(person);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error postperson" + e.getMessage());
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
            e.printStackTrace();
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
                            Log.i(TAG, "this is response success putPerson " +person);
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
                            personRepository.updatePerson(person);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error putPerson" + e.getMessage());

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
            e.printStackTrace();
        }
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
                            Log.i(TAG, "this is response success postMeasure " +measure1);
                            measure1.setTimestamp(prevTimestamp);
                            measure1.setId(measure.getId());
                            measure1.setPersonId(measure.getPersonId());
                            measure1.setType(AppConstants.VAL_MEASURE_MANUAL);
                            measure1.setCreatedBy(measure.getCreatedBy());
                            measure1.setDate(measure.getDate());
                            measure1.setUploaded_at(session.getSyncTimestamp());
                            measure1.setSynced(true);
                            measure1.setEnvironment(measure.getEnvironment());
                            measureRepository.updateMeasure(measure1);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error postMeasurements" + e.getMessage());
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
            e.printStackTrace();
        }
    }

    public void putMeasurement(Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            measure.setMeasured(DataFormat.convertTimestampToDate(measure.getDate()));
            measure.setPersonServerKey(null);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(measure))).toString());
            Log.i(TAG, "this is data of measure " + (new JSONObject(gson.toJson(measure))).toString());

            onThreadChange(1);
            retrofit.create(ApiService.class).putMeasure(session.getAuthTokenWithBearer(), body, measure.getMeasureServerKey()).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Measure>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Measure measure1) {
                            Log.i(TAG, "this is response success putMeasure " +measure1);
                            measure1.setTimestamp(prevTimestamp);
                            measure1.setId(measure.getId());
                            measure1.setPersonId(measure.getPersonId());
                            measure1.setType(AppConstants.VAL_MEASURE_MANUAL);
                            measure1.setCreatedBy(measure.getCreatedBy());
                            measure1.setDate(measure.getDate());
                            measure1.setUploaded_at(session.getSyncTimestamp());
                            measure1.setEnvironment(measure.getEnvironment());
                            measure1.setSynced(true);
                            measureRepository.updateMeasure(measure1);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error putMeasurements" + e.getMessage());
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
            e.printStackTrace();
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
                            Log.i(TAG, "this is response success postConsentSheet " +consent);
                            fileLog.setStatus(AppConstants.CONSENT_UPLOADED);
                            fileLogRepository.updateFileLog(fileLog);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error postConsentSheet" + e.getMessage());
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
            e.printStackTrace();
        }
    }

    public void getEstimates(PostScanResult postScanResult) {
        try {
            Log.i(TAG, "this is data of postScanResulu " + postScanResult.getId());

            onThreadChange(1);
            retrofit.create(ApiService.class).getEstimates(session.getAuthTokenWithBearer(), postScanResult.getId()).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<EstimatesResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull EstimatesResponse estimatesResponse) {
                            Log.i(TAG, "this is response success getEstimation " +estimatesResponse);
                            //TODO : generate notification and store result based on confidence value
                            if (estimatesResponse != null) {

                                Measure measure = measureRepository.getMeasureById(postScanResult.getMeasure_id());
                                if (estimatesResponse.height != null && estimatesResponse.height.size() > 0) {
                                    Collections.sort(estimatesResponse.height);
                                    float height = estimatesResponse.height.get((estimatesResponse.height.size() / 2)).getValue();
                                    Log.i(TAG, "this is value of height " + height);

                                    String qrCode = getQrCode(postScanResult.getMeasure_id());
                                    MeasureNotification notification = MeasureNotification.get(qrCode);

                                    if (measure != null) {
                                        boolean hadHeight = measure.getHeight() > 0;
                                        measure.setHeight(height);
                                        measure.setHeightConfidence(0);
                                        measure.setResulted_at(postScanResult.getTimestamp());
                                        measure.setReceived_at(System.currentTimeMillis());
                                        measureRepository.updateMeasure(measure);

                                        if ((notification != null) && !hadHeight) {
                                            notification.setHeight(height);
                                            MeasureNotification.showNotification(getContext());
                                        }
                                    }
                                }

                                if (estimatesResponse.weight != null && estimatesResponse.weight.size() > 0) {
                                    Collections.sort(estimatesResponse.weight);
                                    float weight = estimatesResponse.weight.get((estimatesResponse.weight.size() / 2)).getValue();
                                    Log.i(TAG, "this is value of weight " + weight);

                                    String qrCode = getQrCode(postScanResult.getMeasure_id());
                                    MeasureNotification notification = MeasureNotification.get(qrCode);

                                    if (measure != null) {
                                        boolean hadWeight = measure.getWeight() > 0;
                                        measure.setWeight(weight);
                                        measure.setWeightConfidence(0);
                                        measure.setResulted_at(postScanResult.getTimestamp());
                                        measure.setReceived_at(System.currentTimeMillis());
                                        measureRepository.updateMeasure(measure);

                                        if ((notification != null) && !hadWeight) {
                                            notification.setWeight(weight);
                                            MeasureNotification.showNotification(getContext());
                                        }
                                    }
                                }

                                if (measure != null) {
                                    if ((measure.getHeight() > 0) && (measure.getWeight() > 0)) {
                                        postScanResult.setSynced(true);
                                        onResultReceived(postScanResult.getMeasure_id());
                                    }
                                }
                            }
                            postScanResultrepository.updatePostScanResult(postScanResult);
                            updated = true;
                            updateDelay = Math.min(60 * 1000, updateDelay);
                            onThreadChange(-1);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is response error getEstimation" + e.getMessage());
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
            e.printStackTrace();
        }
    }

    private String getQrCode(String measureId) {
        try {
            return measureRepository.getMeasureById(measureId).getQrCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void onResultReceived(String measureId) {

        Context c = getContext();
        if (!LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT))
            return;
        if (LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID).compareTo(measureId) != 0)
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

    private void onThreadChange(int diff) {
        int count;
        synchronized (activeThreads) {
            activeThreads += diff;
            count = activeThreads;
        }

        if (updated && (count == 0)) {
            new Thread(() -> {
                if (updateDelay > 0) {
                    Utils.sleep(updateDelay);
                }
                startSyncing();
            }).start();
        }
    }
}
