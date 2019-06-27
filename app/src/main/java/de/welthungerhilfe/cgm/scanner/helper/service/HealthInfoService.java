package de.welthungerhilfe.cgm.scanner.helper.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.models.health.HealthInfo;
import de.welthungerhilfe.cgm.scanner.datasource.models.health.OwnData;
import de.welthungerhilfe.cgm.scanner.datasource.models.health.TotalData;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.HEALTH_INTERVAL;

public class HealthInfoService extends Service {
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
                HealthInfo info = new HealthInfo();
                info.setUuid(Utils.getAndroidID(getContentResolver()));
                info.setOwner(AppController.getInstance().firebaseUser.getEmail());
                info.setCreate_timestamp(System.currentTimeMillis());

                OwnData ownData = new OwnData();
                TotalData totalData = new TotalData();

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        PersonRepository personRepo = PersonRepository.getInstance(getBaseContext());
                        MeasureRepository measureRepo = MeasureRepository.getInstance(getBaseContext());
                        FileLogRepository fileLogRepo = FileLogRepository.getInstance(getBaseContext());

                        ownData.setOwn_persons(personRepo.getOwnPersonCount());
                        ownData.setOwn_measures(measureRepo.getOwnMeasureCount());
                        ownData.setArtifacts(fileLogRepo.getArtifactCount());
                        ownData.setDeleted_artifacts(fileLogRepo.getDeletedArtifactCount());
                        ownData.setTotal_artifacts(fileLogRepo.getTotalArtifactCount());
                        ownData.setArtifact_file_size_mb(fileLogRepo.getArtifactFileSize());
                        ownData.setTotal_artifact_file_size_mb(fileLogRepo.getTotalArtifactFileSize());

                        totalData.setTotal_persons(personRepo.getTotalPersonCount());
                        totalData.setTotal_measures(measureRepo.getTotalMeasureCount());

                        info.setOwn_data(ownData);
                        info.setTotal_data(totalData);

                        Gson gson = new Gson();
                        String healthData = gson.toJson(info);

                        try {
                            CloudStorageAccount storageAccount = CloudStorageAccount.parse(AppController.getInstance().getAzureConnection());
                            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

                            CloudQueue queue = queueClient.getQueueReference("device");
                            queue.createIfNotExists();

                            CloudQueueMessage message = new CloudQueueMessage(healthData);
                            queue.addMessage(message);
                        } catch (StorageException | InvalidKeyException | URISyntaxException e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                }.execute();
            }
        }, 0, HEALTH_INTERVAL);

        return START_STICKY;
    }
}
