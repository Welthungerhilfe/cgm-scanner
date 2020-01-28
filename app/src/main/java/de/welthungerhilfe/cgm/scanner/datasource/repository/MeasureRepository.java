package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.ExecutorService;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasureLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasuresLoad;

public class MeasureRepository {
    private static MeasureRepository instance;
    private CgmDatabase database;
    private SessionManager session;

    private MeasureRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
    }

    public static MeasureRepository getInstance(Context context) {
        if(instance == null) {
            instance = new MeasureRepository(context);
        }
        return instance;
    }

    public void insertMeasure(Measure measure) {
        database.measureDao().insertMeasure(measure);
    }

    @SuppressLint("StaticFieldLeak")
    public void updateMeasure(Measure measure) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                database.measureDao().updateMeasure(measure);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void getSyncableMeasure(OnMeasuresLoad listener, long timestamp) {
        new AsyncTask<Void, Void, List<Measure>>() {
            @Override
            protected List<Measure> doInBackground(Void... voids) {
                return database.measureDao().getSyncableMeasure(timestamp);
            }

            public void onPostExecute(List<Measure> measures) {
                listener.onMeasuresLoaded(measures);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void getPersonLastMeasure(OnMeasureLoad listener, String personId) {
        new AsyncTask<Void, Void, Measure>() {
            @Override
            protected Measure doInBackground(Void... voids) {
                return database.measureDao().getLastMeasure(personId);
            }

            @Override
            public void onPostExecute(Measure data) {
                listener.onMeasureLoad(data);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public void updateHeight(String measure_id, float float_value) {
        database.measureDao().updateHeight(measure_id, float_value);
    }
}
