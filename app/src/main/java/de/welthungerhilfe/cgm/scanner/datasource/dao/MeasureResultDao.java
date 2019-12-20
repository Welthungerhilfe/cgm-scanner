package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;

import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface MeasureResultDao {
    @Insert(onConflict = REPLACE)
    void insertMeasureResult(MeasureResult result);
}
