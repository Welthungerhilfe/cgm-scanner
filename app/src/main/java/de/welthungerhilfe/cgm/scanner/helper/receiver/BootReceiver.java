package de.welthungerhilfe.cgm.scanner.helper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Boot", "BOOT completed, start service");
        context.startService(new Intent(context, UploadService.class));
    }
}
