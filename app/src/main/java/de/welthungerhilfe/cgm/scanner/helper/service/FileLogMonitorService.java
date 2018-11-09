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
import com.google.android.gms.tasks.OnCompleteListener;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;

public class FileLogMonitorService extends Service {
    private List<String> pendingArtefacts;
    private Object lock = new Object();

    private Timer timer = new Timer();
    private ExecutorService executor;

    public void onCreate() {
        pendingArtefacts = new ArrayList<>();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkFileLogDatabase();
            }
        }, 0, AppConstants.LOG_MONITOR_INTERVAL);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        if (executor != null) {
            executor.shutdownNow();
            pendingArtefacts = new ArrayList<>();
        }
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
                        executor = Executors.newFixedThreadPool(MULTI_UPLOAD_BUNCH);
                        //executor = Executors.newSingleThreadExecutor();

                        for (FileLog log : logs) {
                            Runnable worker = new UploadThread(log);
                            executor.execute(worker);
                        }
                        //executor.shutdown();
                    } else {
                        pendingArtefacts.clear();
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
            synchronized (lock) {
                if (pendingArtefacts.size() > MULTI_UPLOAD_BUNCH) {
                    try {
                        lock.wait();
                        Log.e("pending", "waiting now");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                FileInputStream fis = new FileInputStream(log.getPath());

                pendingArtefacts.add(log.getId());
                Log.e("pending added", log.getId());

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

                if (path.contains("{qrcode}") || path.contains("scantimestamp")) {
                    Log.e("MonitorService : ", String.format("id: %s, qrcode: %s, scantimestamp: %s", log.getId(), log.getQrCode(), String.valueOf(log.getCreateDate())));
                }

                String[] arr = log.getPath().split("/");
                StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(path).child(arr[arr.length - 1]);
                photoRef.putStream(fis)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                StorageMetadata metadata = taskSnapshot.getMetadata();
                                if (metadata.getMd5Hash().trim().compareTo(log.getHashValue().trim()) == 0) {
                                    log.setUploadDate(Utils.getUniversalTimestamp());
                                    File file = new File(log.getPath());
                                    if (file.exists() && !log.getType().equals("consent")) {
                                        file.delete();
                                        log.setDeleted(true);
                                    }
                                    log.setPath(photoRef.getPath());
                                    new OfflineTask().saveFileLog(log);
                                    AppController.getInstance().firebaseFirestore.collection("artefacts")
                                            .document(log.getId())
                                            .set(log);
                                }

                                synchronized (lock) {
                                    Log.e("pending removed", log.getId());
                                    pendingArtefacts.remove(log.getId());
                                    lock.notify();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                try {
                                    fis.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }

                                e.printStackTrace();
                                synchronized (lock) {
                                    Log.e("pending removed", log.getId());
                                    pendingArtefacts.remove(log.getId());
                                    lock.notify();
                                }
                            }
                        });
            } catch (FileNotFoundException e) {
                log.setUploadDate(Utils.getUniversalTimestamp());
                log.setDeleted(true);
                new OfflineTask().saveFileLog(log);

                synchronized (lock) {
                    Log.e("pending removed", log.getId());
                    pendingArtefacts.remove(log.getId());
                    lock.notify();
                }
            }
        }
    }
}
