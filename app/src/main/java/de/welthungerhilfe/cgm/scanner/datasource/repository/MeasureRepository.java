package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasureLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasuresLoad;

public class MeasureRepository {
    private static MeasureRepository instance;
    private CgmDatabase database;

    private MeasureRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static MeasureRepository getInstance(Context context) {
        if(instance == null) {
            instance = new MeasureRepository(context);
        }
        return instance;
    }

    public void insertMeasure(Measure measure) {
        ((Runnable) () -> {
            database.measureDao().insertMeasure(measure);
        }).run();
    }

    public void updateMeasure(Measure measure) {
        ((Runnable) () -> {
            database.measureDao().updateMeasure(measure);
        }).run();
    }

    public void getSyncableMeasure(OnMeasuresLoad listener, long timestamp) {
        ((Runnable) () -> {
            List<Measure> data = database.measureDao().getSyncableMeasure(timestamp);
            listener.onMeasuresLoaded(data);
        }).run();
    }

    public void getPersonLastMeasure(OnMeasureLoad listener, String personId) {
        ((Runnable) () -> {
            Measure data = database.measureDao().getLastMeasure(personId);
            listener.onMeasureLoad(data);
        }).run();
    }

    public LiveData<List<Measure>> getPersonMeasures(String personId) {
        return database.measureDao().getPersonMeasures(personId);
    }
}
