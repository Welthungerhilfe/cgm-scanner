package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import androidx.room.Query;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.location.india.IndiaLocation;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class IndiaLocationRepository {

    private static IndiaLocationRepository instance;

    private CgmDatabase database;
    private SessionManager session;
    private boolean updated;

    private IndiaLocationRepository(Context context){
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
        updated = true;
    }

    public static IndiaLocationRepository getInstance(Context context) {
        if (instance == null) {
            instance = new IndiaLocationRepository(context);
        }
        return instance;
    }


    public void insertIndiaLocation(IndiaLocation indiaLocation) {
        database.indiaLocationDao().insertIndianLocation(indiaLocation);
        setUpdated(true);
    }

    public void updateIndiaLocation(IndiaLocation indiaLocation) {
        database.indiaLocationDao().updateIndianLocation(indiaLocation);
        setUpdated(true);
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }


    public List<String> getVillage(int environment){
       return database.indiaLocationDao().getVillage(environment);
    }


    public List<String> getAganwadi(String village, int environment){
        return database.indiaLocationDao().getAganwadi(village,environment);
    }


    public IndiaLocation getVillageObject(String villageName, int environment){
        return database.indiaLocationDao().getVillageObject(villageName,environment);
    }

    public IndiaLocation getCenterLocationId(String village, String aganwadi, int environment){
        return database.indiaLocationDao().getCenterLocationId(village, aganwadi,environment);

    }
    public IndiaLocation getLocationFromId(String id, int environment){
        return database.indiaLocationDao().getLocationFromId(id,environment);

    }

}
