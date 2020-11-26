package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_MEASURE;

@Dao
public interface MeasureDao {
    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE artifact_synced=0")
    List<Measure> getNotSyncedMeasures();

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE timestamp>:timestamp")
    List<Measure> getSyncableMeasure(long timestamp);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE isSynced=0")
    List<Measure> getSyncableMeasure();

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE id=:id LIMIT 1")
    Measure getMeasureById(String id);

    @Insert(onConflict = REPLACE)
    void insertMeasure(Measure measure);

    @Update(onConflict = REPLACE)
    void updateMeasure(Measure measure);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC Limit 1")
    LiveData<Measure> getLastMeasureLiveData(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 AND (height!=0 OR weight!=0) ORDER BY timestamp DESC")
    LiveData<List<Measure>> getManualMeasuresLiveData(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0")
    LiveData<List<Measure>> getPersonMeasures(String personId);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE + " WHERE createdBy=:email")
    long getOwnMeasureCount(String email);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE)
    long getTotalMeasureCount();

    @Query("SELECT * FROM " + TABLE_MEASURE)
    List<Measure> getAll();

    @Query("SELECT * FROM measures WHERE id IN (SELECT measureId FROM file_logs WHERE status=0 GROUP BY measureId)")
    LiveData<List<Measure>> getUploadMeasures();
}
