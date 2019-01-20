package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.helper.DbConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface MeasureDao {
    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY date DESC")
    LiveData<List<Measure>> findMeasures(String personId);

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE timestamp>:timestamp")
    List<Measure> getSyncableMeasure(long timestamp);

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE id=:id")
    Measure findMeasure(String id);

    @Insert(onConflict = REPLACE)
    void saveMeasure(Measure measure);

    @Query("UPDATE " + DbConstants.TABLE_MEASURE + " SET height=:height, muac=:muac, weight=:weight, headCircumference=:head, timestamp=:timestamp WHERE id=:id")
    void updateScanResult(String id, double height, double weight, double muac, double head, long timestamp);

    @Update(onConflict = REPLACE)
    void updateMeasure(Measure measure);

    @Delete
    void deleteMeasure(Measure measure);

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC Limit 1")
    Measure getLastMeasure(String personId);

    @Query("DELETE FROM " + DbConstants.TABLE_MEASURE + " WHERE deleted=1 AND timestamp<=:timestamp")
    void deleteMeasureGarbage(long timestamp);
}
