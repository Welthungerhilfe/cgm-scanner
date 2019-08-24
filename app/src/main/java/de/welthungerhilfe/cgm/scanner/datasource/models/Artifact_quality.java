package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;

import static android.arch.persistence.room.ForeignKey.CASCADE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_ARTIFACT_QUALITY;


@Entity(tableName = TABLE_ARTIFACT_QUALITY)
public class Artifact_quality implements Serializable {
    private String type;
    private String key;
    private double real;
    private String confidence_value;
    private String misc;
    @PrimaryKey
    @NonNull
    @ForeignKey(entity = Measure.class, parentColumns = "id", childColumns = "artifact_id", onDelete = CASCADE, onUpdate = CASCADE)
    private String artifact_id;

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public double getReal() {
        return real;
    }

    public void setReal(double real) {
        this.real = real;
    }

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }

    @NonNull
    public String getArtifact_id() {
        return artifact_id;
    }

    public void setArtifact_id(@NonNull String artifact_id) {
        this.artifact_id = artifact_id;
    }

    public String getConfidence_value() {
        return confidence_value;
    }

    public void setConfidence_value(String confidence_value) {
        this.confidence_value = confidence_value;
    }
}
