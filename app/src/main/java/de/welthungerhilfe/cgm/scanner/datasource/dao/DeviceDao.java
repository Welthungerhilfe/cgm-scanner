package de.welthungerhilfe.cgm.scanner.datasource.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Device;

import static androidx.room.OnConflictStrategy.REPLACE;
import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_DEVICE;

@Dao
public interface DeviceDao {
    @Query("SELECT * FROM " + TABLE_DEVICE + " WHERE sync_timestamp>:timestamp")
    List<Device> getSyncableDevice(long timestamp);

    @Insert(onConflict = REPLACE)
    void insertDevice(Device device);

    @Update(onConflict = REPLACE)
    void updateDevice(Device device);

    @Query("SELECT * FROM " + TABLE_DEVICE)
    List<Device> getAll();
}
