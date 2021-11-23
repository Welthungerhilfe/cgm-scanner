package de.welthungerhilfe.cgm.scanner.datasource.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;


import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.Workflow;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_WORKFLOWS;

@Dao
public interface WorkfolwDao {

    @Insert(onConflict = REPLACE)
    void insertPerson(Workflow workflow);

    @Query("SELECT id FROM " + TABLE_WORKFLOWS + " WHERE name=:name AND version=:version LIMIT 1")
    String getWorkFlowId(String name, String version);
}
