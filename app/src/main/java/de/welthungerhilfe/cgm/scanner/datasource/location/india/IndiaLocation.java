package de.welthungerhilfe.cgm.scanner.datasource.location.india;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_INDIA_LOCATION;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = TABLE_INDIA_LOCATION)
public class IndiaLocation {

    @PrimaryKey
    @NonNull
    private String id;

    private String village_full_name;

    private String aganwadi;

    private String villageName;

    private String location_id;

    private int environment;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVillage_full_name() {
        return village_full_name;
    }

    public void setVillage_full_name(String village_full_name) {
        this.village_full_name = village_full_name;
    }

    public String getAganwadi() {
        return aganwadi;
    }

    public void setAganwadi(String aganwadi) {
        this.aganwadi = aganwadi;
    }

    public String getVillageName() {
        return villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public String getLocation_id() {
        return location_id;
    }

    public void setLocation_id(String location_id) {
        this.location_id = location_id;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }
}
