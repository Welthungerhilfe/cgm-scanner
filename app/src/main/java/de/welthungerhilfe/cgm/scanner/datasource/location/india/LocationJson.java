package de.welthungerhilfe.cgm.scanner.datasource.location.india;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class LocationJson {
    @Expose
    @SerializedName("DISTRICT")
    public ArrayList<DISTRICT> dISTRICT;
    @Expose
    public String country;
    @Expose
    public String id;
    @Expose
    public String location_name;
    @Expose
    public String location_type;
    @Expose
    public Object parent_id;
    @Expose
    public String tree_string;


    public ArrayList<DISTRICT> getdISTRICT() {
        return dISTRICT;
    }

    public void setdISTRICT(ArrayList<DISTRICT> dISTRICT) {
        this.dISTRICT = dISTRICT;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation_name() {
        return location_name;
    }

    public void setLocation_name(String location_name) {
        this.location_name = location_name;
    }

    public String getLocation_type() {
        return location_type;
    }

    public void setLocation_type(String location_type) {
        this.location_type = location_type;
    }

    public Object getParent_id() {
        return parent_id;
    }

    public void setParent_id(Object parent_id) {
        this.parent_id = parent_id;
    }

    public String getTree_string() {
        return tree_string;
    }

    public void setTree_string(String tree_string) {
        this.tree_string = tree_string;
    }
}
