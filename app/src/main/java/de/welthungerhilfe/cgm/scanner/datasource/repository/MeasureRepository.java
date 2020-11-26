package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactList;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.SuccessResponse;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.remote.ApiService;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.RequestBody;
import retrofit2.Retrofit;

public class MeasureRepository {
    private static MeasureRepository instance;
    private CgmDatabase database;
    private SessionManager session;
    private Retrofit retrofit;


    private MeasureRepository(Context context, Retrofit retrofit) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
        this.retrofit = retrofit;
    }

    public static MeasureRepository getInstance(Context context, Retrofit retrofit) {
        if (instance == null) {
            instance = new MeasureRepository(context, retrofit);
        }
        return instance;
    }

    public void insertMeasure(Measure measure) {
        database.measureDao().insertMeasure(measure);
    }

    public void updateMeasure(Measure measure) {
        database.measureDao().updateMeasure(measure);
    }

    public List<Measure> getSyncableMeasure(long timestamp) {
        return database.measureDao().getSyncableMeasure(timestamp);
    }

    public List<Measure> getSyncableMeasure() {
        return database.measureDao().getSyncableMeasure();
    }

    public List<Measure> getNotSyncedMeasures() {
        return database.measureDao().getNotSyncedMeasures();
    }

    public Measure getMeasureById(String id) {
        return database.measureDao().getMeasureById(id);
    }

    public LiveData<List<Measure>> getPersonMeasures(String personId) {
        return database.measureDao().getPersonMeasures(personId);
    }

    public LiveData<Measure> getPersonLastMeasureLiveData(String personId) {
        return database.measureDao().getLastMeasureLiveData(personId);
    }

    public long getOwnMeasureCount() {
        return database.measureDao().getOwnMeasureCount(session.getUserEmail());
    }

    public LiveData<List<Measure>> getManualMeasuresLiveData(String personId) {
        return database.measureDao().getManualMeasuresLiveData(personId);
    }

    public long getTotalMeasureCount() {
        return database.measureDao().getTotalMeasureCount();
    }

    public List<Measure> getAll() {
        return database.measureDao().getAll();
    }

    public LiveData<List<Measure>> getUploadMeasures() {
        return database.measureDao().getUploadMeasures();
    }

    public void uploadMeasure(Context context, Measure measure) {
        if (Utils.isUploadAllowed(context)) {
            FileLogRepository fileLogRepository = FileLogRepository.getInstance(context);
            postMeasure(measure, fileLogRepository);
        }
    }

    public void uploadMeasures(Context context) {
        synchronized (SyncAdapter.getLock()) {
            for (Measure measure : getNotSyncedMeasures()) {
                uploadMeasure(context, measure);
            }
        }
        UploadService.forceResume();
    }

    public void postArtifacts(ArtifactList artifactList, Measure measure) {
        try {
            Log.i("MeasureRepository", "this is value of artifacts " + artifactList);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(new Gson().toJson(artifactList))).toString());
            Log.i("MeasureRepository", "this is value of artifacts " + new Gson().toJson(artifactList));

            retrofit.create(ApiService.class).postArtifacts("bearer " + session.getAuthToken(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SuccessResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull SuccessResponse posts) {
                            Log.i("MeasureRepository", "this is inside onNext artifactsList " + posts.getMessage());

                            //TODO:parse response: if (success)
                            {
                                measure.setArtifact_synced(true);
                                measure.setTimestamp(session.getSyncTimestamp());
                                measure.setUploaded_at(System.currentTimeMillis());
                                updateMeasure(measure);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i("MeasureRepository", "this is inside onError ArtifactList " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i("SyncAdapter", "this is value of exception " + e.getMessage());
        }
    }

    public void postMeasure(Measure measure, FileLogRepository fileLogRepository) {
        if (measure.getType().compareTo(AppConstants.VAL_MEASURE_MANUAL) == 0) {
            //TODO:backend.scanner-api.measure.create_measure
        } else {
            //TODO:backend.scanner-api.scan.create_scan
        }

        try {
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(new Gson().toJson(measure))).toString());

            retrofit.create(ApiService.class).postMeasure("bearer " + session.getAuthToken(), body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Measure>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Measure posts) {
                            Log.i("SyncAdapter", "this is inside onNext measure " + posts);

                            //TODO:parse response: if (success)
                            {
                                List<FileLog> measureArtifacts = fileLogRepository.getArtifactsForMeasure(measure.getId());
                                for (FileLog log : measureArtifacts) {
                                    //TODO:check if backend ID exists, if not call return;
                                }

                                ArtifactList artifactList = new ArtifactList();
                                artifactList.setMeasure_id(measure.getId());
                                artifactList.setStart(1);
                                artifactList.setEnd(measureArtifacts.size());
                                artifactList.setArtifacts(measureArtifacts);
                                artifactList.setTotal(measureArtifacts.size());
                                postArtifacts(artifactList, measure);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i("SyncAdapter", "this is inside onError Measure " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            Log.i("SyncAdapter", "this is value of exception " + e.getMessage());
        }
    }
}
