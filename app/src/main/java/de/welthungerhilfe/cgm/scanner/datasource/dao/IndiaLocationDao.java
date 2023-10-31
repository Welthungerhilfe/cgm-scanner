package de.welthungerhilfe.cgm.scanner.datasource.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.location.india.IndiaLocation;


@Dao
public interface IndiaLocationDao {

    @Insert(onConflict = REPLACE)
    void insertIndianLocation(IndiaLocation indiaLocation);

    @Update(onConflict = REPLACE)
    void updateIndianLocation(IndiaLocation indiaLocation);

    @Query("SELECT DISTINCT village_full_name FROM india_location WHERE environment=:environment")
    List<String> getVillage(int environment);

    @Query("SELECT aganwadi FROM india_location WHERE village_full_name =:village_full_name AND environment=:environment")
    List<String> getAganwadi(String village_full_name, int environment);

    @Query("SELECT * FROM india_location WHERE villageName =:villageName AND environment=:environment LIMIT 1")
    IndiaLocation getVillageObject(String villageName, int environment);

    @Query("SELECT * FROM india_location WHERE village_full_name =:village_full_name AND aganwadi=:aganwadi AND environment=:environment LIMIT 1")
    IndiaLocation getCenterLocationId(String village_full_name, String aganwadi, int environment);

    @Query("SELECT * FROM india_location WHERE id =:id AND environment=:environment LIMIT 1")
    IndiaLocation getLocationFromId(String id, int environment);
}
