/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
