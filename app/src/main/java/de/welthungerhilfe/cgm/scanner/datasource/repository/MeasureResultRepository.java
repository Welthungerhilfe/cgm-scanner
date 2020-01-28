package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;

public class MeasureResultRepository {
    private static MeasureResultRepository instance;
    private CgmDatabase database;

    private MeasureResultRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static MeasureResultRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MeasureResultRepository(context);
        }
        return instance;
    }

    public void insertMeasureResult (MeasureResult result) {
        database.measureResultDao().insertMeasureResult(result);
    }

    public MeasureResult getMeasureResultById(String measure_id) {
        return database.measureResultDao().getMeasureResultById(measure_id);
    }

    public LiveData<List<MeasureResult>> getMeasureResults() {
        return database.measureResultDao().getMeasureResults();
    }

    public float getConfidence(String id, String key) {
        return database.measureResultDao().getConfidence(id, key);
    }

    public float getMaxConfidence(String id) {
        return database.measureResultDao().getMaxConfidence(id);
    }
}
