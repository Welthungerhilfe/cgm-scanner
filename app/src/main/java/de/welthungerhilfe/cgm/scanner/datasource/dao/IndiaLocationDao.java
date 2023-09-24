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

    @Query("SELECT DISTINCT village FROM india_location")
    List<String> getVillage();

    @Query("SELECT aganwadi FROM india_location WHERE village =:village")
    List<String> getAganwadi(String village);

    @Query("SELECT * FROM india_location WHERE villageName =:villageName LIMIT 1")
    IndiaLocation getVillageObject(String villageName);

    @Query("SELECT id FROM india_location WHERE village =:village AND aganwadi=:aganwadi LIMIT 1")
    String getCenterLocationId(String village, String aganwadi);

    @Query("SELECT * FROM india_location WHERE id =:id LIMIT 1")
    IndiaLocation getLocationFromId(String id);
}
