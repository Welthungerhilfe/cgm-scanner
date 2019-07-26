package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;


import de.welthungerhilfe.cgm.scanner.datasource.models.Artifact_quality;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface Artifact_qualityDao {
    @Insert(onConflict = REPLACE)
    void insertArtifact_quality(Artifact_quality artifact_quality);
}
