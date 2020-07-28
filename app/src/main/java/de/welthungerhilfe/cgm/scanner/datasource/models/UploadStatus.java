package de.welthungerhilfe.cgm.scanner.datasource.models;

import androidx.room.ColumnInfo;

public class UploadStatus {
    @ColumnInfo(name = "total")
    private double total;

    @ColumnInfo(name = "uploaded")
    private double uploaded;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getUploaded() {
        return uploaded;
    }

    public void setUploaded(double uploaded) {
        this.uploaded = uploaded;
    }
}
