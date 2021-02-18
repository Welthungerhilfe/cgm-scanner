package de.welthungerhilfe.cgm.scanner.workmanager;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;


public class WorkManagerUtils {

    public static void startSyncingWithWorkManager(Context context) {

        PeriodicWorkRequest SyncingWorkManager =
                new PeriodicWorkRequest.Builder(SyncingWorkManager.class, 16, TimeUnit.MINUTES).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SyncingWorkManager",
                ExistingPeriodicWorkPolicy.REPLACE,
                SyncingWorkManager);
    }
}
