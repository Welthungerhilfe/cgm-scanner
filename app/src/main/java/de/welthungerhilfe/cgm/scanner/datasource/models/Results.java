package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Results {

    @SerializedName("id")
    @Expose
    String id;

    @SerializedName("scan")
    @Expose
    String scan;

    @SerializedName("workflow")
    @Expose
    String workflow;

    @SerializedName("generated")
    @Expose
    String generated;

    @SerializedName("source_artifacts")
    @Expose
    ArrayList<String> source_artifacts;

    @SerializedName("source_results")
    @Expose
    ArrayList<String> source_results;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScan() {
        return scan;
    }

    public void setScan(String scan) {
        this.scan = scan;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public ArrayList<String> getSource_artifacts() {
        return source_artifacts;
    }

    public void setSource_artifacts(ArrayList<String> source_artifacts) {
        this.source_artifacts = source_artifacts;
    }

    public String getGenerated() {
        return generated;
    }

    public void setGenerated(String generated) {
        this.generated = generated;
    }

    public ArrayList<String> getSource_results() {
        return source_results;
    }

    public void setSource_results(ArrayList<String> source_results) {
        this.source_results = source_results;
    }
}

