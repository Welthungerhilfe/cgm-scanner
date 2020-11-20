package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;

import javax.inject.Inject;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactList;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.SuccessResponse;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.remote.ApiService;
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
        if(instance == null) {
            instance = new MeasureRepository(context,retrofit);
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
        Gson gson = new Gson();
        FileLogRepository fileLogRepository = FileLogRepository.getInstance(context);
        synchronized (SyncAdapter.getLock()) {
            try {
                //TODO:REST API implementation
            //    CloudStorageAccount storageAccount = null;
              //  CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

                try {
                    if (!measure.isArtifact_synced()) {
                       /* CloudQueue measureArtifactsQueue = queueClient.getQueueReference("artifact-list");
                        measureArtifactsQueue.createIfNotExists();*/

                        long totalNumbers  = fileLogRepository.getTotalArtifactCountForMeasure(measure.getId());
                        final int size = 50;
                        int offset = 0;

                        measure.setArtifact_synced(true);
                        measure.setUploaded_at(System.currentTimeMillis());
                        postMeasure(measure);
                        measure.setTimestamp(session.getSyncTimestamp());
                        updateMeasure(measure);
                        while (offset + 1 < totalNumbers) {
                            List<FileLog> measureArtifacts = fileLogRepository.getArtifactsForMeasure(measure.getId(), offset, size);

                            ArtifactList artifactList = new ArtifactList();
                            artifactList.setMeasure_id(measure.getId());
                            artifactList.setStart(offset + 1);
                            artifactList.setEnd(offset + measureArtifacts.size());
                            artifactList.setArtifacts(measureArtifacts);
                            artifactList.setTotal(totalNumbers);

                            offset += measureArtifacts.size();

                            postArtifacts(artifactList);

                           /* CloudQueueMessage measureArtifactsMessage = new CloudQueueMessage(measure.getId());
                            measureArtifactsMessage.setMessageContent(gson.toJson(artifactList));
                            measureArtifactsQueue.addMessage(measureArtifactsMessage);*/
                        }

                      /*  measure.setArtifact_synced(true);
                        measure.setUploaded_at(System.currentTimeMillis());*/
                    }

                  /*  postMeasure(measure);
                    measure.setTimestamp(session.getSyncTimestamp());
                    updateMeasure(measure);
*/

                   /* CloudQueue measureQueue = queueClient.getQueueReference("measure");
                    measureQueue.createIfNotExists();

                    CloudQueueMessage message = new CloudQueueMessage(measure.getId());
                    message.setMessageContent(gson.toJson(measure));
                    measureQueue.addMessage(message);

                    measure.setTimestamp(session.getSyncTimestamp());
                    updateMeasure(measure);*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void postArtifacts(ArtifactList artifactList)

    {
        try {
            Log.i("MeasureRepository","this is value of artifacts "+artifactList);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),(new JSONObject(new Gson().toJson(artifactList))).toString());
            Log.i("MeasureRepository","this is value of artifacts "+new Gson().toJson(artifactList));

            retrofit.create(ApiService.class).postArtifacts("bearer "+session.getAuthToken(),body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SuccessResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull SuccessResponse posts) {
                            Log.i("MeasureRepository", "this is inside onNext artifactsList " + posts.getMessage());

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i("MeasureRepository", "this is inside onError ArtifactList " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
        catch (Exception e)
        {
            Log.i("SyncAdapter","this is value of exception "+e.getMessage());
        }
    }

    public void postMeasure(Measure measure)

    {
        try {
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),(new JSONObject(new Gson().toJson(measure))).toString());

            retrofit.create(ApiService.class).postMeasure("bearer "+session.getAuthToken(),body).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SuccessResponse>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull SuccessResponse posts) {
                            Log.i("SyncAdapter", "this is inside onNext measure " + posts.getMessage());

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.i("SyncAdapter", "this is inside onError Measure " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
        catch (Exception e)
        {
            Log.i("SyncAdapter","this is value of exception "+e.getMessage());
        }
    }
}
