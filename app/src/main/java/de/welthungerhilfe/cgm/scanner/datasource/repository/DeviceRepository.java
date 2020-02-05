package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnDevicesLoad;

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

    @SuppressLint("StaticFieldLeak")
    public void getSyncablePerson(OnDevicesLoad listener, long timestamp) {
        new AsyncTask<Void, Void, List<Device>>() {
            @Override
            protected List<Device> doInBackground(Void... voids) {
                return database.deviceDao().getSyncableDevice(timestamp);
            }

            public void onPostExecute(List<Device> devices) {
                listener.onDevicesLoaded(devices);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void updateDevice(Device device) {
        executor.execute(() -> database.deviceDao().updateDevice(device));
    }

    public List<Device> getAll() {
        return database.deviceDao().getAll();
    }
}
