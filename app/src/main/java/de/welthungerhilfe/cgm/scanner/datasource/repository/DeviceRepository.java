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
package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;

public class DeviceRepository {

    private static DeviceRepository instance;

    private CgmDatabase database;

    private DeviceRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static DeviceRepository getInstance(Context context) {
        if(instance == null) {
            instance = new DeviceRepository(context);
        }
        return instance;
    }

    public void insertDevice(Device device) {
        database.deviceDao().insertDevice(device);
    }

    public List<Device> getSyncableDevice(long timestamp) {
        return database.deviceDao().getSyncableDevice(timestamp);
    }

    public void updateDevice(Device device) {
        database.deviceDao().updateDevice(device);
    }

    public List<Device> getAll() {
        return database.deviceDao().getAll();
    }
}
