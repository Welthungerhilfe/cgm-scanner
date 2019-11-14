package de.welthungerhilfe.cgm.scanner.helper.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.HEALTH_INTERVAL;

public class DeviceService extends Service {
    private Timer timer = new Timer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (AppController.getInstance().firebaseUser == null)
                    return;

                Device device = new Device();
                device.setId(AppController.getInstance().getDeviceId());
                device.setUuid(Utils.getAndroidID(getContentResolver()));
                device.setCreate_timestamp(Utils.getUniversalTimestamp());
                device.setSync_timestamp(Utils.getUniversalTimestamp());
                device.setCreated_by(AppController.getInstance().firebaseUser.getEmail());
                device.setSchema_version(CgmDatabase.version);

                try {
                    device.setApp_version(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    device.setApp_version("1.0.0");
                }

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        PersonRepository personRepo = PersonRepository.getInstance(getBaseContext());
                        MeasureRepository measureRepo = MeasureRepository.getInstance(getBaseContext());
                        FileLogRepository fileLogRepo = FileLogRepository.getInstance(getBaseContext());

                        device.setNew_artifacts(fileLogRepo.getArtifactCount());
                        device.setNew_artifact_file_size_mb(fileLogRepo.getArtifactFileSize());

                        device.setDeleted_artifacts(fileLogRepo.getDeletedArtifactCount());

                        device.setTotal_artifacts(fileLogRepo.getTotalArtifactCount());
                        device.setTotal_artifact_file_size_mb(fileLogRepo.getTotalArtifactFileSize());

                        device.setOwn_persons(personRepo.getOwnPersonCount());
                        device.setOwn_measures(measureRepo.getOwnMeasureCount());

                        device.setTotal_persons(personRepo.getTotalPersonCount());
                        device.setTotal_measures(measureRepo.getTotalMeasureCount());

                        DeviceRepository.getInstance(getBaseContext()).insertDevice(device);

                        return null;
                    }
                }.execute();
            }
        }, 0, HEALTH_INTERVAL);

        return START_STICKY;
    }
}