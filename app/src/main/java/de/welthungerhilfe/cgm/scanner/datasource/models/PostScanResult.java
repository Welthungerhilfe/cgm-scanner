package de.welthungerhilfe.cgm.scanner.datasource.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_POST_SCAN_RESULT;

@Entity(tableName = TABLE_POST_SCAN_RESULT)
public class PostScanResult {

    @PrimaryKey
    @NonNull
    private String id;

    private String measure_id;

    private boolean isSynced = false;

    private long timestamp;

    private int environment;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getMeasure_id() {
        return measure_id;
    }

    public void setMeasure_id(String measure_id) {
        this.measure_id = measure_id;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }
}
