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

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;

@Entity(tableName = TABLE_PERSON)
public class Person extends CsvExportableModel implements Serializable {
    @PrimaryKey
    @NonNull
    @Expose(serialize = false, deserialize = false)
    private String id;  // firebase id

    @SerializedName("id")
    @Expose(serialize = false)
    private String serverId;

    private boolean isSynced = false;

    private boolean isDenied = false;

    private int environment;

    @SerializedName("name")
    @Expose
    private String name;

    private String surname;


    private long birthday;

    @SerializedName("date_of_birth")
    @Expose
    @Ignore
    private String birthdayString;

    @Expose
    private String sex;  // female, male, other

    @SerializedName("guardian")
    @Expose
    private String guardian;

    @SerializedName("age_estimated")
    @Expose
    private boolean isAgeEstimated;

    @SerializedName("qr_code")
    @Expose
    private String qrcode;

    private long created;

    @SerializedName("qr_scanned")
    @Expose
    @Ignore
    private String qr_scanned;

    @Expose
    @Ignore
    private String device_updated_at;

    private long device_updated_at_timestamp;

    //below fileld is used for geeting info about-> when last time manual measurments synced with persons.
    private long last_sync_measurments;

    @Expose
    private long timestamp;

    @Expose
    private String createdBy;

    @Expose
    private boolean deleted;

    @Expose
    private String deletedBy;

    @Expose
    private int schema_version;


    @SerializedName("lastLocation")
    @Expose
    @Embedded
    private Loc lastLocation;

    @Ignore
    private Measure lastMeasure;

    private long last_updated = 0;


    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getBirthdayString() {
        return birthdayString;
    }

    public void setBirthdayString(String birthdayString) {
        this.birthdayString = birthdayString;
    }

    public String getQr_scanned() {
        return qr_scanned;
    }

    public void setQr_scanned(String qr_scanned) {
        this.qr_scanned = qr_scanned;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public String getSurname() {
        return surname;
    }

    @Deprecated
    public void setSurname(String surname) {
        this.surname = surname;
    }

    public long getBirthday() {
        return birthday;
    }

    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getGuardian() {
        return guardian;
    }

    public void setGuardian(String guardian) {
        this.guardian = guardian;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public Measure getLastMeasure() {
        return lastMeasure;
    }

    public void setLastMeasure(Measure lastMeasure) {
        this.lastMeasure = lastMeasure;
    }

    public Loc getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Loc lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isAgeEstimated() {
        return isAgeEstimated;
    }

    public void setAgeEstimated(boolean ageEstimated) {
        isAgeEstimated = ageEstimated;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
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

    public int getSchema_version() {
        return schema_version;
    }

    public void setSchema_version(int schema_version) {
        this.schema_version = schema_version;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }

    public boolean isDenied() {
        return isDenied;
    }

    public void setDenied(boolean denied) {
        isDenied = denied;
    }

    public String getDevice_updated_at() {
        return device_updated_at;
    }

    public void setDevice_updated_at(String device_updated_at) {
        this.device_updated_at = device_updated_at;
    }

    public long getDevice_updated_at_timestamp() {
        return device_updated_at_timestamp;
    }

    public void setDevice_updated_at_timestamp(long device_updated_at_timestamp) {
        this.device_updated_at_timestamp = device_updated_at_timestamp;
    }

    public long getLast_sync_measurments() {
        return last_sync_measurments;
    }

    public void setLast_sync_measurments(long last_sync_measurments) {
        this.last_sync_measurments = last_sync_measurments;
    }

    public String getFullName() {
        String fullname = getName();
        if (getSurname() != null && !getSurname().isEmpty()) {
            fullname = getName() + " " + getSurname();
        }
        return fullname;
    }

    public long getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(long last_updated) {
        this.last_updated = last_updated;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US, "%s,%s,%s,%d,%s,%s,%b,%s,%d,%d,%s,%b,%s,%d,%s,%d,%b,%s,%d,%d", id, name, surname, birthday, sex, guardian, isAgeEstimated, qrcode, created, timestamp, createdBy, deleted, deletedBy, schema_version, serverId, environment, isDenied, device_updated_at,device_updated_at_timestamp,last_sync_measurments);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,name,surname,birthday,sex,guardian,isAgeEstimated,qrcode,created,timestamp,createdBy,deleted,deletedBy,schema_version,serverId,environment,isDenied,device_updated_at,last_sync_measurments";
    }
}
