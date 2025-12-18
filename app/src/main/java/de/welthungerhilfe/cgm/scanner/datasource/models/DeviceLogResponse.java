package de.welthungerhilfe.cgm.scanner.datasource.models;

public class DeviceLogResponse {

    private String id;
    private String device_id;
    private String file_id;
    private String log_type;
    private String log_data_from;
    private String log_data_to;
    private String generated_at;
    private String uploaded_at;
    private String app_version;
    private String device_info;
    private long file_size_bytes;
    private boolean processed;
    private String created_by;

    public String getId() {
        return id;
    }

    public String getDevice_id() {
        return device_id;
    }

    public String getFile_id() {
        return file_id;
    }

    public String getLog_type() {
        return log_type;
    }

    public String getLog_data_from() {
        return log_data_from;
    }

    public String getLog_data_to() {
        return log_data_to;
    }

    public String getGenerated_at() {
        return generated_at;
    }

    public String getUploaded_at() {
        return uploaded_at;
    }

    public String getApp_version() {
        return app_version;
    }

    public String getDevice_info() {
        return device_info;
    }

    public long getFile_size_bytes() {
        return file_size_bytes;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getCreated_by() {
        return created_by;
    }
}