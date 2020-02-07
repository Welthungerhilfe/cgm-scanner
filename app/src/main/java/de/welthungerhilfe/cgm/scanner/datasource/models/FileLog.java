package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_FILE_LOG;

@Entity(tableName = TABLE_FILE_LOG)
public class FileLog extends CsvExportableModel implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;
    private String type; // consent, pcd, rgb
    private String path;
    private String hashValue;
    private long fileSize;
    private long uploadDate;
    private boolean deleted;
    private String qrCode;
    private long createDate;
    private String createdBy;
    private int status;
    private long age;
    private int schema_version;
    private String measureId;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public int getSchema_version() {
        return schema_version;
    }

    public void setSchema_version(int schema_version) {
        this.schema_version = schema_version;
    }

    public String getMeasureId() {
        return measureId;
    }

    public void setMeasureId(String measureId) {
        this.measureId = measureId;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US, "%s,%s,%s,%s,%d,%d,%b,%s,%d,%s,%d,%d,%d,%s",id,type,path,hashValue,fileSize,uploadDate,deleted,qrCode,createDate,createdBy,status,age,schema_version,measureId);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,type,path,hashValue,fileSize,uploadDate,deleted,qrCode,createDate,createdBy,status,age,schema_version,measureId";
    }
}
