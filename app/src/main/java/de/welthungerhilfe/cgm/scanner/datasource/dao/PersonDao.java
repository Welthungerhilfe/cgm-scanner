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

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE timestamp>:timestamp ORDER By timestamp")
    List<Person> getSyncablePersons(long timestamp);

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
