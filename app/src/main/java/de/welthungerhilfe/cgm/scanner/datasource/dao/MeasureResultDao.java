package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface MeasureResultDao {
    @Insert(onConflict = REPLACE)
    void insertMeasureResult(MeasureResult result);

    @Query("SELECT * FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:measure_id")
    MeasureResult getMeasureResultById(String measure_id);

    @Query("SELECT * FROM " + CgmDatabase.TABLE_MEASURE_RESULT)
    LiveData<List<MeasureResult>> getMeasureResults();

    @Query("SELECT MAX(confidence_value) FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:id AND `key` LIKE :key")
    float getConfidence(String id, String key);

    @Query("SELECT MAX(confidence_value) FROM " + CgmDatabase.TABLE_MEASURE_RESULT + " WHERE measure_id=:id AND `key` LIKE :key")
    float getMaxConfidence(String id, String key);

    @Query("SELECT * FROM " + CgmDatabase.TABLE_MEASURE_RESULT)
    List<MeasureResult> getAll();
}
