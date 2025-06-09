package de.welthungerhilfe.cgm.scanner;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;

public class UploadTriggerWorker extends Worker {
    private static final String TAG = "UploadTriggerWorker";

    public UploadTriggerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            LogFileUtils.logInfo(TAG, "Triggering UploadService");
            Context context = getApplicationContext();
            if (!UploadService.isInitialized()) {
                context.startService(new Intent(context, UploadService.class));
            } else {
                UploadService.forceResume();
            }
            return Result.success();
        } catch (Exception e) {
            LogFileUtils.logException(e, "Failed to trigger UploadService: " + e.getMessage());
            return Result.failure();
        }
    }
}
