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
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;

@Dao
public interface PersonDao {
    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE qrcode=:qrCode AND deleted=0 LIMIT 1")
    LiveData<Person> getPersonByQr(String qrCode);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE qrcode=:qrCode AND deleted=0 LIMIT 1")
    Person findPersonByQr(String qrCode);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE id=:id AND deleted=0 LIMIT 1")
    Person getPersonById(String id);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE isSynced=0 And environment=:environment ORDER By timestamp")
    List<Person> getSyncablePersons(int environment);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE last_updated > :currentDate")
    List<Person> getPersonStat(long currentDate);

    @Insert(onConflict = REPLACE)
    void insertPerson(Person person);

    @Update(onConflict = REPLACE)
    void updatePerson(Person person);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND STRFTIME('%Y-%m-%d', DATETIME(created/1000, 'unixepoch'))=DATE('now') AND createdBy LIKE :createdBy ORDER BY created DESC")
    LiveData<List<Person>> getAll(String createdBy);

    @Query("SELECT * FROM " + TABLE_PERSON)
    List<Person> getAll();

    @RawQuery(observedEntities = Person.class)
    LiveData<List<Person>> getResultPerson(SupportSQLiteQuery query);

    @Query("SELECT COUNT(id) FROM " + TABLE_PERSON + " WHERE createdBy LIKE :email")
    long getOwnPersonCount(String email);

    @Query("SELECT COUNT(id) FROM " + TABLE_PERSON)
    long getTotalPersonCount();
}
