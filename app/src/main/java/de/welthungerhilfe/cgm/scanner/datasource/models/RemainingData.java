package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

public class RemainingData {
    @Expose
    String device_id;
    @Expose
    String artifact;
    @Expose
    String measure;
    @Expose
    String person;
    @Expose
    String consent;
    @Expose
    String version;
    @Expose
    String user;
    @Expose
    String error;
    @Expose
    String scan;

    @Expose
    String app_auto_detect;
    @Expose
    String app_height;
    @Expose
    String app_light_score;
    @Expose
    String app_pose_score;
    @Expose
    String app_distance;
    @Expose
    String app_bounding_box;

    @Expose
    String app_orientation;





    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getConsent() {
        return consent;
    }

    public void setConsent(String consent) {
        this.consent = consent;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getScan() {
        return scan;
    }

    public void setScan(String scan) {
        this.scan = scan;
    }

    public String getApp_auto_detect() {
        return app_auto_detect;
    }

    public void setApp_auto_detect(String app_auto_detect) {
        this.app_auto_detect = app_auto_detect;
    }

    public String getApp_height() {
        return app_height;
    }

    public void setApp_height(String app_height) {
        this.app_height = app_height;
    }

    public String getApp_light_score() {
        return app_light_score;
    }

    public void setApp_light_score(String app_light_score) {
        this.app_light_score = app_light_score;
    }

    public String getApp_pose_Score() {
        return app_pose_score;
    }

    public void setApp_pose_Score(String app_pose_score) {
        this.app_pose_score = app_pose_score;
    }

    public String getApp_distance() {
        return app_distance;
    }

    public void setApp_distance(String app_distance) {
        this.app_distance = app_distance;
    }

    public String getApp_bounding_box() {
        return app_bounding_box;
    }

    public void setApp_bounding_box(String app_bounding_box) {
        this.app_bounding_box = app_bounding_box;
    }

    public String getApp_orientation() {
        return app_orientation;
    }

    public void setApp_orientation(String app_orientation) {
        this.app_orientation = app_orientation;
    }
}
