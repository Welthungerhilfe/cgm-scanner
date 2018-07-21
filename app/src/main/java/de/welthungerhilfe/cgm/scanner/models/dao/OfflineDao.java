package de.welthungerhilfe.cgm.scanner.models.dao;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.helper.DbConstants;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

/**
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

@Dao
public interface OfflineDao {

    // ---------------------- Person --------------------------//

    @Query("SELECT * FROM " + DbConstants.TABLE_PERSON)
    LiveData<List<Person>> getPersons();

    @Query("SELECT * FROM " + DbConstants.TABLE_PERSON + " WHERE id=:id")
    LiveData<Person> getPerson(String id);

    @Query("SELECT * FROM " + DbConstants.TABLE_PERSON + " WHERE qrcode=:qrCode")
    LiveData<Person> getPersonByQr(String qrCode);

    @Query("SELECT * FROM " + DbConstants.TABLE_PERSON + " WHERE timestamp>:timestamp")
    List<Person> getSyncablePersons(long timestamp);

    @Insert(onConflict = REPLACE)
    void savePerson(Person person);

    @Update(onConflict = REPLACE)
    void updatePerson(Person person);

    @Delete
    void deletePerson(Person person);

    // ---------------------- Consent --------------------------//

    @Query("SELECT * FROM " + DbConstants.TABLE_CONSENT + " WHERE personId=:personId")
    LiveData<List<Consent>> findConsents(String personId);

    @Insert(onConflict = REPLACE)
    void saveConsent(Consent consent);

    @Insert(onConflict = REPLACE)
    void updatePerson(Consent consent);

    @Delete
    void deleteConsent(Consent consent);

    // ---------------------- Measure -------------------------- //

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE personId=:personId ORDER BY date DESC")
    LiveData<List<Measure>> findMeasures(String personId);

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE timestamp>:timestamp")
    List<Measure> getSyncableMeasure(long timestamp);

    @Insert(onConflict = REPLACE)
    void saveMeasure(Measure measure);

    @Update(onConflict = REPLACE)
    void updateMeasure(Measure measure);

    @Delete
    void deleteMeasure(Measure measure);

    @Query("SELECT * FROM " + DbConstants.TABLE_MEASURE + " WHERE personId=:personId ORDER BY timestamp DESC Limit 1")
    Measure getLastMeasure(String personId);

    // ----------------------- File Log ------------------------ //

    @Insert(onConflict = REPLACE)
    void saveFileLog(FileLog log);

    @Update(onConflict = REPLACE)
    void updateFileLog(FileLog log);

    @Delete
    void deleteFileLog(FileLog log);

    @Query("SELECT * FROM " + DbConstants.TABLE_FILE_LOG + " WHERE id=:param OR path=:param")
    FileLog getFileLog(String param);

    @Query("SELECT * FROM " + DbConstants.TABLE_FILE_LOG + " WHERE deleted=0")
    List<FileLog> getSyncableFileLogs();
}
