/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.datasource.models;

import android.os.Build;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;

import static androidx.room.ForeignKey.CASCADE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_MEASURE;

@Entity(tableName = TABLE_MEASURE)
public class Measure extends CsvExportableModel implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;

    @SerializedName("id")
    @Expose(serialize = false)
    private String measureServerKey;

    @ForeignKey(entity = Person.class, parentColumns = "id", childColumns = "personId", onDelete = CASCADE, onUpdate = CASCADE)
    @Expose(serialize = false, deserialize = false)
    private String personId;

    @SerializedName("person")
    @Expose
    private String personServerKey;


    private long date;

    private int environment = 0;

    @SerializedName("measured")
    @Expose
    @Ignore
    private String measured;

    @Expose
    @Ignore
    private String measure_updated;

    private boolean isSynced = false;

    private String type;
    private long age; // age from birthday in days

    @Expose
    private double height;

    @Expose
    private double weight;

    @Expose
    private double muac;

    private double headCircumference;
    private String artifact;
    private boolean visible;

    @Expose
    private boolean oedema;
    private long timestamp;
    private String createdBy;
    private boolean deleted;
    private String deletedBy;
    @SerializedName("qr_code")
    @Expose
    private String qrCode;
    private int schema_version;
    private boolean artifact_synced;
    private long uploaded_at;
    private long resulted_at;
    private long received_at;
    private double heightConfidence;
    private double weightConfidence;
    private String scannedBy;

    @Expose
    private String std_test_qr_code;


    private double positive_height_error;

    private double negative_height_error;



    @Embedded
    @SerializedName("location")
    @Expose
    private Loc location;

    public String getMeasureServerKey() {
        return measureServerKey;
    }

    public void setMeasureServerKey(String measureServerKey) {
        this.measureServerKey = measureServerKey;
    }

    public String getMeasured() {
        return measured;
    }

    public void setMeasured(String measured) {
        this.measured = measured;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public String getPersonServerKey() {
        return personServerKey;
    }

    public void setPersonServerKey(String personServerKey) {
        this.personServerKey = personServerKey;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public double getHeight() {
        return height;
    }

    public double getHeightConfidence() {
        return heightConfidence;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setHeightConfidence(double heightConfidence) {
        this.heightConfidence = heightConfidence;
    }

    public double getWeight() {
        return weight;
    }

    public double getWeightConfidence() {
        return weightConfidence;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setWeightConfidence(double weightConfidence) {
        this.weightConfidence = weightConfidence;
    }

    public double getMuac() {
        return muac;
    }

    public void setMuac(double muac) {
        this.muac = muac;
    }

    public double getHeadCircumference() {
        return headCircumference;
    }

    public void setHeadCircumference(double headCircumference) {
        this.headCircumference = headCircumference;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public Loc getLocation() {
        return location;
    }

    public void setLocation(Loc location) {
        this.location = location;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isOedema() {
        return oedema;
    }

    public void setOedema(boolean oedema) {
        this.oedema = oedema;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public int getSchema_version() {
        return schema_version;
    }

    public void setSchema_version(int schema_version) {
        this.schema_version = schema_version;
    }

    public boolean isArtifact_synced() {
        return artifact_synced;
    }

    public void setArtifact_synced(boolean artifact_synced) {
        this.artifact_synced = artifact_synced;
    }

    public long getUploaded_at() {
        return uploaded_at;
    }

    public void setUploaded_at(long uploaded_at) {
        this.uploaded_at = uploaded_at;
    }

    public long getResulted_at() {
        return resulted_at;
    }

    public void setResulted_at(long resulted_at) {
        this.resulted_at = resulted_at;
    }

    public long getReceived_at() {
        return received_at;
    }

    public void setReceived_at(long received_at) {
        this.received_at = received_at;
    }

    @NonNull
    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(@NonNull String scannedBy) {
        this.scannedBy = scannedBy;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }

    public String getMeasure_updated() {
        return measure_updated;
    }

    public void setMeasure_updated(String measure_updated) {
        this.measure_updated = measure_updated;
    }

    public String getStd_test_qr_code() {
        return std_test_qr_code;
    }

    public void setStd_test_qr_code(String std_test_qr_code) {
        this.std_test_qr_code = std_test_qr_code;
    }

    public double getPositive_height_error() {
        return positive_height_error;
    }

    public void setPositive_height_error(double positive_height_error) {
        this.positive_height_error = positive_height_error;
    }

    public double getNegative_height_error() {
        return negative_height_error;
    }

    public void setNegative_height_error(double negative_height_error) {
        this.negative_height_error = negative_height_error;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US, "%s,%s,%d,%s,%d,%f,%f,%f,%f,%s,%b,%b,%d,%s,%b,%s,%s,%d,%b,%d,%d,%d,%f,%f,%s,%d,%s,%s", id, personId, date, type, age, height, weight,
                muac, headCircumference, artifact, visible, oedema, timestamp, createdBy, deleted, deletedBy, qrCode, schema_version, artifact_synced, uploaded_at, resulted_at, received_at,
                heightConfidence, weightConfidence, scannedBy, environment, std_test_qr_code, measure_updated);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,personId,date,type,age,height,weight,muac,headCircumference,artifact,visible,oedema,timestamp,createdBy,deleted,deletedBy,qrCode,schema_version,artifact_synced,uploaded_at,resulted_at,received_at,heightConfidence,weightConfidence,scannedBy,environment,st_qr_code,measure_updated";
    }

    public HashMap<Integer, Scan> split(FileLogRepository fileLogRepository, int environment) {

        //check if measure is ready to be synced
        HashMap<Integer, Scan> output = new HashMap<>();
        List<FileLog> measureArtifacts = fileLogRepository.getArtifactsForMeasure(getId(), environment);
        for (FileLog log : measureArtifacts) {
            if (log.getServerId() == null) {
                return output;
            }
        }

        //create structure to split artifacts by scan step
        FileLog calibration = null;
        for (FileLog log : measureArtifacts) {
            if (log.getStep() != 0) {
                if (!output.containsKey(log.getStep())) {
                    output.put(log.getStep(), new Scan());
                }
            } else {
                calibration = log;
            }
        }

        //fill the structure
        Set<Integer> keys = output.keySet();
        for (Integer key : keys) {

            //get the artifacts for a step
            List<Artifact> scanArtifacts = new ArrayList<>();
            for (FileLog log : measureArtifacts) {
                if ((key == 0) || (key == log.getStep())) {

                    String timestamp = log.getPath();
                    timestamp = timestamp.substring(timestamp.lastIndexOf('_') + 1);
                    timestamp = timestamp.substring(0, timestamp.lastIndexOf('.'));
                    int diff;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                        diff = (int) (Float.parseFloat(timestamp) * 10);
                    } else {
                        diff = Integer.parseInt(timestamp);
                    }

                    Artifact artifact = new Artifact();
                    artifact.setFile(log.getServerId());
                    artifact.setFormat(log.getType());
                    artifact.setOrder(diff);
                    artifact.setTimestamp(log.getCreateDate());
                    artifact.setTimestampString(DataFormat.convertMilliSeconsToServerDate(log.getCreateDate()));
                    scanArtifacts.add(artifact);
                }
            }

            //set the artifacts order
            Collections.sort(scanArtifacts, (a, b) -> {
                int diff = a.getOrder() - b.getOrder();
                if (diff == 0) {
                    return b.getFormat().compareTo(a.getFormat());
                } else {
                    return diff;
                }
            });

            //ordering from one
            int order = 0;
            int value = -1;
            for (Artifact artifact : scanArtifacts) {
                if (artifact.getOrder() == value) {
                    artifact.setOrder(order);
                } else {
                    value = artifact.getOrder();
                    artifact.setOrder(++order);
                }
            }

            //create scan object
            Scan scan = new Scan();
            scan.setArtifacts(scanArtifacts);
            scan.setLocation(getLocation());
            scan.setPersonServerKey(getPersonServerKey());
            scan.setScan_start(DataFormat.convertMilliSeconsToServerDate(getTimestamp()));
            scan.setScan_end(DataFormat.convertMilliSeconsToServerDate(getDate()));
            scan.setType(key);
            scan.setVersion(getType());
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setModel(getScannedBy());
            scan.setDevice_info(deviceInfo);
            scan.setStd_test_qr_code(getStd_test_qr_code());
            output.put(key, scan);
        }

        //add calibration file
        if (calibration != null) {
            for (Scan scan : output.values()) {
                Artifact artifact = new Artifact();
                artifact.setFile(calibration.getServerId());
                artifact.setFormat(calibration.getType());
                artifact.setOrder(0);
                artifact.setTimestamp(calibration.getCreateDate());
                artifact.setTimestampString(DataFormat.convertMilliSeconsToServerDate(calibration.getCreateDate()));
                scan.getArtifacts().add(0, artifact);
            }
        }
        return output;
    }
}
