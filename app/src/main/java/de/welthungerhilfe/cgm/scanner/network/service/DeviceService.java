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
package de.welthungerhilfe.cgm.scanner.network.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

import static de.welthungerhilfe.cgm.scanner.AppConstants.HEALTH_INTERVAL;

public class DeviceService extends Service {
    private final Timer timer = new Timer();
    private SessionManager session;
    private static final String TAG = DeviceService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {


        session = new SessionManager(getBaseContext());
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (session.isSigned())
                    return;

                Device device = new Device();

                device.setId(AppController.getInstance().get_deviceId());

                device.setUuid(AppController.getInstance().getAndroidID());
                device.setCreate_timestamp(AppController.getInstance().getUniversalTimestamp());
                device.setSync_timestamp(AppController.getInstance().getUniversalTimestamp());
                device.setCreated_by(session.getUserEmail());
                device.setSchema_version(CgmDatabase.version);
                device.setIssues(LocalPersistency.getString(getApplicationContext(), DeviceCheckActivity.KEY_LAST_DEVICE_CHECK_ISSUES));

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"this is inside onDestroy");
    }
}
