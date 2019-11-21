package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;

public class DeviceRepository {

    private static DeviceRepository instance;

    private CgmDatabase database;

    private ExecutorService executor;

    private DeviceRepository(Context context) {
        database = CgmDatabase.getInstance(context);

        executor = Executors.newSingleThreadExecutor();
    }

    public static DeviceRepository getInstance(Context context) {
        if(instance == null) {
            instance = new DeviceRepository(context);
        }
        return instance;
    }

    public void insertDevice(Device device) {
        executor.execute(() -> database.deviceDao().insertDevice(device));
    }

    public List<Device> getSyncablePerson(long timestamp) {
        return database.deviceDao().getSyncableDevice(timestamp);
    }
}
