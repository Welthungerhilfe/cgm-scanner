package de.welthungerhilfe.cgm.scanner.datasource.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultAutoDetect extends Results {

    @SerializedName("data")
    @Expose
    Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data{
        @Expose
        boolean auto_detect = false;

        public boolean isAuto_detect() {
            return auto_detect;
        }

        public void setAuto_detect(boolean auto_detect) {
            this.auto_detect = auto_detect;
        }
    }
}
