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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Scan implements Serializable {

    @SerializedName("id")
    @Expose
    private String id;

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

    @SerializedName("device_info")
    @Expose
    private DeviceInfo device_info;

    @SerializedName("std_test_qr_code")
    @Expose
    private String std_test_qr_code;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public DeviceInfo getDevice_info() {
        return device_info;
    }

    public void setDevice_info(DeviceInfo device_info) {
        this.device_info = device_info;
    }

    public String getStd_test_qr_code() {
        return std_test_qr_code;
    }

    public void setStd_test_qr_code(String std_test_qr_code) {
        this.std_test_qr_code = std_test_qr_code;
    }


}
