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
}
