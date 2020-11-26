package de.welthungerhilfe.cgm.scanner.datasource.models;

public class Artifact {

    private long timestamp;
    private int order;
    private String file;
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

    public void setFormat(String order) {
        this.format = format;
    }
}
