package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class MemoryMonitorService extends Service {
    private Timer timer = new Timer();

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            battery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        }
    };

    private double cpu = 0;
    private double memory = 0;
    private long battery = 100;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Get memory state
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.getMemoryInfo(mi);

                    //double availableMegs = mi.availMem / 0x100000L;
                    //double percentAvail = mi.availMem / (double)mi.totalMem * 100.0;
                    memory = ((double)(mi.totalMem - mi.availMem) / mi.totalMem) * 100;
                    Log.e("memory usage:", String.format("%f%% of memory is in usage", memory));
                    Log.e("memory usage:", String.format("available: %d, total: %d", mi.availMem, mi.totalMem));

                    // Get CPU state
                    cpu = Utils.readUsage();
                    Log.e("cpu state:", String.format("cpu usage %f%%", cpu));
                    Log.e("battery usage:", String.format("%d%% of battery is left", battery));
                }
            }
        }, 0, AppConstants.MEMORY_MONITOR_INTERVAL);


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
