package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class FileLogMonitorService extends Service {
    private Timer timer = new Timer();

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
                    for (FileLog log : logs) {
                        /*
                        new Runnable() {
                            @Override
                            public void run() {
                                startService(new Intent(FileLogMonitorService.this, FirebaseUploadService.class)
                                        .putExtra(FirebaseUploadService.EXTRA_FILE_URI, Uri.fromFile(new File(log.getPath())))
                                        .putExtra(AppConstants.EXTRA_ARTEFACT, log)
                                        .putExtra(AppConstants.EXTRA_QR, log.getQrCode())
                                        .putExtra(AppConstants.EXTRA_SCANTIMESTAMP, String.valueOf(log.getCreateDate()))
                                        .putExtra(AppConstants.EXTRA_SCANARTEFACT_SUBFOLDER, AppConstants.STORAGE_RGB_URL)
                                        .setAction(FirebaseUploadService.ACTION_UPLOAD));
                            }
                        }.run();
                        */
                    }
                }
            }
        });
    }
}
