package de.welthungerhilfe.cgm.scanner.datasource.location.india;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class Root {
    @Expose
    public String country;
    @Expose
    public ArrayList<LocationJson> location_json;
    @Expose
    public ArrayList<String> location_types;
    @Expose
    public int version;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public ArrayList<LocationJson> getLocation_json() {
        return location_json;
    }

    public void setLocation_json(ArrayList<LocationJson> location_json) {
        this.location_json = location_json;
    }

    public ArrayList<String> getLocation_types() {
        return location_types;
    }

    public void setLocation_types(ArrayList<String> location_types) {
        this.location_types = location_types;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}

