/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE isSynced=0 AND environment=:environment")
    List<Measure> getSyncableMeasure(int environment);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE id=:id LIMIT 1")
    Measure getMeasureById(String id);

    @Insert(onConflict = REPLACE)
    void insertMeasure(Measure measure);

    @Update(onConflict = REPLACE)
    void updateMeasure(Measure measure);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 AND (height!=0 OR weight!=0) AND type='manual' ORDER BY timestamp DESC")
    List<Measure> getManualMeasures(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 AND (height!=0 OR weight!=0) ORDER BY timestamp DESC")
    List<Measure> getAllMeasuresByPersonId(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC Limit 1")
    LiveData<Measure> getLastMeasureLiveData(String personId);

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE personId=:personId AND deleted=0 ORDER BY timestamp DESC")
    LiveData<List<Measure>> getPersonMeasures(String personId);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE + " WHERE createdBy=:email")
    long getOwnMeasureCount(String email);

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE)
    long getTotalMeasureCount();

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE + " WHERE isSynced=0 AND type!='manual' AND std_test_qr_code IS NULL")
    LiveData<Long> getScanMeasureCount();

    @Query("SELECT COUNT(id) FROM " + TABLE_MEASURE + " WHERE isSynced=0 AND type!='manual' AND std_test_qr_code NOT NULL")
    LiveData<Long> getStdScanMeasureCount();

    @Query("SELECT * FROM " + TABLE_MEASURE)
    List<Measure> getAll();

    @Query("SELECT * FROM measures WHERE id IN (SELECT measureId FROM file_logs WHERE status=0 GROUP BY measureId)")
    LiveData<List<Measure>> getUploadMeasures();

    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE measureServerKey=:measureServerKey")
    Measure getMeasureByMeasureServerKey(String measureServerKey);
    @Query("SELECT * FROM " + TABLE_MEASURE + " WHERE received_at=0 AND isSynced=1 AND type!='manual'")
    List<Measure> getMeasureWithoutScanResult();
}
