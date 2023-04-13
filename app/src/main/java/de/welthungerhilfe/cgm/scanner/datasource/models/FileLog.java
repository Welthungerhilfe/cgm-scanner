/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.datasource.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_FILE_LOG;

@Entity(tableName = TABLE_FILE_LOG)
public class FileLog extends CsvExportableModel implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;
    private String serverId;
    private String type; // calibration, consent, depth, rgb
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
    private int step;
    private int environment;
    private boolean childDetected;
    private float childHeight;
    private String scanServerId;
    private String artifactId;
    private boolean autoDetectSynced;
    private boolean childHeightSynced;
    private boolean poseScoreSynced;
    private float poseScore;
    private String poseCoordinates;
    private float child_distance;
    private float light_score;
    private boolean child_distance_synced;
    private boolean light_score_synced;

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

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }

    public boolean getChildDetected() {
        return childDetected;
    }

    public void setChildDetected(boolean childDetected) {
        this.childDetected = childDetected;
    }

    public float getChildHeight() {
        return childHeight;
    }

    public void setChildHeight(float childHeight) {
        this.childHeight = childHeight;
    }


    public String getScanServerId() {
        return scanServerId;
    }

    public void setScanServerId(String scanServerId) {
        this.scanServerId = scanServerId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public boolean getAutoDetectSynced() {
        return autoDetectSynced;
    }

    public void setAutoDetectSynced(boolean autoDetectSynced) {
        this.autoDetectSynced = autoDetectSynced;
    }
    public void setChildHeightSynced(boolean childHeightSynced) {
        this.childHeightSynced = childHeightSynced;
    }

    public boolean isChildHeightSynced() {
        return childHeightSynced;
    }

    public float getPoseScore() {
        return poseScore;
    }

    public void setPoseScore(float poseScore) {
        this.poseScore = poseScore;
    }

    public String getPoseCoordinates() {
        return poseCoordinates;
    }

    public void setPoseCoordinates(String poseCoordinates) {
        this.poseCoordinates = poseCoordinates;
    }

    public boolean isPoseScoreSynced() {
        return poseScoreSynced;
    }

    public void setPoseScoreSynced(boolean poseScoreSynced) {
        this.poseScoreSynced = poseScoreSynced;
    }

    public float getChild_distance() {
        return child_distance;
    }

    public void setChild_distance(float child_distance) {
        this.child_distance = child_distance;
    }

    public float getLight_score() {
        return light_score;
    }

    public void setLight_score(float light_score) {
        this.light_score = light_score;
    }

    public boolean isChild_distance_synced() {
        return child_distance_synced;
    }

    public void setChild_distance_synced(boolean child_distance_synced) {
        this.child_distance_synced = child_distance_synced;
    }

    public boolean isLight_score_synced() {
        return light_score_synced;
    }

    public void setLight_score_synced(boolean light_score_synced) {
        this.light_score_synced = light_score_synced;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US, "%s,%s,%s,%s,%d,%d,%b,%s,%d,%s,%d,%d,%d,%s,%d,%s,%d,%s,%f,%s,%s,%b",
                id,type,path,hashValue.replace("\n", "").replace("\r", ""),
                fileSize,uploadDate,deleted,qrCode,createDate,createdBy,status,age,schema_version,measureId,
                step,serverId,environment,childDetected,childHeight,scanServerId,artifactId,autoDetectSynced);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,type,path,hashValue,fileSize,uploadDate,deleted,qrCode,createDate,createdBy,status,age," +
               "schema_version,measureId,step,serverId,environment,childDetected,childHeight,scanServerId,artifactId,autoDetectSynced";
    }
}
