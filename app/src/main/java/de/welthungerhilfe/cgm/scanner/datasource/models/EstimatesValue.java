package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

public class EstimatesValue implements Comparable<EstimatesValue> {
    @Expose
    public float value;

    @Expose
    public float confidence;

    public float getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }


    @Override
    public int compareTo(EstimatesValue o) {
        float compareQuantity = o.getValue();

        //ascending order
        return (int) ((this.value - compareQuantity) * 1000000);
    }
}
