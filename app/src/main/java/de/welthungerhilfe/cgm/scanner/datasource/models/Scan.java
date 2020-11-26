package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Scan implements Serializable {

    private List<Artifact> artifacts;
    private Loc location;
    private int type;
    private long scan_start;
    private long scan_end;
    private String version;

    @SerializedName("person")
    @Expose
    private String personServerKey;

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public Loc getLocation() {
        return location;
    }

    public void setLocation(Loc location) {
        this.location = location;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPersonServerKey() {
        return personServerKey;
    }

    public void setPersonServerKey(String personServerKey) {
        this.personServerKey = personServerKey;
    }

    public long getScan_start() {
        return scan_start;
    }

    public void setScan_start(long scan_start) {
        this.scan_start = scan_start;
    }

    public long getScan_end() {
        return scan_end;
    }

    public void setScan_end(long scan_end) {
        this.scan_end = scan_end;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
