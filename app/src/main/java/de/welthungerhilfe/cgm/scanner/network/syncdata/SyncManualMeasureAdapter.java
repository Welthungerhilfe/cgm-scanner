package de.welthungerhilfe.cgm.scanner.network.syncdata;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.SyncManualMeasureResponse;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.NetworkUtils;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.service.ApiService;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import retrofit2.Retrofit;

public class SyncManualMeasureAdapter {

    String TAG = SyncManualMeasureAdapter.class.getSimpleName();

    private static SyncManualMeasureAdapter instance;
    SessionManager session;
    MeasureRepository measureRepository;
    PersonRepository personRepository;
    Retrofit retrofit;
    Context context;


    public static SyncManualMeasureAdapter getInstance(Context context) {
        instance = new SyncManualMeasureAdapter(context);
        return instance;
    }

    public SyncManualMeasureAdapter(Context context) {
        measureRepository = MeasureRepository.getInstance(context);
        personRepository = PersonRepository.getInstance(context);
        session = new SessionManager(context);
        this.context = context;
        if (retrofit == null) {
            retrofit = SyncingWorkManager.provideRetrofit();
        }
    }

    public void getSyncManualMeasure(Person person) {
        try {

            String lastManualSyncTime = null;
            if (person.getLast_sync_measurments() > 0) {
                lastManualSyncTime = DataFormat.convertMilliSeconsToServerDate(person.getLast_sync_measurments());
            }
            Log.i(TAG, "this is inside getSyncPersons " + lastManualSyncTime);
            LogFileUtils.logInfo(TAG, "Syncing manual measurements... ");
            retrofit.create(ApiService.class).getSyncManualMeasure(session.getAuthTokenWithBearer(), person.getServerId(), lastManualSyncTime)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SyncManualMeasureResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull SyncManualMeasureResponse syncManualMeasureResponse) {
                            Log.i(TAG, "this is inside getSyncManualMeasure success " + syncManualMeasureResponse.measurements.size());

                            LogFileUtils.logInfo(TAG, "Sync manual measurements successfully fetch measurements " + syncManualMeasureResponse.measurements.size());
                            person.setLast_sync_measurments(System.currentTimeMillis());
                            if (syncManualMeasureResponse.measurements != null && syncManualMeasureResponse.measurements.size() > 0) {
                                for (int i = 0; i < syncManualMeasureResponse.measurements.size(); i++) {
                                    Measure measure = syncManualMeasureResponse.measurements.get(i);
                                    Measure existingMeasure = measureRepository.getMeasureByMeasureServerKey(measure.getMeasureServerKey());

                                    measure.setDate(DataFormat.convertServerDateToMilliSeconds(measure.getMeasured()));
                                    measure.setEnvironment(session.getEnvironment());
                                    measure.setSynced(true);
                                    measure.setType(AppConstants.VAL_MEASURE_MANUAL);
                                    long age = (measure.getDate() - person.getBirthday()) / 1000 / 60 / 60 / 24;
                                    measure.setAge(age);
                                    measure.setArtifact("");
                                    measure.setVisible(false);
                                    measure.setTimestamp(DataFormat.convertServerDateToMilliSeconds(measure.getMeasure_updated()));
                                    measure.setCreatedBy(session.getUserEmail());
                                    measure.setDeleted(false);
                                    measure.setQrCode(person.getQrcode());
                                    measure.setSchema_version(CgmDatabase.version);
                                    measure.setArtifact_synced(true);
                                    measure.setPersonId(person.getId());
                                    if (existingMeasure != null) {
                                        measure.setId(existingMeasure.getId());
                                        insertMeasure(measure);
                                    } else {
                                        measure.setId(AppController.getInstance().getMeasureId());
                                        insertMeasure(measure);
                                    }
                                }

                            }
                            person.setLast_sync_measurments(Utils.getUniversalTimestamp());
                            savePerson(person);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i(TAG, "this is inside getSyncPersons error " + e.getMessage());
                            LogFileUtils.logError(TAG, "Sync person failed " + e.getMessage());
                            if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                AuthenticationHandler.restoreToken(context);
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            LogFileUtils.logException(e);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void savePerson(Person person) {
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

    @SuppressLint("StaticFieldLeak")
    public void insertMeasure(Measure measure) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                measureRepository.insertMeasure(measure);
                return null;
            }

            public void onPostExecute(Void result) {
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
