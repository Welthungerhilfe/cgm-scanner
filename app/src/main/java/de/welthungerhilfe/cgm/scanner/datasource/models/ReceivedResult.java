package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class ReceivedResult implements Serializable {

    @Expose
    double artifact_max_99_percentile_neg_error;

    @Expose
    double artifact_max_99_percentile_pos_error;

    @Expose
    String height_diagnosis;

    @Expose
    double mae;

    @Expose
    double mean_height;

    public double getArtifact_max_99_percentile_neg_error() {
        return artifact_max_99_percentile_neg_error;
    }

    public void setArtifact_max_99_percentile_neg_error(double artifact_max_99_percentile_neg_error) {
        this.artifact_max_99_percentile_neg_error = artifact_max_99_percentile_neg_error;
    }

    public double getArtifact_max_99_percentile_pos_error() {
        return artifact_max_99_percentile_pos_error;
    }

    public void setArtifact_max_99_percentile_pos_error(double artifact_max_99_percentile_pos_error) {
        this.artifact_max_99_percentile_pos_error = artifact_max_99_percentile_pos_error;
    }

    public String getHeight_diagnosis() {
        return height_diagnosis;
    }

    public void setHeight_diagnosis(String height_diagnosis) {
        this.height_diagnosis = height_diagnosis;
    }

    public double getMae() {
        return mae;
    }

    public void setMae(double mae) {
        this.mae = mae;
    }

    public double getMean_height() {
        return mean_height;
    }

    public void setMean_height(double mean_height) {
        this.mean_height = mean_height;
    }
}
