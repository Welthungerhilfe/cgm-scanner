package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface MeasureResultDao {
    @Insert(onConflict = REPLACE)
    void insertMeasureResult(MeasureResult result);

    @Query("SELECT * FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:measure_id")
    MeasureResult getMeasureResultById(String measure_id);

    @Query("SELECT * FROM " + CgmDatabase.TABLE_MEASURE_RESULT)
    LiveData<List<MeasureResult>> getMeasureResults();

    @Query("SELECT confidence_value FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:id AND `key`=:key")
    float getConfidence(String id, String key);

    @Query("SELECT MAX(confidence_value) FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:id")
    float getMaxConfidence(String id);
}
