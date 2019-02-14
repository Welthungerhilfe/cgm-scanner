package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;

@Dao
public interface PersonDao {
    //@Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC LIMIT :pageSize OFFSET :index")
    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC")
    List<Person> getPersons();

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND createdBy=:email")
    LiveData<List<Person>> getOwnPersons(String email);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE id=:id AND deleted=0")
    LiveData<Person> getPerson(String id);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE qrcode=:qrCode AND deleted=0 LIMIT 1")
    LiveData<Person> getPersonByQr(String qrCode);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE timestamp>:timestamp")
    List<Person> getSyncablePersons(long timestamp);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND (id=:key OR qrcode=:key)")
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

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 AND STRFTIME('%Y-%m-%d', DATETIME(created/1000, 'unixepoch'))=DATE('now') AND createdBy=:createdBy ORDER BY created DESC")
    LiveData<List<Person>> getAll(String createdBy);

    @Query("SELECT * FROM " + TABLE_PERSON + " WHERE deleted=0 ORDER BY created DESC LIMIT 50 OFFSET :offset")
    LiveData<List<Person>> getPersonByPage(int offset);
}
