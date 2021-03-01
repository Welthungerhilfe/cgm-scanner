package de.welthungerhilfe.cgm.scanner.network.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.network.syncdata.MeasureNotification;
import de.welthungerhilfe.cgm.scanner.network.syncdata.WifiStateChangeReceiver;

public class WifiStateChangereceiverHelperService extends Service {

    WifiStateChangeReceiver wifiStateChangeReceiver;
    public static boolean isServiceRunning = false;
    public static final int FN_ID_WIFI_STATE_CHANGE_SERVICE = 101;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FN_ID_WIFI_STATE_CHANGE_SERVICE, MeasureNotification.createForegroundNotification(getApplicationContext(), getApplicationContext().getString(R.string.app_name), getApplicationContext().getString(R.string.wifi_state_change_watcher)));
            wifiStateChangeReceiver = new WifiStateChangeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.STATE_CHANGE");
            registerReceiver(wifiStateChangeReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiStateChangeReceiver);
        isServiceRunning = false;
    }
}
