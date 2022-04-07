package de.welthungerhilfe.cgm.scanner.datasource.models;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_LANGUAGE_SELECTED;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = TABLE_LANGUAGE_SELECTED)
public class LanguageSelected {
    @PrimaryKey
    @NonNull
    private String id;

    private String selectedLanguage;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getSelectedLanguage() {
        return selectedLanguage;
    }

    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage;
    }
}
