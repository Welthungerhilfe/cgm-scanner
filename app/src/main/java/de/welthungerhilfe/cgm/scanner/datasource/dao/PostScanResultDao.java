package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.PostScanResult;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_POST_SCAN_RESULT;

@Dao
public interface PostScanResultDao {

    @Insert(onConflict = REPLACE)
    void insertPostScanResult(PostScanResult postScanResult);

    @Update(onConflict = REPLACE)
    void updatePostScanResult(PostScanResult postScanResult);

    @Query("SELECT * FROM " + TABLE_POST_SCAN_RESULT + " WHERE isSynced=0 And environment=:environment ORDER By timestamp")
    List<PostScanResult> getSyncablePostScanResult(int environment);
}
