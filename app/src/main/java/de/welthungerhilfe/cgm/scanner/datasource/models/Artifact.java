package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Artifact {


    private long timestamp;


    @SerializedName("timestamp")
    @Expose
    private String timestampString;

    @SerializedName("order")
    @Expose
    private int order;

    @SerializedName("file")
    @Expose
    private String file;

    @SerializedName("format")
    @Expose
    private String format;


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFormat() {
        return format;
    }

    public String getTimestampString() {
        return timestampString;
    }

    public void setTimestampString(String timestampString) {
        this.timestampString = timestampString;
    }

    public void setFormat(String format) {
        this.format = format;
    }


}
