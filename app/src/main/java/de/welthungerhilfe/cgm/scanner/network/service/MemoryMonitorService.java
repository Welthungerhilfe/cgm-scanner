/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class MemoryMonitorService extends Service {
    private final Timer timer = new Timer();

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
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
