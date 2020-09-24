package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.FILE_NOT_FOUND;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED_DELETED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOAD_ERROR;

public class UploadService extends Service implements OnFileLogsLoad {
    private List<String> pendingArtefacts;
    private int remainingCount = 0;

    private static boolean running = false;
    private static UploadService service = null;

    private FileLogRepository repository;

    public CloudBlobClient blobClient;

    private final Object lock = new Object();
    private ExecutorService executor;

    public static void forceResume() {
        if (service != null) {
            synchronized (service.lock) {
                if (!running) {
                    service.loadQueueFileLogs();
                }
            }
        }
    }

    public void onCreate() {
        service = this;

        repository = FileLogRepository.getInstance(getApplicationContext());

        pendingArtefacts = new ArrayList<>();

        executor = Executors.newFixedThreadPool(MULTI_UPLOAD_BUNCH);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (remainingCount <= 0) {
            try {
                synchronized (SyncAdapter.getLock()) {
                    CloudStorageAccount storageAccount = CloudStorageAccount.parse(AppController.getInstance().getAzureConnection());
                    blobClient = storageAccount.createCloudBlobClient();
                }

                loadQueueFileLogs();

                return START_STICKY;
            } catch (URISyntaxException | InvalidKeyException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("UploadService", "Stopped");
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            pendingArtefacts = new ArrayList<>();
        }
    }

    private void loadQueueFileLogs() {
        Log.e("UploadService", "Started");
        running = true;

        repository.loadQueuedData(this);
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        synchronized (lock) {
            remainingCount = list.size();
        }
        Log.e("UploadService", String.format(Locale.US, "%d artifacts are in queue now", remainingCount));

        Context c = getApplicationContext();
        if (LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
            String measureId = LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID);
            boolean finished = true;
            for (FileLog log : list) {
                if (measureId.compareTo(log.getMeasureId()) == 0) {
                    finished = false;
                    break;
                }
            }
            if (finished) {
                onUploadFinished();
            }
        }

        if (remainingCount <= 0) {
            stopSelf();
        } else {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Runnable worker = new UploadThread(list.get(i));
                    executor.execute(worker);
                } catch (Exception ex) {
                    remainingCount --;
                }
            }
        }
    }

    private class UploadThread implements Runnable {
        private FileLog log;

        UploadThread (FileLog log) {
            this.log = log;
        }

        @Override
        public void run() {
            Context c = getApplicationContext();
            if (LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
                String measureId = LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID);
                if (measureId.compareTo(log.getMeasureId()) == 0) {
                    if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START) == 0) {
                        LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START, System.currentTimeMillis());
                    }
                }
            }

            synchronized (lock) {
                if (pendingArtefacts.size() >= MULTI_UPLOAD_BUNCH) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            pendingArtefacts.add(log.getId());

            String path = "";
            switch (log.getType()) {
                case "calibration":
                    path = AppConstants.STORAGE_CALIBRATION_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
                case "depth":
                    path = AppConstants.STORAGE_DEPTH_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
                case "rgb":
                    path = AppConstants.STORAGE_RGB_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
                case "consent":
                    path = AppConstants.STORAGE_CONSENT_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
            }

            boolean wifiOnly = LocalPersistency.getBoolean(getBaseContext(), SettingsActivity.KEY_UPLOAD_WIFI);
            while (wifiOnly && !Utils.isWifiConnected(getBaseContext())) {
                Utils.sleep(3000);
            }

            String[] arr = log.getPath().split("/");

            try {
                final File file = new File(log.getPath());
                FileInputStream stream = new FileInputStream(file);

                CloudBlobContainer container = blobClient.getContainerReference(AppConstants.STORAGE_CONTAINER);
                container.createIfNotExists();

                CloudBlockBlob blob = container.getBlockBlobReference(path + arr[arr.length - 1]);
                blob.upload(stream, stream.available());

                log.setUploadDate(Utils.getUniversalTimestamp());

                if (file.delete()) {
                    log.setDeleted(true);
                    log.setStatus(UPLOADED_DELETED);
                } else {
                    log.setStatus(UPLOADED);
                }
                stream.close();
            } catch (FileNotFoundException e) {
                log.setDeleted(true);
                log.setStatus(FILE_NOT_FOUND);
            } catch (Exception e) {
                log.setStatus(UPLOAD_ERROR);
            }
            log.setCreateDate(Utils.getUniversalTimestamp());

            repository.updateFileLog(log);

            synchronized (lock) {
                pendingArtefacts.remove(log.getId());
                remainingCount--;
                Log.e("UploadService", String.format(Locale.US, "%d artifacts are in queue now", remainingCount));
                if (remainingCount <= 0) {
                    loadQueueFileLogs();
                }
                lock.notify();
            }
        }
    }

    private void onUploadFinished() {

        //do not continue if the previous timestamps are missing
        Context c = getApplicationContext();
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_SCAN) == 0) {
            return;
        }
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START) == 0) {
            return;
        }

        //set timestamp for the end of upload
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_END) == 0) {
            LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_END, System.currentTimeMillis());
        }
    }
}
