package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.DIFF_HASH;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.FILE_NOT_FOUND;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED_NOT_DELETED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOAD_ERROR;

public class UploadService extends Service implements OnFileLogsLoad {
    private List<String> pendingArtefacts;
    private static int remainingCount = 0;

    private FileLogRepository repository;


    private Object lock = new Object();
    private ExecutorService executor;

    public void onCreate() {
        repository = FileLogRepository.getInstance(getApplicationContext());

        pendingArtefacts = new ArrayList<>();

        executor = Executors.newFixedThreadPool(MULTI_UPLOAD_BUNCH);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("UploadService", "Start new uploads");

        if (remainingCount == 0) {
            loadQueueFileLogs();
        }

        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.e("UploadService", "Started");

        if (remainingCount <= 0) {
            loadQueueFileLogs();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("UploadService", "Stopped");

        if (executor != null) {
            executor.shutdownNow();
            pendingArtefacts = new ArrayList<>();
        }
    }

    private void loadQueueFileLogs() {
        repository.loadQueuedData(this);
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        remainingCount = list.size();
        Log.e("UploadService", String.format(Locale.US, "%d artifacts are in queue now", remainingCount));

        if (remainingCount == 0) {
            stopSelf();
        } else {
            for (int i = 0; i < list.size(); i++) {
                Runnable worker = new UploadThread(list.get(i));
                executor.execute(worker);
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
            synchronized (lock) {
                if (pendingArtefacts.size() > MULTI_UPLOAD_BUNCH) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            Log.e("UploadService", String.format("Upload Started : %s", log.getId()));

            pendingArtefacts.add(log.getId());

            String path = "";
            switch (log.getType()) {
                case "pcd":
                    path = AppConstants.STORAGE_PC_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
                case "rgb":
                    path = AppConstants.STORAGE_RGB_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
                case "consent":
                    path = AppConstants.STORAGE_CONSENT_URL.replace("{qrcode}", log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                    break;
            }

            String[] arr = log.getPath().split("/");
            StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(path).child(arr[arr.length - 1]);
            photoRef.putFile(Uri.fromFile(new File(log.getPath())))
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                log.setPath(photoRef.getPath());
                                log.setUploadDate(Utils.getUniversalTimestamp());

                                StorageMetadata metadata = task.getResult().getMetadata();
                                if (metadata.getMd5Hash().trim().equals(log.getHashValue().trim())) {
                                    log.setStatus(UPLOADED);

                                    try {
                                        new File(log.getPath()).delete();
                                        log.setDeleted(true);
                                        log.setStatus(UPLOADED_NOT_DELETED);
                                    } catch (Exception e) {
                                        log.setStatus(FILE_NOT_FOUND);
                                    }
                                } else {
                                    log.setStatus(DIFF_HASH);
                                }

                                AppController.getInstance().firebaseFirestore.collection("artefacts")
                                        .document(log.getId())
                                        .set(log);
                            } else {
                                log.setStatus(UPLOAD_ERROR);
                            }

                            repository.updateFileLog(log);

                            synchronized (lock) {
                                Log.e("UploadService", String.format("Upload Completed : %s", log.getId()));

                                pendingArtefacts.remove(log.getId());
                                remainingCount --;

                                Log.e("UploadService", String.format("Remaining Count : %d", remainingCount));

                                if (remainingCount <= 0) {
                                    loadQueueFileLogs();
                                }

                                lock.notify();
                            }
                        }
                    });
        }
    }
}
