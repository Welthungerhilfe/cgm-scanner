package de.welthungerhilfe.cgm.scanner.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.service.FileLogMonitorService;
import de.welthungerhilfe.cgm.scanner.helper.service.FirebaseUploadService;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;

public class FastUploadActivity extends AppCompatActivity {
    @BindView(R.id.rytLoading)
    RelativeLayout rytLoading;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txtStatus)
    TextView txtStatus;

    @OnClick(R.id.btnStart)
    void onStartClicked(Button btnStart) {
        rytLoading.setVisibility(View.VISIBLE);

        for (int i = 0; i < MULTI_UPLOAD_BUNCH + 5; i++) {
            uploadArtefacts();
        }
    }

    @OnClick(R.id.btnStop)
    void onStopClicked(Button btnStop) {
        rytLoading.setVisibility(View.GONE);
    }

    private ExecutorService executor;
    private List<String> pendingArtefacts;
    private List<String> filePaths = new ArrayList<>();
    private double totalFileSize = 0;
    private double uploadedFileSize = 0;
    private int index = 0;
    private long succeed = 0;
    private long failed = 0;

    private Object lock = new Object();


    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        setContentView(R.layout.activity_fast_upload);
        ButterKnife.bind(this);

        setupToolbar();
        stopRunningUploadService();

        prepareFastUpload();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File root = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
                loadFiles(root);

                txtStatus.setText(String.valueOf(filePaths.size()) + "files are remaining." + String.valueOf(totalFileSize / 1024 / 1024) + "MB remaining");
            }
        });
    }

    public void onDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }

        startService(new Intent(getApplicationContext(), FileLogMonitorService.class));

        super.onDestroy();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_fast_uploading);
    }

    private void stopRunningUploadService() {
        getApplicationContext().stopService(new Intent(getApplicationContext(), FileLogMonitorService.class));
    }

    private void loadFiles(File target) throws NullPointerException, OutOfMemoryError {
        if (target.isDirectory()) {
            File[] children = target.listFiles();

            if (children.length == 0) {
                target.delete();
            } else {
                for (int i = 0; i < children.length; i++) {
                    loadFiles(children[i]);
                }
            }
        } else {
            filePaths.add(target.getPath());
            totalFileSize += target.length();
        }
    }

    private void prepareFastUpload() {
        pendingArtefacts = new ArrayList<>();

        executor = Executors.newFixedThreadPool(MULTI_UPLOAD_BUNCH);
    }

    private void uploadArtefacts() {
        if (index >= filePaths.size()) {
            rytLoading.setVisibility(View.GONE);
        } else {
            index ++;

            new OfflineTask().getFileLog(filePaths.get(index - 1), new OfflineTask.OnLoadFileLog() {
                @Override
                public void onLoadFileLog(FileLog log) {
                    if (log != null)
                        executor.execute(new UploadThread(log));
                }
            });
        }
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
                    Log.e("FastUpload : ", String.format("id: %s, qrcode: %s, scantimestamp: %s", log.getId(), log.getQrCode(), String.valueOf(log.getCreateDate())));
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
                                } else {

                                }

                                succeed ++;

                                synchronized (lock) {
                                    Log.e("finished", log.getId());
                                    pendingArtefacts.remove(log.getId());
                                    lock.notify();

                                    uploadArtefacts();
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

                                failed ++;

                                e.printStackTrace();
                                synchronized (lock) {
                                    Log.e("failed", log.getId());
                                    pendingArtefacts.remove(log.getId());
                                    lock.notify();

                                    uploadArtefacts();
                                }
                            }
                        });
            } catch (FileNotFoundException e) {
                log.setUploadDate(Utils.getUniversalTimestamp());
                log.setDeleted(true);
                new OfflineTask().saveFileLog(log);

                failed ++;

                synchronized (lock) {
                    Log.e("file not found", log.getId());
                    pendingArtefacts.remove(log.getId());
                    lock.notify();

                    uploadArtefacts();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
