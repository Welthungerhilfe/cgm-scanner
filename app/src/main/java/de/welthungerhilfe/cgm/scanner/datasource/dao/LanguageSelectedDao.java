package de.welthungerhilfe.cgm.scanner.datasource.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_LANGUAGE_SELECTED;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import de.welthungerhilfe.cgm.scanner.datasource.models.LanguageSelected;

@Dao
public interface LanguageSelectedDao {

    @Insert(onConflict = REPLACE)
    void insertLanguageSelected(LanguageSelected languageSelected);

    @Update(onConflict = REPLACE)
    void updateLanguageSelected(LanguageSelected languageSelected);

    @Query("SELECT * FROM " + TABLE_LANGUAGE_SELECTED + " WHERE id=:id")
    LanguageSelected getLanguageSelectedId(String id);
}
