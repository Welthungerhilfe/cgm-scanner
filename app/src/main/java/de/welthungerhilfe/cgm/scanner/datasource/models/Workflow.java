package de.welthungerhilfe.cgm.scanner.datasource.models;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.repository.CsvExportableModel;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_WORKFLOWS;


@Entity(tableName = TABLE_WORKFLOWS)
public class Workflow extends CsvExportableModel implements Serializable {

    @PrimaryKey
    @Expose
    @NonNull
    private String id;

    @Expose
    private boolean eligible = false;

    @Expose
    private String name;

    @Expose
    private String result_binding;

    @Expose
    private String result_format;

    @Expose
    private String version;

    @Expose
    @Embedded
    private Data data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResult_binding() {
        return result_binding;
    }

    public void setResult_binding(String result_binding) {
        this.result_binding = result_binding;
    }

    public String getResult_format() {
        return result_format;
    }

    public void setResult_format(String result_format) {
        this.result_format = result_format;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public String getCsvFormattedString() {
        return String.format(Locale.US,"%s,%b,%s,%s,%s",id,eligible,name,result_binding,result_format,version);
    }

    @Override
    public String getCsvHeaderString() {
        return "id,eligible,name,result_binding,result_format,version";
    }

    public class Data{
        @Expose
        private String input_format;

        @Expose
        private String model;

        public String getInput_format() {
            return input_format;
        }

        public void setInput_format(String input_format) {
            this.input_format = input_format;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
