package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_MEASURE_RESULT;

@Entity(tableName = TABLE_MEASURE_RESULT)
public class MeasureResult {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String measure_id;
    private String model_id;
    private String key;
    private float confidence_value;
    private float float_value;
    private String json_value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMeasure_id() {
        return measure_id;
    }

    public void setMeasure_id(@NonNull String measure_id) {
        this.measure_id = measure_id;
    }

    public String getModel_id() {
        return model_id;
    }

    public void setModel_id(String model_id) {
        this.model_id = model_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public float getConfidence_value() {
        return confidence_value;
    }

    public void setConfidence_value(float confidence_value) {
        this.confidence_value = confidence_value;
    }

    public float getFloat_value() {
        return float_value;
    }

    public void setFloat_value(float float_value) {
        this.float_value = float_value;
    }

    public String getJson_value() {
        return json_value;
    }

    public void setJson_value(String json_value) {
        this.json_value = json_value;
    }
}
