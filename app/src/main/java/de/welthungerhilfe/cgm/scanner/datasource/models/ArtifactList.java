package de.welthungerhilfe.cgm.scanner.datasource.models;

import java.util.List;

public class ArtifactList {
    private String measure_id;
    private long total;
    private long start;
    private long end;
    private List<FileLog> artifacts;

    public String getMeasure_id() {
        return measure_id;
    }

    public void setMeasure_id(String measure_id) {
        this.measure_id = measure_id;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public List<FileLog> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<FileLog> artifacts) {
        this.artifacts = artifacts;
    }
}
