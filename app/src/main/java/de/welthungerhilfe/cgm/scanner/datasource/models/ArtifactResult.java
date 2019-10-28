package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.errorprone.annotations.ForOverride;

import java.io.Serializable;

import static android.arch.persistence.room.ForeignKey.CASCADE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_ARTIFACT_RESULT;

@Entity(tableName = TABLE_ARTIFACT_RESULT)
public class ArtifactResult implements Serializable {
    private String type;
    private int key;
    private double real;
    private String confidence_value;
    private String misc;

    @NonNull
    @ForeignKey(entity = Measure.class, parentColumns = "id", childColumns = "measure_id", onDelete = CASCADE, onUpdate = CASCADE)
    private String measure_id;

    @PrimaryKey
    @NonNull
    private String artifact_id;

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
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

    @NonNull
    public String getMeasure_id() {
        return measure_id;
    }

    public void setMeasure_id(@NonNull String measure_id) {
        this.measure_id = measure_id;
    }

}