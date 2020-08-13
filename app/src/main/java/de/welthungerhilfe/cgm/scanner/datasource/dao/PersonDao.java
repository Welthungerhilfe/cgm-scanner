package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Delete;
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
    //@Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC LIMIT :pageSize OFFSET :index")
    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC")
    List<Person> getPersons();

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND createdBy LIKE :email")
    LiveData<List<Person>> getOwnPersons(String email);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE id=:id AND deleted=0")
    LiveData<Person> getPerson(String id);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE qrcode=:qrCode AND deleted=0 LIMIT 1")
    LiveData<Person> getPersonByQr(String qrCode);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE timestamp>:timestamp ORDER By timestamp")
    List<Person> getSyncablePersons(long timestamp);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND (id=:key OR qrcode LIKE :key)")
    LiveData<Person> findPerson(String key);

    @Insert(onConflict = REPLACE)
    void insertPerson(Person person);

    @Update(onConflict = REPLACE)
    void updatePerson(Person person);

    @Delete
    void deletePerson(Person person);

    @Query("DELETE FROM " + TABLE_PERSON + " WHERE deleted=1")
    void deletePersonGarbage();

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC LIMIT :pageSize OFFSET :index")
    LiveData<List<Person>> loadMore(int index, int pageSize);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND STRFTIME('%Y-%m-%d', DATETIME(created/1000, 'unixepoch'))=DATE('now') AND createdBy LIKE :createdBy ORDER BY created DESC")
    LiveData<List<Person>> getAll(String createdBy);

    @Query("SELECT * FROM " + TABLE_PERSON)
    List<Person> getAll();

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC LIMIT 50 OFFSET :offset")
    LiveData<List<Person>> getPersonByPage(int offset);

    @RawQuery(observedEntities = Person.class)
    LiveData<List<Person>> getResultPerson(SupportSQLiteQuery query);

    @Query("SELECT COUNT(id) FROM " + TABLE_PERSON + " WHERE createdBy LIKE :email")
    long getOwnPersonCount(String email);

    @Query("SELECT COUNT(id) FROM " + TABLE_PERSON)
    long getTotalPersonCount();
}
