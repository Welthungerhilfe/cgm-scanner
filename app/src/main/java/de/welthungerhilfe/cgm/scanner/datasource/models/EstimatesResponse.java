package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

import java.util.List;

public class EstimatesResponse {
    @Expose
    public List<EstimatesValue> height;
    @Expose
    public List<EstimatesValue> weight;
    @Expose
    public List<EstimatesValue> muac;

    public List<EstimatesValue> getHeight() {
        return height;
    }

    public void setHeight(List<EstimatesValue> height) {
        this.height = height;
    }

    public List<EstimatesValue> getWeight() {
        return weight;
    }

    public void setWeight(List<EstimatesValue> weight) {
        this.weight = weight;
    }

    public List<EstimatesValue> getMuac() {
        return muac;
    }

    public void setMuac(List<EstimatesValue> muac) {
        this.muac = muac;
    }
}
