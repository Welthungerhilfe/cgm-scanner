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

    private String village;

    private String aganwadi;

    private String villageName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
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
}
