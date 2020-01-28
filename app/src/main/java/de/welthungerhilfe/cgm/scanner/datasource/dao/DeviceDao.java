package de.welthungerhilfe.cgm.scanner.datasource.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Device;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_DEVICE;

@Dao
public interface DeviceDao {
    @Query("SELECT * FROM " + TABLE_DEVICE + " WHERE sync_timestamp>:timestamp")
    List<Device> getSyncableDevice(long timestamp);

    @Insert(onConflict = REPLACE)
    void insertDevice(Device device);

    @Update(onConflict = REPLACE)
    void updateDevice(Device device);

    @Delete
    void deleteDevice(Device device);
}
