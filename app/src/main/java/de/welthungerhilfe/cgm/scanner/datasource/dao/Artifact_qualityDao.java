package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;

import android.arch.persistence.room.Query;
import de.welthungerhilfe.cgm.scanner.datasource.models.Artifact_quality;
import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_ARTIFACT_QUALITY;

@Dao
public interface Artifact_qualityDao {
    @Insert(onConflict = REPLACE)
    void insertArtifact_quality(Artifact_quality artifact_quality);

    @Query("SELECT * FROM " + TABLE_ARTIFACT_QUALITY + " WHERE artifact_id=:artifact_id")
    LiveData<List<Artifact_quality>> getArtifactQuality(String artifact_id);
}
