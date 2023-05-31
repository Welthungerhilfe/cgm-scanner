package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultBoundingBox extends Results {

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
        String appBoundingBox;

        public void setAppBoundingBox(String boundingBox) {
            this.appBoundingBox = boundingBox;
        }


    }
}
