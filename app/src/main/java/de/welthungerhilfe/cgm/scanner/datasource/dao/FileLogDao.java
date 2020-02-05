package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_FILE_LOG;

@Dao
public interface FileLogDao {
    @Insert(onConflict = REPLACE)
    void saveFileLog(FileLog log);

    @Update(onConflict = REPLACE)
    void updateFileLog(FileLog log);

    @Delete
    void deleteFileLog(FileLog log);

    @Query("SELECT * FROM " + TABLE_FILE_LOG + " WHERE deleted=0 LIMIT 15")
    List<FileLog> loadQueuedData();

    @Query("SELECT * FROM " + TABLE_FILE_LOG + " WHERE createDate>:timestamp")
    List<FileLog> getSyncableData(long timestamp);

    @Query("SELECT COUNT(id) FROM " + TABLE_FILE_LOG + " WHERE deleted=0")
    long getArtifactCount();

    @Query("SELECT SUM(fileSize)/1024/1024 FROM " + TABLE_FILE_LOG + " WHERE deleted=0")
    double getArtifactFileSize();

    @Query("SELECT COUNT(id) FROM " + TABLE_FILE_LOG + " WHERE deleted=1")
    long getDeletedArtifactCount();

    @Query("SELECT COUNT(id) FROM " + TABLE_FILE_LOG)
    long getTotalArtifactCount();

    @Query("SELECT SUM(fileSize)/1024/1024 FROM " + TABLE_FILE_LOG)
    double getTotalArtifactFileSize();

    @Query("SELECT SUM(fileSize)/1024/1024 FROM " + TABLE_FILE_LOG + " WHERE measureId=:measureId")
    double getMeasureArtifactSize(String measureId);

    @Query("SELECT SUM(fileSize)/1024/1024 FROM " + TABLE_FILE_LOG + " WHERE measureId=:measureId AND deleted=1")
    double getMeasureArtifactUploadedSize(String measureId);

    @Query("SELECT * FROM " + TABLE_FILE_LOG)
    List<FileLog> getAll();
}
