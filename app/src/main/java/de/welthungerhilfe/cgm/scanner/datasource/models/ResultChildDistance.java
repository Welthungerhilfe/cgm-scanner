package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultChildDistance extends Results{


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
        double child_distance;

        public double getChild_distance() {
            return child_distance;
        }

        public void setChild_distance(double child_distance) {
            this.child_distance = child_distance;
        }
    }
}
