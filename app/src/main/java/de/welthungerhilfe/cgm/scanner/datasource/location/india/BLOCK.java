package de.welthungerhilfe.cgm.scanner.datasource.location.india;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class BLOCK {
    @Expose
    @SerializedName("VILLAGE")
    public ArrayList<VILLAGE> vILLAGE;
    @Expose
    public String country;
    @Expose
    public String id;
    @Expose
    public String location_name;
    @Expose
    public String location_type;
    @Expose
    public String parent_id;
    @Expose
    public String tree_string;

    public ArrayList<VILLAGE> getvILLAGE() {
        return vILLAGE;
    }

    public void setvILLAGE(ArrayList<VILLAGE> vILLAGE) {
        this.vILLAGE = vILLAGE;
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

    public String getParent_id() {
        return parent_id;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getTree_string() {
        return tree_string;
    }

    public void setTree_string(String tree_string) {
        this.tree_string = tree_string;
    }
}
