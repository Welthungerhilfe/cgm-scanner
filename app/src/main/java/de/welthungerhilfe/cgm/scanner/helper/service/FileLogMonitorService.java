package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class FileLogMonitorService extends Service {
    private List<String> pendingArtefacts;

    private Timer timer = new Timer();

    public void onCreate() {
        pendingArtefacts = new ArrayList<>();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.e("Monitor", "service started");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.e("Monitor", "monitor called");
                checkFileLogDatabase();
            }
        }, 0, AppConstants.LOG_MONITOR_INTERVAL);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkFileLogDatabase() {
        new OfflineTask().getSyncableFileLog(new OfflineTask.OnLoadFileLogs() {
            @Override
            public void onLoadFileLogs(List<FileLog> logs) {
                if (Utils.isNetworkConnectionAvailable(FileLogMonitorService.this)) {
                    if (logs.size() > 0) {
                        Log.e("File Monitor", "Do upload");

                        ExecutorService executor = Executors.newFixedThreadPool(5);

                        for (FileLog log : logs) {
                            Runnable worker = new UploadThread(log);
                            executor.execute(worker);
                        }
                        executor.shutdown();
                    }
                }
            }
        });
    }

    private class UploadThread implements Runnable {
        FileLog log;

        UploadThread (FileLog log) {
            this.log = log;
        }

        @Override
        public void run() {
            if (pendingArtefacts.contains(log.getId())) {
                return;
            }
            pendingArtefacts.add(log.getId());

            File file = new File(log.getPath());
            if (!file.exists()) {
                log.setUploadDate(Utils.getUniversalTimestamp());
                log.setDeleted(true);
                new OfflineTask().saveFileLog(log);
            } else {
                Uri fileUri = Uri.fromFile(file);

                String path = "";
                if (log.getType().equals("pcd"))
                    path = AppConstants.STORAGE_PC_URL.replace("{qrcode}",  log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                else if (log.getType().equals("rgb"))
                    path = AppConstants.STORAGE_RGB_URL.replace("{qrcode}",  log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));
                else if (log.getType().equals("consent"))
                    path = AppConstants.STORAGE_CONSENT_URL.replace("{qrcode}",  log.getQrCode()).replace("{scantimestamp}", String.valueOf(log.getCreateDate()));

                StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(path)
                        .child(fileUri.getLastPathSegment());

                photoRef.putFile(fileUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                StorageMetadata metadata = taskSnapshot.getMetadata();
                                if (metadata.getMd5Hash().trim().compareTo(log.getHashValue().trim()) == 0) {
                                    Log.e("upload", "succeed");
                                    log.setUploadDate(Utils.getUniversalTimestamp());
                                    File file = new File(log.getPath());
                                    if (file.exists()) {
                                        file.delete();
                                        log.setDeleted(true);
                                    }
                                    Log.e("Artefact Id", log.getId());
                                    new OfflineTask().saveFileLog(log);
                                } else {
                                    Log.e("upload", "hash different");
                                    Log.e("server hash", metadata.getMd5Hash());
                                    Log.e("local hash", log.getHashValue());
                                }
                                pendingArtefacts.remove(log.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("upload", "failed");
                                e.printStackTrace();
                                pendingArtefacts.remove(log.getId());
                            }
                        });
            }
        }
    }
}
