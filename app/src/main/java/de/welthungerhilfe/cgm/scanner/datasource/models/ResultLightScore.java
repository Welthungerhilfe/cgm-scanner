package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultLightScore extends Results{


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
        float light_score;

        public float getLight_score() {
            return light_score;
        }

        public void setLight_score(float light_score) {
            this.light_score = light_score;
        }
    }
}
