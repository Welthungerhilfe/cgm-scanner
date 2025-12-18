package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

public class AppConfig {

    @Expose
    String app_sec;

    public String getApp_sec() {
        return app_sec;
    }

    public void setApp_sec(String app_sec) {
        this.app_sec = app_sec;
    }


}