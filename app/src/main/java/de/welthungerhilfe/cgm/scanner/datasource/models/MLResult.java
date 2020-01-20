package de.welthungerhilfe.cgm.scanner.datasource.models;

public class MLResult {
    private float mean;
    private float min;
    private float max;
    private float std;

    public float getMean() {
        return mean;
    }

    public void setMean(float mean) {
        this.mean = mean;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getStd() {
        return std;
    }

    public void setStd(float std) {
        this.std = std;
    }
}
