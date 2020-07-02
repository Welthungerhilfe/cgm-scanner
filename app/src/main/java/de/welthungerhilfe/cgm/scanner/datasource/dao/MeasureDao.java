package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_MEASURE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;

@Dao
public interface MeasureDao {
    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY date DESC")
    LiveData<List<Measure>> findMeasures(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE timestamp>:timestamp")
    List<Measure> getSyncableMeasure(long timestamp);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE id=:id LIMIT 1")
    Measure getMeasureById(String id);

    @Insert(onConflict = REPLACE)
    void insertMeasure(Measure measure);

    @Update(onConflict = REPLACE)
    void updateMeasure(Measure measure);

    @Query("UPDATE " + TABLE_MEASURE + " SET height=:height, muac=:muac, weight=:weight, headCircumference=:head, timestamp=:timestamp WHERE id=:id")
    void updateScanResult(String id, double height, double weight, double muac, double head, long timestamp);

    @Delete
    void deleteMeasure(Measure measure);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC Limit 1")
    Measure getLastMeasure(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC Limit 1")
    LiveData<Measure> getLastMeasureLiveData(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 AND (height!=0 OR weight!=0) ORDER BY timestamp DESC")
    LiveData<List<Measure>> getManualMeasuresLiveData(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0")
    LiveData<List<Measure>> getPersonMeasures(String personId);

    @Query("DELETE FROM " + TABLE_MEASURE + " WHERE deleted=1 AND timestamp<=:timestamp")
    void deleteMeasureGarbage(long timestamp);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE + " WHERE createdBy=:email")
    long getOwnMeasureCount(String email);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE)
    long getTotalMeasureCount();

    @Query("UPDATE " + TABLE_MEASURE + " SET height=:float_value WHERE id=:measure_id")
    void updateHeight(String measure_id, float float_value);

    @Query("UPDATE " + TABLE_MEASURE + " SET weight=:float_value WHERE id=:measure_id")
    void updateWeight(String measure_id, float float_value);

    @Query("SELECT * FROM " + TABLE_MEASURE)
    List<Measure> getAll();

    @Query("UPDATE " + TABLE_MEASURE + " SET resulted_at=:currentTimeMillis WHERE id=:measure_id")
    void updateResultTimestamp(String measure_id, long currentTimeMillis);

    @Query("UPDATE " + TABLE_MEASURE + " SET received_at=:currentTimeMillis WHERE id=:measure_id")
    void updateReceiveTimestamp(String measure_id, long currentTimeMillis);

    @Query("SELECT * FROM measures WHERE id IN (SELECT measureId FROM file_logs WHERE status=0 GROUP BY measureId)")
    LiveData<List<Measure>> getUploadMeasures();
}
