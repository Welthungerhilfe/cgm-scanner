package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultAppScore extends Results {

    @SerializedName("data")
    @Expose
    Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        @Expose
        float poseScore;

        @Expose
        String poseCoordinates;

        public void setPoseScore(float poseScore) {
            this.poseScore = poseScore;
        }

        public void setPoseCoordinates(String poseCoordinates) {
            this.poseCoordinates = poseCoordinates;
        }
    }
}
