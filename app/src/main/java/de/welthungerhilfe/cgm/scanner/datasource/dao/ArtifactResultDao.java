package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_ARTIFACT_RESULT;

@Dao
public interface ArtifactResultDao {
    @Insert(onConflict = REPLACE)
    void insertArtifact_quality(ArtifactResult artifactResuslt);

    @Query("SELECT COUNT(*) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND `key`=:key")
    int getPointCloudCount(String measure_id, int key);

    @Query("SELECT COUNT(*) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=100 OR `key`=200)")
    int getPointCloudCountForFront(String measure_id);

    @Query("SELECT COUNT(*) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=101 OR `key`=201)")
    int getPointCloudCountForSide(String measure_id);

    @Query("SELECT COUNT(*) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=102 OR `key`=202)")
    int getPointCloudCountForBack(String measure_id);

    @Query("SELECT AVG(real) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND `key`=:key")
    double getAveragePointCount(String measure_id, int key);

    @Query("SELECT AVG(real) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=100 OR `key`=200)")
    double getAveragePointCountForFront(String measure_id);

    @Query("SELECT AVG(real) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=101 OR `key`=201)")
    double getAveragePointCountForSide(String measure_id);

    @Query("SELECT AVG(real) FROM " + TABLE_ARTIFACT_RESULT + " WHERE measure_id=:measure_id AND (`key`=102 OR `key`=202)")
    double getAveragePointCountForBack(String measure_id);

    @Query("SELECT * FROM " + TABLE_ARTIFACT_RESULT)
    List<ArtifactResult> getAll();
}


