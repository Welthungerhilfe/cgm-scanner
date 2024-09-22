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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.Utils;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.location.india.IndiaLocation;
import de.welthungerhilfe.cgm.scanner.datasource.location.india.Root;
import de.welthungerhilfe.cgm.scanner.datasource.models.Artifact;
import de.welthungerhilfe.cgm.scanner.datasource.models.CompleteScan;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.PostScanResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.ReceivedResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemainingData;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultAppHeight;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultAppScore;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultAutoDetect;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultBoundingBox;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultChildDistance;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultLightScore;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultOrientation;
import de.welthungerhilfe.cgm.scanner.datasource.models.Results;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultsData;
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import de.welthungerhilfe.cgm.scanner.datasource.models.SyncPersonsResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.Workflow;
import de.welthungerhilfe.cgm.scanner.datasource.models.WorkflowsResponse;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.IndiaLocationRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PostScanResultrepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.WorkflowRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.NetworkUtils;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.service.ApiService;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.RequestBody;
import retrofit2.Retrofit;


public class SyncAdapter implements FileLogRepository.OnFileLogsLoad {

    private static SyncAdapter instance;
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
    private WorkflowRepository workflowRepository;

    private IndiaLocationRepository indiaLocationRepository;

    private SessionManager session;
    private AsyncTask<Void, Void, Void> syncTask;
    private Retrofit retrofit;
    private Context context;
    FirebaseAnalytics firebaseAnalytics;
    private long lastSyncResultTimeStamp = 0L;
    private long lastSyncDailyReport = 0L;


    public SyncAdapter(Context context) {
        this.context = context;
        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
        deviceRepository = DeviceRepository.getInstance(context);
        fileLogRepository = FileLogRepository.getInstance(context);
        postScanResultrepository = PostScanResultrepository.getInstance(context);
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(context);
        workflowRepository = WorkflowRepository.getInstance(context);
        indiaLocationRepository = IndiaLocationRepository.getInstance(context);
        activeThreads = 0;
        session = new SessionManager(context);
    }

    public static SyncAdapter getInstance(Context context) {
        if (instance == null) {
            instance = new SyncAdapter(context);
        }
        return instance;
    }

    public static Object getLock() {
        return lock;
    }

    public void resetRetrofit() {
        retrofit = null;
    }

    private void initUploadService() {
        if (!UploadService.isInitialized()) {
            try {
                context.startService(new Intent(context, UploadService.class));

            } catch (IllegalStateException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(context, UploadService.class);
                    intent.putExtra(AppConstants.IS_FOREGROUND, true);
                    context.startForegroundService(intent);
                }
            }
        } else {
            UploadService.forceResume();
        }
    }

    private synchronized void startSyncing() {
        LogFileUtils.logInfo(TAG, "Start syncing requested");

        if (!session.isSigned()) {
            return;
        }
        LogFileUtils.logInfo(TAG, "Start syncing requested after signed ");

        synchronized (activeThreads) {
            if (activeThreads > 0) {
                LogFileUtils.logInfo(TAG,"Start syncing activatedthread "+activeThreads);
                if(fileLogRepository.getArtifactCount(session.getEnvironment())==0){
                    if(session.getBackgrounThreadCount() > 2){
                        session.setBackgrounThreadCount(0);
                        activeThreads = 0;
                    }else {
                        int count = session.getBackgrounThreadCount()+1;
                        session.setBackgrounThreadCount(count);
                        return;
                    }
                }
                else {
                    return;
                }

            }
        }
        LogFileUtils.logInfo(TAG, "Start syncing requested after activeThreads 0");

        if (syncTask == null) {
            prevTimestamp = session.getSyncTimestamp();
            currentTimestamp = System.currentTimeMillis();
            LogFileUtils.logInfo(TAG, "Start syncing requested after synctask null");

            if (retrofit == null) {
                retrofit = SyncingWorkManager.provideRetrofit();
                LogFileUtils.logInfo(TAG, "Start syncing requested after retrofit null");

            }

            syncTask = new SyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            LogFileUtils.logInfo(TAG, "Start syncing started");
        }
        LogFileUtils.logInfo(TAG, "Start syncing started with synctask null");

    }


    public void startPeriodicSync() {
        initUploadService();
        startSyncing();
    }


    @SuppressLint("StaticFieldLeak")
    class SyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (!NetworkUtils.isUploadAllowed(context)) {
                LogFileUtils.logInfo(TAG, "skipped due to missing connection");
                syncTask = null;
                return null;
            }

            //REST API implementation
            updated = false;
            updateDelay = 60 * 60 * 1000;
            LogFileUtils.logInfo(TAG, "start updating");
            synchronized (getLock()) {
                try {
                    //  postRemainingData();
                    LogFileUtils.logInfo(TAG,"this is calling processPersonQueue");
                    processPersonQueue();
                    LogFileUtils.logInfo(TAG,"this is calling processMeasureQueue");
                    processMeasureQueue();
                    LogFileUtils.logInfo(TAG,"this is calling processDeviceQueue");
                    processDeviceQueue();
                    LogFileUtils.logInfo(TAG,"this is calling processConsentSheet");
                    processConsentSheet();
                    LogFileUtils.logInfo(TAG,"this is calling processMeasureResults");
                    processMeasureResults();
                    LogFileUtils.logInfo(TAG,"this is calling getSyncPersons");
                    getSyncPersons();
                    LogFileUtils.logInfo(TAG,"this is calling migrateEnvironmentColumns");
                    migrateEnvironmentColumns();
                    LogFileUtils.logInfo(TAG,"this is calling getWorkflows");
                    getWorkflows();
                    LogFileUtils.logInfo(TAG,"this is calling postRemainingData");
                    postRemainingData();
                    LogFileUtils.logInfo(TAG,"this is calling getLocationIndia");
                    getLocationIndia();
                    LogFileUtils.logInfoOffline(TAG,"this is calling postWorkFlowsResult");
                    postWorkFlowsResult();



                    session.setSyncTimestamp(currentTimestamp);
                } catch (Exception e) {
                    LogFileUtils.logException(e,"doInBackground");
                }
            }
            LogFileUtils.logInfo(TAG, "end updating");
            syncTask = null;
            onThreadChange(0,"SyncTask");
            return null;
        }

        private void processMeasureResults() {

            try {
                List<Measure> measures = measureRepository.getMeasureWithoutScanResult();
                LogFileUtils.logInfo(TAG, measures.size() + "this is scan results to update");

                for (Measure measure : measures) {
                    getEstimates(measure);
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                LogFileUtils.logException(e,"processMeasureResult");
            }

        }

        private void processPersonQueue() {
            try {
                List<Person> syncablePersons = personRepository.getSyncablePerson(session.getEnvironment());
                LogFileUtils.logInfo(TAG, syncablePersons.size() + "this is persons to sync");

                for (Person person : syncablePersons) {
                    if (person.getServerId() == null || person.getServerId().isEmpty()) {
                        postPerson(person);
                    } else {
                        putPerson(person);
                    }
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                LogFileUtils.logException(e,"processPersonQueue");
            }
        }


        private void processMeasureQueue() {
            try {
                List<Measure> syncableMeasures = measureRepository.getSyncableMeasure(session.getEnvironment());
                LogFileUtils.logInfo(TAG, syncableMeasures.size() + "this is measures/scans to sync count");

                for (Measure measure : syncableMeasures) {
                    String localPersonId = measure.getPersonId();
                    Person person = personRepository.getPersonById(localPersonId);
                    String backendPersonId = person.getServerId();
                    if (backendPersonId == null) {
                        try {
                            LogFileUtils.logInfo(TAG, "this is measures/scans to sync issue person id-> " + backendPersonId + " type -> " + measure.getType() + " measureid-> " + measure.getId() + " time-> " + DataFormat.convertMilliSeconsToServerDate(measure.getDate()) + " std code-> " + measure.getStd_test_qr_code());
                        }catch (Exception e){
                            LogFileUtils.logInfo(TAG, "this is measures/scans to sync issue person id missing ");
                        }
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
                        try {
                            LogFileUtils.logInfo(TAG, "this is measures/scans to post measureid-> " + measure.getId() + " time-> " + DataFormat.convertMilliSeconsToServerDate(measure.getDate()) + " std code-> " + measure.getStd_test_qr_code());
                        }catch (Exception e){
                        }
                        HashMap<Integer, Scan> scans = measure.split(session, fileLogRepository, session.getEnvironment());

                        if (!scans.isEmpty()) {
                            postScans(scans, measure);
                        } else {
                            try {
                                LogFileUtils.logInfo(TAG, "this is measures/scans to sync issue scan empty person id-> " + backendPersonId + " type -> " + measure.getType() + " measureid-> " + measure.getId() + " time-> " + DataFormat.convertMilliSeconsToServerDate(measure.getDate()) + " std code-> " + measure.getStd_test_qr_code());
                                LogFileUtils.writeAppCenter(session,"SCAN_ERROR","this is measures/scans to sync issue scan empty");
                            }catch (Exception e){
                                LogFileUtils.logInfo(TAG, "this is measures/scans to sync issue scan empty");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                LogFileUtils.logException(e,"processMeasureQueue");
            }
        }

        private void processConsentSheet() {
            try {
                List<FileLog> syncableConsent = fileLogRepository.loadConsentFile(session.getEnvironment());
                LogFileUtils.logInfo(TAG, syncableConsent.size() + " consents to sync");

                for (FileLog fileLog : syncableConsent) {
                    LogFileUtils.logInfo(TAG, "Person consent to sync1 "+fileLog.getStatus()+", "+fileLog.getQrCode()+" "+fileLog.getPath());

                    if (!fileLog.isDeleted()) {
                        continue;
                    }
                    Person person = personRepository.findPersonByQr(fileLog.getQrCode(),session.getEnvironment());
                    LogFileUtils.logInfo(TAG,"Person consent to sync2 "+person);
                    if(person == null){
                        Person tempPerson = personRepository.findPersonByQrinApp(fileLog.getQrCode());
                        LogFileUtils.logInfo(TAG,"Person consent to sync3 "+personRepository.findPersonByQrinApp(fileLog.getQrCode()));

                        if(tempPerson!=null){
                            LogFileUtils.logInfo(TAG,"Person consent to sync4 "+tempPerson.getQrcode()+" "+tempPerson.getEnvironment()+" "+session.getEnvironment());

                        }
                    }
                    // String backendPersonId = person.getServerId();
                    if (person== null && person.getServerId()==null) {
                        continue;
                    }

                    postConsentSheet(fileLog, person.getServerId());
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                LogFileUtils.logException(e,"processConsentSheet");
            }
        }


        private void processDeviceQueue() {
            try {
                List<Device> syncableDevices = deviceRepository.getSyncableDevice(prevTimestamp);
                LogFileUtils.logInfo(TAG, syncableDevices.size() + " devices to sync");

                for (Device device : syncableDevices) {

                    //TODO:process by REST API
                    device.setSync_timestamp(prevTimestamp);
                    deviceRepository.updateDevice(device);
                }
            } catch (Exception e) {
                currentTimestamp = prevTimestamp;
                LogFileUtils.logException(e,"processDeviceQueue");
            }
        }
    }

    private void postScans(HashMap<Integer, Scan> scans, Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            final int[] count = {scans.values().size()};
            CompleteScan completeScan = new CompleteScan();
            ArrayList<Scan> scanList = new ArrayList();
            for (Scan scan : scans.values()) {
                scanList.add(scan);
                LogFileUtils.logInfo(TAG, "this is posting scan " + measure.getId());

            }
            completeScan.setScans(scanList);

            // LogFileUtils.logInfo(TAG, "this is posting scan raw data " + (new JSONObject(gson.toJson(completeScan))).toString());

            onThreadChange(1,"Post Scan");
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(completeScan))).toString());
            retrofit.create(ApiService.class).postScans(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CompleteScan>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull CompleteScan completeScan) {

                            if(completeScan==null || completeScan.getScans().size() == 0){
                                onThreadChange(-1,"Post Scan");

                                return;
                            }

                        /*    if(true){
                                return;
                            }*/
                            if(!(completeScan.getScans().size()==4)){
                                return;
                            }

                            for(Scan scan:completeScan.getScans()){
                                LogFileUtils.logInfo(TAG, "scan " + measure.getId() + " successfully posted");
                                PostScanResult postScanResult = new PostScanResult();
                                postScanResult.setEnvironment(measure.getEnvironment());
                                postScanResult.setId(scan.getId());
                                postScanResult.setMeasure_id(measure.getId());
                                postScanResult.setTimestamp(prevTimestamp);
                                postScanResultrepository.insertPostScanResult(postScanResult);
                                addScanDataToFileLogs(scan.getId(), scan.getArtifacts());
                            }

                            measure.setArtifact_synced(true);
                            measure.setTimestamp(session.getSyncTimestamp());
                            measure.setUploaded_at(System.currentTimeMillis());
                            measure.setSynced(true);
                            updated = true;
                            updateDelay = 0;
                            measureRepository.updateMeasure(measure);
                            onThreadChange(-1,"Post Scan");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "scan error  posting failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }

                            onThreadChange(-1,"Post Scan");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });

        } catch (Exception e) {
            LogFileUtils.logException(e,"postScan");
        }
    }

    /*private void postScans(HashMap<Integer, Scan> scans, Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            final int[] count = {scans.values().size()};
            for (Scan scan : scans.values()) {

                onThreadChange(1);
                LogFileUtils.logInfo(TAG, "this is posting scan " + scan.getId());
                RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(scan))).toString());
                retrofit.create(ApiService.class).postScans(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Scan>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull Scan scan) {
                                LogFileUtils.logInfo(TAG, "scan " + scan.getId() + " successfully posted");
                                PostScanResult postScanResult = new PostScanResult();
                                postScanResult.setEnvironment(measure.getEnvironment());
                                postScanResult.setId(scan.getId());
                                postScanResult.setMeasure_id(measure.getId());
                                postScanResult.setTimestamp(prevTimestamp);
                                postScanResultrepository.insertPostScanResult(postScanResult);
                                addScanDataToFileLogs(scan.getId(), scan.getArtifacts());

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
                                LogFileUtils.logError(TAG, "scan error " + scan.getId() + " posting failed " + e.getMessage());
                                if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                    AuthenticationHandler.restoreToken(context);
                                    error401();
                                }

                                onThreadChange(-1);
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }
        } catch (Exception e) {
            LogFileUtils.logException(e);
        }
    }*/

    public void addScanDataToFileLogs(String scanServerId, List<Artifact> artifactsList) {
        for (Artifact artifact : artifactsList) {
            FileLog fileLog = fileLogRepository.getFileLogByFileId(artifact.getFile());
            fileLog.setArtifactId(artifact.getId());
            fileLog.setScanServerId(scanServerId);
            fileLogRepository.updateFileLog(fileLog);
        }
    }


    public void postPerson(Person person1) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            person1.setBirthdayString(DataFormat.convertMilliSecondToBirthDay(person1.getBirthday()));
            person1.setQr_scanned(DataFormat.convertMilliSeconsToServerDate(person1.getCreated()));
            person1.setDevice_updated_at(DataFormat.convertMilliSeconsToServerDate(person1.getDevice_updated_at_timestamp()));
            /*person1.setCenter_location_id(null);
            person1.setLocation_id(null);*/
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(person1))).toString());

            onThreadChange(1,"post person data "+(new JSONObject(gson.toJson(person1))).toString());
            LogFileUtils.logInfo(TAG, "posting person " + person1.getQrcode());

            retrofit.create(ApiService.class).postPerson(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Person>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Person person) {
                            LogFileUtils.logInfo(TAG, "posting person successfully posted" + person.getQrcode());
                            person.setTimestamp(prevTimestamp);
                            person.setId(person1.getId());
                            person.setCreatedBy(person1.getCreatedBy());
                            person.setCreated(person1.getCreated());
                            person.setSynced(true);
                            person.setEnvironment(person1.getEnvironment());
                            person.setDenied(false);
                            person.setDevice_updated_at_timestamp(DataFormat.convertServerDateToMilliSeconds(person1.getDevice_updated_at()));

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
                            onThreadChange(-1,"post person");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "posting person failed" + person1.getQrcode() + " error  " + e.getMessage());
                            if (NetworkUtils.isDenied(e.getMessage())) {
                                person1.setDenied(true);
                                personRepository.updatePerson(person1);
                            }
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"post person");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"postPerson");
        }
    }

    public void putPerson(Person person1) {
        try {

            Person putPerson = new Person();

            putPerson.setBirthdayString(DataFormat.convertMilliSecondToBirthDay(person1.getBirthday()));
            putPerson.setGuardian(person1.getGuardian());
            putPerson.setAgeEstimated(person1.isAgeEstimated());
            putPerson.setName(person1.getName());
            putPerson.setSex(person1.getSex());
            putPerson.setDevice_updated_at(DataFormat.convertMilliSeconsToServerDate(person1.getDevice_updated_at_timestamp()));
            putPerson.setLocation_id(person1.getLocation_id());
            putPerson.setCenter_location_id(person1.getCenter_location_id());
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(putPerson))).toString());

            onThreadChange(1,"put person");
            LogFileUtils.logInfo(TAG, "putting person " + person1.getQrcode());
            retrofit.create(ApiService.class).putPerson(session.getAuthTokenWithBearer(), body, person1.getServerId()).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Person>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Person person) {
                            LogFileUtils.logInfo(TAG, "person " + person.getQrcode() + " successfully put");
                            person.setTimestamp(prevTimestamp);
                            person.setId(person1.getId());
                            person.setCreatedBy(person1.getCreatedBy());
                            person.setCreated(person1.getCreated());
                            person.setSynced(true);
                            person.setEnvironment(person1.getEnvironment());
                            person.setBirthday(person1.getBirthday());
                            person.setDevice_updated_at_timestamp(DataFormat.convertServerDateToMilliSeconds(person.getDevice_updated_at()));
                            personRepository.updatePerson(person);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"put person");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "person " + person1.getQrcode() + " putting failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"put person");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"putPerson");
        }
    }

    public void getSyncPersons() {
        try {
            LogFileUtils.logInfo(TAG,"get Syncing persons works step 1");
            if ((System.currentTimeMillis() - session.getLastPersonSyncTimestamp()) < 10000L) {
                return;
            }
            LogFileUtils.logInfo(TAG,"get Syncing persons works step 2");

            String lastPersonSyncTime = null;
            if (session.getLastPersonSyncTimestamp() > 0) {
                lastPersonSyncTime = DataFormat.convertMilliSeconsToServerDate(session.getLastPersonSyncTimestamp());
                LogFileUtils.logInfo(TAG, "get Syncing persons, the last sync was " + lastPersonSyncTime);
            } else {
                LogFileUtils.logInfo(TAG, "get Syncing persons for the first time");
            }
            LogFileUtils.logInfo(TAG,"get Syncing persons works step 3");

            boolean belongs_to_rst;
            if(session.getSelectedMode()==AppConstants.CGM_MODE){
                belongs_to_rst = false;
            }
            else {
                belongs_to_rst = true;
            }
            LogFileUtils.logInfo(TAG, "get Syncing persons "+belongs_to_rst);

            onThreadChange(1,"sync person");
            retrofit.create(ApiService.class).getSyncPersons(session.getAuthTokenWithBearer(), lastPersonSyncTime, belongs_to_rst)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SyncPersonsResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull SyncPersonsResponse syncPersonsResponse) {
                            LogFileUtils.logInfo(TAG, "Sync persons successfully fetch " + syncPersonsResponse.persons.size() + " person(s)");
                            session.setPersonSyncTimestamp(System.currentTimeMillis());
                            if (syncPersonsResponse.persons != null && syncPersonsResponse.persons.size() > 0) {
                                for (int i = 0; i < syncPersonsResponse.persons.size(); i++) {
                                    Person person = syncPersonsResponse.persons.get(i);
                                    Person existingPerson = personRepository.findPersonByQr(person.getQrcode(),session.getEnvironment());
                                    person.setSynced(true);
                                    person.setEnvironment(session.getEnvironment());
                                    person.setBirthday(DataFormat.convertBirthDateToMilliSeconds(person.getBirthdayString()));
                                    person.setCreated(DataFormat.convertServerDateToMilliSeconds(person.getQr_scanned()));
                                    person.setCreatedBy(session.getUserEmail());
                                    person.setDeleted(false);
                                    person.setSchema_version(CgmDatabase.version);
                                    person.setDevice_updated_at_timestamp(DataFormat.convertServerDateToMilliSeconds(person.getDevice_updated_at()));
                                    person.setLastLocation(null);

                                    if (existingPerson != null) {
                                        person.setId(existingPerson.getId());
                                        personRepository.updatePerson(person);

                                    } else {
                                        person.setId(AppController.getInstance().getPersonId());
                                        personRepository.insertPerson(person);
                                    }
                                }
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"sync person");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "Sync person failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"sync person");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"getSyncPerson");
        }
    }


    public void postMeasurement(Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            measure.setMeasured(DataFormat.convertMilliSeconsToServerDate(measure.getDate()));
            measure.setMeasure_updated(DataFormat.convertMilliSeconsToServerDate(measure.getTimestamp()));

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(measure))).toString());

            onThreadChange(1,"post measure");
            LogFileUtils.logInfo(TAG, "posting measure " + measure.getId());
            retrofit.create(ApiService.class).postMeasure(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Measure>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Measure measure1) {
                            LogFileUtils.logInfo(TAG, "posting measure successfully posted" + measure1.getId());
                            measure1.setTimestamp(DataFormat.convertServerDateToMilliSeconds(measure.getMeasure_updated()));
                            measure1.setId(measure.getId());
                            measure1.setPersonId(measure.getPersonId());
                            measure1.setType(AppConstants.VAL_MEASURE_MANUAL);
                            measure1.setCreatedBy(measure.getCreatedBy());
                            measure1.setDate(measure.getDate());
                            measure1.setUploaded_at(session.getSyncTimestamp());
                            measure1.setSynced(true);
                            measure1.setEnvironment(measure.getEnvironment());
                            measure1.setQrCode(measure.getQrCode());
                            measureRepository.updateMeasure(measure1);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"post measure");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "posting measure failed" + measure.getId() + "error  " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"post measure");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"postmeasure");
        }
    }

    public void putMeasurement(Measure measure) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            measure.setMeasured(DataFormat.convertMilliSeconsToServerDate(measure.getDate()));
            measure.setMeasure_updated(DataFormat.convertMilliSeconsToServerDate(measure.getTimestamp()));
            measure.setPersonServerKey(null);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(measure))).toString());

            onThreadChange(1,"put measure");
            LogFileUtils.logInfo(TAG, "putting measure " + measure.getId());
            retrofit.create(ApiService.class).putMeasure(session.getAuthTokenWithBearer(), body, measure.getMeasureServerKey()).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Measure>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Measure measure1) {
                            LogFileUtils.logInfo(TAG, "measure " + measure1.getId() + " successfully put");
                            measure1.setTimestamp(DataFormat.convertServerDateToMilliSeconds(measure.getMeasure_updated()));
                            measure1.setId(measure.getId());
                            measure1.setPersonId(measure.getPersonId());
                            measure1.setType(AppConstants.VAL_MEASURE_MANUAL);
                            measure1.setCreatedBy(measure.getCreatedBy());
                            measure1.setDate(measure.getDate());
                            measure1.setUploaded_at(session.getSyncTimestamp());
                            measure1.setEnvironment(measure.getEnvironment());
                            measure1.setSynced(true);
                            measure1.setQrCode(measure.getQrCode());
                            measureRepository.updateMeasure(measure1);
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"put measure");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "measure " + measure.getId() + " putting failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"put measure");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"putmeasure");
        }
    }

    public void postConsentSheet(FileLog fileLog, String personId) {
        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            Consent consent = new Consent();
            consent.setFile(fileLog.getServerId());
            consent.setScanned(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(consent))).toString());

            onThreadChange(1,"post consent sheet");
            LogFileUtils.logInfo(TAG, "posting consent " + fileLog.getPath());
            retrofit.create(ApiService.class).postConsent(session.getAuthTokenWithBearer(), body, personId).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Consent>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Consent consent) {
                            LogFileUtils.logInfo(TAG, "consent " + fileLog.getPath() + " successfully posted");
                            fileLog.setStatus(AppConstants.CONSENT_UPLOADED);
                            fileLogRepository.updateFileLog(fileLog);
                            updated = true;
                            updateDelay = 0;

                            onThreadChange(-1,"post consent sheet");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "consent " + fileLog.getPath() + " posting failed " + e.getMessage());
                            try {
                                LogFileUtils.logError(TAG,"consent request body "+new JSONObject(gson.toJson(consent)));
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"post consent sheet");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"postConsentsheet");
        }
    }

    public void getEstimates(Measure measure) {
        try {

            LogFileUtils.logInfo(TAG, "getting estimate for scan result " + measure);
            List<String> postScanResultList = postScanResultrepository.getScanIdsFromMeasureId(measure.getId());

            String ids = postScanResultList.get(0) + "," + postScanResultList.get(1) + "," + postScanResultList.get(2);
            onThreadChange(1,"Get Estimate");

            retrofit.create(ApiService.class).getEstimatesAll(session.getAuthTokenWithBearer(), ids).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ReceivedResult>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ReceivedResult receivedResult) {
                            //TODO : generate notification and store result based on confidence value
                            if (receivedResult != null && receivedResult.getEstimate()!=null && !receivedResult.getStatus().equalsIgnoreCase("result not generated") ) {
                                LogFileUtils.logInfo(TAG, "scan result " + measure.getId() + " estimate successfully received");

                                if (receivedResult.getEstimate().getMean_height() > 0) {

                                    String qrCode = getQrCode(measure.getId());
                                    MeasureNotification notification = MeasureNotification.get(qrCode);

                                    if (measure != null) {
                                        boolean hadHeight = measure.getHeightConfidence() > 0;
                                        measure.setHeight(receivedResult.getEstimate().getMean_height());
                                        measure.setHeightConfidence(0.1);
                                        measure.setResulted_at(System.currentTimeMillis());
                                        measure.setReceived_at(System.currentTimeMillis());
                                        if(receivedResult.getEstimate().getArtifact_max_99_percentile_pos_error()!=null) {
                                            measure.setPositive_height_error(Double.parseDouble(receivedResult.getEstimate().getArtifact_max_99_percentile_pos_error()));
                                        } else {
                                            measure.setPositive_height_error(0.0);
                                        }
                                        if(receivedResult.getEstimate().getArtifact_max_99_percentile_neg_error()!=null) {
                                            measure.setNegative_height_error(Double.parseDouble(receivedResult.getEstimate().getArtifact_max_99_percentile_neg_error()));
                                        } else {
                                            measure.setNegative_height_error(0.0);
                                        }
                                        measureRepository.updateMeasure(measure);


                                        if ((notification != null) && !hadHeight) {
                                            notification.setHeight((float) receivedResult.getEstimate().getMean_height());
                                            MeasureNotification.showNotification(context);
                                        }
                                    }
                                }

                                //Do not remove this code as we can use this chunk of code while app start receving weight result.
                               /* if (estimatesResponse.weight != null && estimatesResponse.weight.size() > 0) {
                                    Collections.sort(estimatesResponse.weight);
                                    float weight = estimatesResponse.weight.get((estimatesResponse.weight.size() / 2)).getValue();

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
                                            MeasureNotification.showNotification(context);
                                        }
                                    }
                                }*/

                                if (measure != null) {
                                    if ((measure.getHeight() > 0)) {

                                        firebaseAnalytics.logEvent(FirebaseService.RESULT_RECEIVED, null);
                                        onResultReceived(measure.getId());
                                    }
                                }
                            } else if(receivedResult.getStatus().equalsIgnoreCase("result not generated")){
                                LogFileUtils.logInfo(TAG, "scan result " + measure.getId() + " estimate not generated yet");

                            }
                            //updated = true;
                            updateDelay = Math.min(60 * 1000, updateDelay);
                            onThreadChange(-1,"Get Estimate");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "scan result " + measure.getId() + " estimate receiving failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"Get Estimate");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"getEstimation");
        }
    }


    private String getQrCode(String measureId) {
        try {
            return measureRepository.getMeasureById(measureId).getQrCode();
        } catch (Exception e) {
            LogFileUtils.logException(e,"getQRcode");
        }
        return null;
    }

    private void onResultReceived(String measureId) {

        Context c = context;
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

    private void onThreadChange(int diff, String name) {
        int count;
        if(diff==1) {
            LogFileUtils.logInfo(TAG, "this is value of thread start " + name);
        }
        if(diff == -1){
            LogFileUtils.logInfo(TAG, "this is value of thread end " + name);

        }
        synchronized (activeThreads) {
            activeThreads += diff;
            count = activeThreads;
        }
        LogFileUtils.logInfo(TAG, "this is value of thread final " + activeThreads+" "+updated+" "+count);

        if (updated && (count == 0)) {
            new Thread(() -> {
                if (updateDelay > 0) {

                    AppController.sleep(5000);
                }
                startSyncing();
            }).start();
        }
    }


    public void migrateEnvironmentColumns() {
        List<Person> syncablePersons = personRepository.getSyncablePerson(AppConstants.ENV_UNKNOWN);
        for (Person person : syncablePersons) {
            person.setEnvironment(session.getEnvironment());
            personRepository.updatePerson(person);
        }

        List<Measure> syncableMeasures = measureRepository.getSyncableMeasure(AppConstants.ENV_UNKNOWN);

        for (Measure measure : syncableMeasures) {
            measure.setEnvironment(session.getEnvironment());
            measureRepository.updateMeasure(measure);
        }

        List<PostScanResult> syncablePostScanResult = postScanResultrepository.getSyncablePostScanResult(AppConstants.ENV_UNKNOWN);

        for (PostScanResult scanResult : syncablePostScanResult) {
            scanResult.setEnvironment(session.getEnvironment());
            postScanResultrepository.updatePostScanResult(scanResult);
        }

        loadQueueFileLogs();
    }

    private void loadQueueFileLogs() {
        fileLogRepository.loadQueuedData(this, AppConstants.ENV_UNKNOWN);
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    list.get(i).setEnvironment(session.getEnvironment());
                    fileLogRepository.updateFileLog(list.get(i));
                } catch (Exception e) {
                    LogFileUtils.logException(e,"getfileLogsLoaded");
                }
            }
            loadQueueFileLogs();
        }
    }

    public void getWorkflows() {
        LogFileUtils.logInfo(TAG, "this is Workflow lists sync ");
        for (String workflow : AppConstants.workflowsList) {
            String[] data = workflow.split("-");
            if (workflowRepository.getWorkFlowId(data[0], data[1], session.getEnvironment()) == null) {
                getWorkFlowsFromServer();
                break;
            }
        }
    }

    public void getWorkFlowsFromServer() {
        LogFileUtils.logInfo(TAG, "Workflow lists featching... ");
        retrofit.create(ApiService.class).getWorkflows(session.getAuthTokenWithBearer()).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<WorkflowsResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull WorkflowsResponse workflowsResponse) {
                        LogFileUtils.logInfo(TAG, "Workflow list successfully fetched... ");
                        ArrayList<String> appWorkflowList = new ArrayList<String>(Arrays.asList(AppConstants.workflowsList));

                        if (workflowsResponse != null && workflowsResponse.getWorkflows() != null && workflowsResponse.getWorkflows().size() > 0) {
                            String receivedWorkflow;
                            for (Workflow workflow : workflowsResponse.getWorkflows()) {
                                receivedWorkflow = workflow.getName() + "-" + workflow.getVersion();
                                if (appWorkflowList.contains(receivedWorkflow)) {
                                    workflow.setEnvironment(session.getEnvironment());
                                    workflowRepository.insertWorkflow(workflow);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogFileUtils.logError(TAG, "error getting workflow lists... " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void postWorkFlowsResult() {
        LogFileUtils.logInfoOffline(TAG, "this is post Workflow result ");

        if (System.currentTimeMillis() - lastSyncResultTimeStamp < 15000) {
            return;
        }
        lastSyncResultTimeStamp = System.currentTimeMillis();
        postAutoDetectResult();
        postAppHeightResult();
        postAppPoseScoreResult();
        postChildDistance();
        postChildLightScore();
        postAppBoundingBoxResult();
        postAppOrientationResult();
    }

    public void postAutoDetectResult() {
        LogFileUtils.logInfoOffline(TAG, "this is start postAutoDetectResult sync "+fileLogRepository.loadAutoDetectedFileLog(session.getEnvironment()));

        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadAutoDetectedFileLog(session.getEnvironment());
            if(fileLogsList != null) {
                LogFileUtils.logInfoOffline(TAG, "this is start postAutoDetectResult sync " + fileLogsList.size());
            }

            if (fileLogsList.size() == 0) {
                return;
            }

            String workflow[] = AppConstants.APP_AUTO_DETECT_1_0.split("-");
            String appAutoDetectWorkflowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG, "this is posting postAutoDetectResult sync id "+appAutoDetectWorkflowId);

            if (appAutoDetectWorkflowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultAutoDetect resultAutoDetect = new ResultAutoDetect();
                resultAutoDetect.setId(UUID.randomUUID().toString());
                resultAutoDetect.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultAutoDetect.setScan(fileLog.getScanServerId());
                resultAutoDetect.setWorkflow(appAutoDetectWorkflowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultAutoDetect.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultAutoDetect.setSource_results(sourceResults);
                ResultAutoDetect.Data data = new ResultAutoDetect.Data();
                data.setAuto_detected(fileLog.getChildDetected());
                resultAutoDetect.setData(data);
                resultList.add(resultAutoDetect);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());
            LogFileUtils.logInfoOffline(TAG, "this is posting postAutoDetectResult sync ");

            onThreadChange(1,"Post auto detect");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is posting postAutoDetectResult successfully ");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setAutoDetectSynced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"Post auto detect");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is posting postAutoDetectResult failed" + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"Post auto detect");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postAutoDetectResult exception "+e.getMessage());

        }
    }

    public void postAppHeightResult() {


        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadAppHeightFileLog(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfoOffline(TAG, "this is start postAppHeightResult sync "+fileLogsList.size());

            }
            if (fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_HEIGHT_1_0.split("-");
            String appHeightWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            if(appHeightWorkFlowId!=null){
                LogFileUtils.logInfoOffline(TAG,"this is start postAppHeightResult sync id "+appHeightWorkFlowId);
            }
            if (appHeightWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultAppHeight resultAppHeight = new ResultAppHeight();
                resultAppHeight.setId(UUID.randomUUID().toString());
                resultAppHeight.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultAppHeight.setScan(fileLog.getScanServerId());
                resultAppHeight.setWorkflow(appHeightWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultAppHeight.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultAppHeight.setSource_results(sourceResults);
                ResultAppHeight.Data data = new ResultAppHeight.Data();
                if(fileLog.getChildHeight()==0.0){
                    data.setHeight(9999.0F);
                } else {
                    data.setHeight(fileLog.getChildHeight());
                }
                resultAppHeight.setData(data);
                resultList.add(resultAppHeight);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());

            onThreadChange(1,"Post app Height");
            LogFileUtils.logInfoOffline(TAG,"this is posting postAppHeightResult");

            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG,"this is posting postAppHeightResult successfully");

                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setChildHeightSynced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"Post app Height");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG,"this is posting postAppHeightResult failed "+e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"Post app Height");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postAppHeightResult exception "+e.getMessage());
        }
    }


    public void postAppPoseScoreResult() {


        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadAppPoseScoreFileLog(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfoOffline(TAG,"this is start postAppPoseScoreResult sync "+fileLogsList.size());
            }
            if (fileLogsList==null || fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_POSE_PREDICITION_1_0.split("-");
            String appPoseScoreWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG,"this is start postAppPoseScoreResult sync id "+appPoseScoreWorkFlowId);

            if (appPoseScoreWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultAppScore resultAppScore = new ResultAppScore();
                resultAppScore.setId(UUID.randomUUID().toString());
                resultAppScore.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultAppScore.setScan(fileLog.getScanServerId());
                resultAppScore.setWorkflow(appPoseScoreWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultAppScore.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultAppScore.setSource_results(sourceResults);
                ResultAppScore.Data data = new ResultAppScore.Data();
                if(fileLog.getPoseScore()==0.0){
                    data.setPoseScore(999.0f);
                }else {
                    data.setPoseScore(fileLog.getPoseScore());
                }
                if(fileLog.getPoseCoordinates()==null){
                    data.setPoseCoordinates("Not generated");
                }else {
                    data.setPoseCoordinates(formatPoseCoordinates(fileLog.getPoseCoordinates(), fileLog.getPoseScore()));
                }
                resultAppScore.setData(data);
                resultList.add(resultAppScore);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());

            onThreadChange(1,"postAppPoseScoreResult");
            LogFileUtils.logInfoOffline(TAG, "this is posting postAppPoseScoreResult  ");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is start postAppPoseScoreResult sync successfully");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setPoseScoreSynced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"postAppPoseScoreResult");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is start postAppPoseScoreResult sync failed " + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"postAppPoseScoreResult");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postAppPoseScoreResult exception "+e.getMessage());
        }
    }

    public String formatPoseCoordinates(String poseCoordinates, float poseScore){
        String full = null;

        try {
            String[] result = poseCoordinates.split(" ");
            int i;
            String key_points_coordinate = "\"key_points_coordinate\":[";
            String key_points_prob = "\"key_points_prob\":[";
            for(i=0 ; i<result.length;i++){
                if(result[i] != null && !result[i].trim().isEmpty()) {
                    String[] result1 = result[i].split(",");
                    String landmarkType = Utils.getLandmarkType(i-1);
                    if(result1!=null) {
                        key_points_coordinate = key_points_coordinate + "{" + "\""+landmarkType+"\":{\"x\":\"" + result1[1] + "\"," + "\"y\":\"" + result1[2] + "\"}},";
                        key_points_prob = key_points_prob + "{" + "\""+landmarkType+"\":{\"score\":\"" + result1[0] + "\"}},";
                    }
                }


            }

            full = "{\"body_pose_score\":\"" + poseScore + "\"," + key_points_coordinate.substring(0, key_points_coordinate.length() - 1) + "],"
                    + key_points_prob.substring(0, key_points_prob.length() - 1) + "]}";
        }catch (Exception e){
            LogFileUtils.logException(e,"formatposecoordinates");

        }
        return full;

    }

    public void postChildLightScore()  {


        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadChildLightScoreFileLog(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfo(TAG,"this is postChildLightScore start "+fileLogsList.size());
            }
            if (fileLogsList==null || fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_LIGHT_SCORE_1_0.split("-");
            String appLightScoreWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG,"this is postChildLightScore start id"+appLightScoreWorkFlowId);

            if (appLightScoreWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultLightScore resultLightScore = new ResultLightScore();
                resultLightScore.setId(UUID.randomUUID().toString());
                resultLightScore.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultLightScore.setScan(fileLog.getScanServerId());
                resultLightScore.setWorkflow(appLightScoreWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultLightScore.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultLightScore.setSource_results(sourceResults);
                ResultLightScore.Data data = new ResultLightScore.Data();
                data.setLight_score(String.valueOf(fileLog.getLight_score()));
                resultLightScore.setData(data);
                resultList.add(resultLightScore);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());
            Log.i(TAG,"this is light score body "+(new JSONObject(gson.toJson(resultsData))).toString());
            onThreadChange(1,"postChildLightScore");
            LogFileUtils.logInfoOffline(TAG, "this is postChildLightScore post ");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is postChildLightScore posted successfully");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setLight_score_synced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"postChildLightScore");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is postChildLightScore posted failed " + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"postChildLightScore");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postChildLightScore exception "+e.getMessage());

        }
    }


    public void postChildDistance() {



        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadChildDistanceFileLog(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfoOffline(TAG,"this is start postChildDistance sync"+fileLogsList.size());
            }
            if (fileLogsList==null || fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_CHILD_DISTANCE_1_0.split("-");
            String appChildDistanceWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG,"this is start postChildDistance id"+appChildDistanceWorkFlowId);

            if (appChildDistanceWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultChildDistance resultChildDistanc = new ResultChildDistance();
                resultChildDistanc.setId(UUID.randomUUID().toString());
                resultChildDistanc.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultChildDistanc.setScan(fileLog.getScanServerId());
                resultChildDistanc.setWorkflow(appChildDistanceWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultChildDistanc.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultChildDistanc.setSource_results(sourceResults);
                ResultChildDistance.Data data = new ResultChildDistance.Data();
                data.setChild_distance(String.valueOf(fileLog.getChild_distance()));
                resultChildDistanc.setData(data);
                resultList.add(resultChildDistanc);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());
            LogFileUtils.logInfoOffline(TAG,"this is post postChildDistance id");

            onThreadChange(1,"postChildDistance");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is post postChildDistance successfully posted...");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setChild_distance_synced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"postChildDistance");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is post postChildDistance posting failed " + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"postChildDistance");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postChildDistance exception "+e.getMessage());

        }
    }

    public void postRemainingData() {

        lastSyncDailyReport = session.getLastSyncDailyReport();
        LogFileUtils.logInfo(TAG,"this is remaining data 1 "+lastSyncDailyReport);
        LogFileUtils.logInfo(TAG,"this is remaining data 1.5 "+(System.currentTimeMillis() - lastSyncDailyReport));
        if((System.currentTimeMillis() - lastSyncDailyReport) > 86400000 ) {

            session.setLastSyncDailyReport(System.currentTimeMillis());
            LogFileUtils.logInfo(TAG,"this is remaining data 2 "+session.getLastSyncDailyReport());

            try {
                Gson gson = new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create();
                RemainingData remainingData = new RemainingData();
                remainingData.setArtifact(String.valueOf((int) fileLogRepository.getArtifactCount()));
                remainingData.setConsent(String.valueOf(fileLogRepository.loadConsentFile(session.getEnvironment()).size()));
                remainingData.setDevice_id(AppController.getInstance().getAndroidID());
                remainingData.setUser(session.getUserEmail());
                remainingData.setVersion(AppController.getInstance().getAppVersion());
                remainingData.setMeasure(String.valueOf(measureRepository.getSyncableMeasure(session.getEnvironment()).size()));
                remainingData.setPerson(String.valueOf(personRepository.getSyncablePerson(session.getEnvironment()).size()));
                remainingData.setScan(String.valueOf(measureRepository.getSyncableMeasure(session.getEnvironment()).size()));
                remainingData.setApp_auto_detect("--");
                remainingData.setApp_height("--");
                remainingData.setApp_light_score("--");
                remainingData.setApp_pose_Score("--");
                remainingData.setApp_distance("--");
                remainingData.setApp_bounding_box("--");
                remainingData.setApp_orientation("--");
                remainingData.setError("---");
                remainingData.setDevice_check("RGB-> "+session.getRgbSensor()+" ,TOF-> "+session.getTofSensor());

                RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(remainingData))).toString());
                LogFileUtils.logInfo(TAG,"this is remaining data 3 "+(new JSONObject(gson.toJson(remainingData))).toString());

                onThreadChange(1,"postRemainingData");
                LogFileUtils.logInfo(TAG, "posting remaining data ");
                retrofit.create(ApiService.class).postRemainingData(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<RemainingData>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }


                            @Override
                            public void onNext(@NonNull RemainingData remainingData1) {
                                LogFileUtils.logInfo(TAG, "RemainingData successfully posted");
                                Log.i(TAG,"this is remaining data 4 update ");

                                updated = true;
                                updateDelay = 0;
                                onThreadChange(-1,"postRemainingData");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                LogFileUtils.logError(TAG, "RemainingData posting failed " + e.getMessage());
                                LogFileUtils.logInfo(TAG,"this is remaining data 5 error "+e.getMessage());

                                try {
                                    LogFileUtils.logError(TAG, "RemainingData request body " + new JSONObject(gson.toJson(remainingData)));
                                } catch (JSONException jsonException) {
                                    jsonException.printStackTrace();
                                }
                                if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                    AuthenticationHandler.restoreToken(context);
                                    error401();
                                }
                                onThreadChange(-1,"postRemainingData");
                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            } catch (Exception e) {
                LogFileUtils.logException(e,"postRemainingData");
            }
        }
    }


    public void error401(){
        int count = session.getSessionError()+1;
        session.setSessionError(count);
        LogFileUtils.logInfo(TAG,"error 401 "+count);
    }

    public void postAppBoundingBoxResult() {


        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadAppBoundingBox(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfo(TAG,"this is start postAppBoundingBoxResult sync "+fileLogsList.size());
            }
            if (fileLogsList==null || fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_BOUNDING_BOX_1_0.split("-");
            String appBoundingBoxWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG,"this is start postAppBoundingBoxResult sync workflowid:wq "+fileLogsList.size());

            if (appBoundingBoxWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultBoundingBox resultBoundingBox = new ResultBoundingBox();
                resultBoundingBox.setId(UUID.randomUUID().toString());
                resultBoundingBox.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultBoundingBox.setScan(fileLog.getScanServerId());
                resultBoundingBox.setWorkflow(appBoundingBoxWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultBoundingBox.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultBoundingBox.setSource_results(sourceResults);
                ResultBoundingBox.Data data = new ResultBoundingBox.Data();
                if(fileLog.getBoundingBox()==null){
                    data.setAppBoundingBox("Not generated");
                }else {
                    data.setAppBoundingBox(fileLog.getBoundingBox());
                }
                resultBoundingBox.setData(data);
                resultList.add(resultBoundingBox);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());

            onThreadChange(1,"postAppBoundingBox");
            LogFileUtils.logInfoOffline(TAG, "this is postAppBoundingBoxResult sync posting");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is postAppBoundingBoxResult sync successfully posted...");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setBounding_box_synced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"postAppBoundingBox");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is postAppBoundingBoxResult sync posting failed " + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"postAppBoundingBox");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postBoundingBox exception "+e.getMessage());

        }
    }

    public void postAppOrientationResult() {


        try {
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            List<FileLog> fileLogsList = fileLogRepository.loadAppOrientation(session.getEnvironment());
            if(fileLogsList!=null){
                LogFileUtils.logInfoOffline(TAG,"this is start postAppOrientationResult sync "+fileLogsList.size());
            }
            if (fileLogsList==null || fileLogsList.size() == 0) {
                return;
            }
            String workflow[] = AppConstants.APP_ORIENTATION_1_0.split("-");
            String appOrientationWorkFlowId = workflowRepository.getWorkFlowId(workflow[0], workflow[1], session.getEnvironment());
            LogFileUtils.logInfoOffline(TAG,"this is start postAppOrientationResult sync id"+fileLogsList.size());

            if (appOrientationWorkFlowId == null) {
                return;
            }
            ArrayList<Results> resultList = new ArrayList();
            for (FileLog fileLog : fileLogsList) {
                ResultOrientation resultOrientation = new ResultOrientation();
                resultOrientation.setId(UUID.randomUUID().toString());
                resultOrientation.setGenerated(DataFormat.convertMilliSeconsToServerDate(fileLog.getCreateDate()));
                resultOrientation.setScan(fileLog.getScanServerId());
                resultOrientation.setWorkflow(appOrientationWorkFlowId);
                ArrayList<String> sourceArtifacts = new ArrayList<>();
                sourceArtifacts.add(fileLog.getArtifactId());
                resultOrientation.setSource_artifacts(sourceArtifacts);
                ArrayList<String> sourceResults = new ArrayList<>();
                resultOrientation.setSource_results(sourceResults);
                ResultOrientation.Data data = new ResultOrientation.Data();
                data.setAppOrientation(fileLog.getOrientation());
                resultOrientation.setData(data);
                resultList.add(resultOrientation);
            }
            ResultsData resultsData = new ResultsData();
            resultsData.setResults(resultList);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(gson.toJson(resultsData))).toString());

            onThreadChange(1,"postappOrientationResult");
            LogFileUtils.logInfoOffline(TAG, "this is post postAppOrientationResult sync  ");
            retrofit.create(ApiService.class).postWorkFlowsResult(session.getAuthTokenWithBearer(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResultsData>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull ResultsData resultsData1) {
                            LogFileUtils.logInfoOffline(TAG, "this is postAppOrientationResult sync successfully posted...");
                            for (Results results : resultsData1.getResults()) {
                                FileLog fileLog = fileLogRepository.getFileLogByArtifactId(results.getSource_artifacts().get(0));
                                fileLog.setOrientation_synced(true);
                                fileLogRepository.updateFileLog(fileLog);
                            }
                            updated = true;
                            updateDelay = 0;
                            onThreadChange(-1,"postappOrientationResult");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logInfoOffline(TAG, "this is start postAppOrientationResult sync posting failed " + e.getMessage());

                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"postappOrientationResult");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logInfoOffline("SyncAdapter","postAppOrientation exception "+e.getMessage());

        }
    }

    public void getLocationIndia() {
        LogFileUtils.logInfo(TAG, "this is getLocationIndia sync ");

        try {



            LogFileUtils.logInfo(TAG, "Syncing Location india address ");

            onThreadChange(1,"LocationIndia");
            retrofit.create(ApiService.class).getLocationIndia(session.getAuthTokenWithBearer())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Root>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Root root) {
                            LogFileUtils.logInfo(TAG, "Sync location successfully fetched " + root.country);
                            onThreadChange(-1,"LocationIndia");

                            switch (session.getEnvironment()){
                                case AppConstants.ENV_DEMO_QA:
                                    if(session.getDemoQaVersionLocation() >= root.getVersion()){
                                        return;
                                    }
                                    session.setLocationDemoQAVersion(root.version);
                                    break;
                                case AppConstants.ENV_IN_BMZ:
                                    if(session.getIndiaVersionLocation() >= root.getVersion()){
                                        return;
                                    }
                                    session.setLocationIndiaVersion(root.version);
                                    break;

                            }

                            for (int i = 0; i < root.location_json.size(); i++) {
                                Log.i(TAG,"this is inside address onnext A "+root.location_json.get(i).location_name);
                                for (int j = 0; j < root.location_json.get(i).dISTRICT.size(); j++) {
                                    Log.i(TAG,"this is inside address onnext B "+root.location_json.get(i).location_name+" "+j);

                                    for (int k = 0; k < root.location_json.get(i).dISTRICT.get(j).bLOCK.size(); k++) {

                                        Log.i(TAG,"this is inside address onnext C "+i+" "+j+" "+k);

                                        for (int l = 0; l < root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.size(); l++) {
                                            if(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).aANGANWADICENTER != null) {
                                                for (int m = 0; m < root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).aANGANWADICENTER.size(); m++) {
                                                    Log.i(TAG, "this is values of location " + root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).tree_string + " " + root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).aANGANWADICENTER.get(m).location_name);

                                                    IndiaLocation indiaLocation = new IndiaLocation();
                                                    indiaLocation.setId(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).aANGANWADICENTER.get(m).id);
                                                    indiaLocation.setVillage_full_name(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).tree_string);
                                                    indiaLocation.setAganwadi(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).aANGANWADICENTER.get(m).location_name);
                                                    indiaLocation.setVillageName(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).location_name);
                                                    indiaLocation.setLocation_id(root.location_json.get(i).dISTRICT.get(j).bLOCK.get(k).vILLAGE.get(l).id);
                                                    indiaLocation.setEnvironment(session.getEnvironment());
                                                    indiaLocationRepository.insertIndiaLocation(indiaLocation);

                                                }
                                            }
                                        }
                                    }
                                }
                            }



                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogFileUtils.logError(TAG, "Sync person failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                                error401();
                            }
                            onThreadChange(-1,"LocationIndia");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e,"getLocationIndia");
        }
    }

}
