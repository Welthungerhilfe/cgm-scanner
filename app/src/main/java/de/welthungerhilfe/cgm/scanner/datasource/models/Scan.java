package de.welthungerhilfe.cgm.scanner.datasource.models;

import java.io.Serializable;

public class Scan implements Serializable {

    private ArtifactList artifacts;
    private int type;

    public ArtifactList getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(ArtifactList artifacts) {
        this.artifacts = artifacts;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
