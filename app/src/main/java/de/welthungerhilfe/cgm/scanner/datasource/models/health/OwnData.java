package de.welthungerhilfe.cgm.scanner.datasource.models.health;

public class OwnData {
    private long own_persons;
    private long own_measures;
    private long artifacts;
    private long deleted_artifacts;
    private long total_artifacts;
    private double artifact_file_size_mb;
    private double total_artifact_file_size_mb;

    public long getOwn_persons() {
        return own_persons;
    }

    public void setOwn_persons(long own_persons) {
        this.own_persons = own_persons;
    }

    public long getOwn_measures() {
        return own_measures;
    }

    public void setOwn_measures(long own_measures) {
        this.own_measures = own_measures;
    }

    public long getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(long artifacts) {
        this.artifacts = artifacts;
    }

    public long getDeleted_artifacts() {
        return deleted_artifacts;
    }

    public void setDeleted_artifacts(long deleted_artifacts) {
        this.deleted_artifacts = deleted_artifacts;
    }

    public long getTotal_artifacts() {
        return total_artifacts;
    }

    public void setTotal_artifacts(long total_artifacts) {
        this.total_artifacts = total_artifacts;
    }

    public double getArtifact_file_size_mb() {
        return artifact_file_size_mb;
    }

    public void setArtifact_file_size_mb(double artifact_file_size_mb) {
        this.artifact_file_size_mb = artifact_file_size_mb;
    }

    public double getTotal_artifact_file_size_mb() {
        return total_artifact_file_size_mb;
    }

    public void setTotal_artifact_file_size_mb(double total_artifact_file_size_mb) {
        this.total_artifact_file_size_mb = total_artifact_file_size_mb;
    }
}
