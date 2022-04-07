package de.welthungerhilfe.cgm.scanner.datasource.repository;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_LANGUAGE_SELECTED;

import android.content.Context;

import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.LanguageSelected;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class LanguageSelectedRepository {

    private static LanguageSelectedRepository instance;
    private CgmDatabase database;
    private SessionManager session;

    private LanguageSelectedRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
    }

    public static LanguageSelectedRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageSelectedRepository(context);
        }
        return instance;
    }
    public void insertLanguageSelected(LanguageSelected languageSelected){
        database.languageSelectedDao().insertLanguageSelected(languageSelected);
    }


    public void updateLanguageSelected(LanguageSelected languageSelected){
        database.languageSelectedDao().updateLanguageSelected(languageSelected);
    }


    public String getLanguageSelectedId(String id){
        return database.languageSelectedDao().getLanguageBySelectedId(id);
    }
}
