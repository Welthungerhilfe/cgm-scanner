package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Scan implements Serializable {

    @SerializedName("artifacts")
    @Expose
    private List<Artifact> artifacts;

    @SerializedName("location")
    @Expose
    private Loc location;

    @SerializedName("type")
    @Expose
    private int type;

    @SerializedName("scan_start")
    @Expose
    private String scan_start;

    @SerializedName("scan_end")
    @Expose
    private String scan_end;

    @SerializedName("version")
    @Expose
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

    public String getScan_start() {
        return scan_start;
    }

    public void setScan_start(String scan_start) {
        this.scan_start = scan_start;
    }

    public String getScan_end() {
        return scan_end;
    }

    public void setScan_end(String scan_end) {
        this.scan_end = scan_end;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
