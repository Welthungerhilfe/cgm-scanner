package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;

public class EstimatesValue implements Comparable<EstimatesValue> {
    @Expose
    public int value;

    @Expose
    public int confidence;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }


    @Override
    public int compareTo(EstimatesValue o) {
        int compareQuantity = ((EstimatesValue) o).getValue();

        //ascending order
        return this.value - compareQuantity;
    }
}
