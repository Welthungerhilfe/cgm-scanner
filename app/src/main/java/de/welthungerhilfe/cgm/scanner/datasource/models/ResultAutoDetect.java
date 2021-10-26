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

    public static class Data {
        @Expose
        boolean auto_detected = false;

        public boolean isAuto_detected() {
            return auto_detected;
        }

        public void setAuto_detected(boolean auto_detected) {
            this.auto_detected = auto_detected;
        }
    }

    @Override
    public String toString() {
        return "ResultAutoDetect{" +
                "data=" + data +
                ", id='" + id + '\'' +
                ", scan='" + scan + '\'' +
                ", source_artifacts=" + source_artifacts +
                '}';
    }
}