package de.welthungerhilfe.cgm.scanner.datasource.models;

public class Device {
    private String id;
    private String uuid;
    private long create_timestamp;
    private long sync_timestamp;
    private double new_artifact_file_size_mb;
    private long new_artifacts;
    private long deleted_artifacts;
    private double total_artifact_file_size_mb;
    private long total_artifacts;
    private long own_measures;
    private long own_persons;
    private String created_by;
    private long total_measures;
    private long total_persons;
    private String app_version;
    private int schema_version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getCreate_timestamp() {
        return create_timestamp;
    }

    public void setCreate_timestamp(long create_timestamp) {
        this.create_timestamp = create_timestamp;
    }

    public long getSync_timestamp() {
        return sync_timestamp;
    }

    public void setSync_timestamp(long sync_timestamp) {
        this.sync_timestamp = sync_timestamp;
    }

    public double getNew_artifact_file_size_mb() {
        return new_artifact_file_size_mb;
    }

    public void setNew_artifact_file_size_mb(double new_artifact_file_size_mb) {
        this.new_artifact_file_size_mb = new_artifact_file_size_mb;
    }

    public long getNew_artifacts() {
        return new_artifacts;
    }

    public void setNew_artifacts(long new_artifacts) {
        this.new_artifacts = new_artifacts;
    }

    public long getDeleted_artifacts() {
        return deleted_artifacts;
    }

    public void setDeleted_artifacts(long deleted_artifacts) {
        this.deleted_artifacts = deleted_artifacts;
    }

    public double getTotal_artifact_file_size_mb() {
        return total_artifact_file_size_mb;
    }

    public void setTotal_artifact_file_size_mb(double total_artifact_file_size_mb) {
        this.total_artifact_file_size_mb = total_artifact_file_size_mb;
    }

    public long getTotal_artifacts() {
        return total_artifacts;
    }

    public void setTotal_artifacts(long total_artifacts) {
        this.total_artifacts = total_artifacts;
    }

    public long getOwn_measures() {
        return own_measures;
    }

    public void setOwn_measures(long own_measures) {
        this.own_measures = own_measures;
    }

    public long getOwn_persons() {
        return own_persons;
    }

    public void setOwn_persons(long own_persons) {
        this.own_persons = own_persons;
    }

    public String getCreated_by() {
        return created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public long getTotal_measures() {
        return total_measures;
    }

    public void setTotal_measures(long total_measures) {
        this.total_measures = total_measures;
    }

    public long getTotal_persons() {
        return total_persons;
    }

    public void setTotal_persons(long total_persons) {
        this.total_persons = total_persons;
    }

    public String getApp_version() {
        return app_version;
    }

    public void setApp_version(String app_version) {
        this.app_version = app_version;
    }

    public int getSchema_version() {
        return schema_version;
    }

    public void setSchema_version(int schema_version) {
        this.schema_version = schema_version;
    }
}
