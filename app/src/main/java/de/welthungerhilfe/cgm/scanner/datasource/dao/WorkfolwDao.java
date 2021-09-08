package de.welthungerhilfe.cgm.scanner.datasource.dao;


import androidx.room.Dao;
import androidx.room.Insert;


import de.welthungerhilfe.cgm.scanner.datasource.models.Workflow;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface WorkfolwDao {

    @Insert(onConflict = REPLACE)
    void insertPerson(Workflow workflow);
}
