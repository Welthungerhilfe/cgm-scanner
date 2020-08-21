package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

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
}
