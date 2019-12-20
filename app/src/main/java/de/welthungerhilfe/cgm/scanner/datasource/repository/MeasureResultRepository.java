package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

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
}
