package de.welthungerhilfe.cgm.scanner.network.syncdata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import de.welthungerhilfe.cgm.scanner.network.NetworkUtils;

public class WifiStateChangeReceiver extends BroadcastReceiver {

    public PendingResult result;
    public int recheckCounter;
    public static boolean isActivate = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isActivate) {
            recheckCounter = 5;
            isActivate = true;
            new NetworkAccess(context, result).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public class NetworkAccess extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<Context> contextRef;
        PendingResult pendingResult;

        public NetworkAccess(Context context, PendingResult pendingResult) {
            //WeakReference for avoiding memory leak.
            contextRef = new WeakReference<>(context);
            this.pendingResult = pendingResult;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return isInternetWorking(contextRef.get());
        }

        @Override
        protected void onPostExecute(Boolean res) {
            super.onPostExecute(res);
            Context context = contextRef.get();
            if (res && NetworkUtils.isWifiConnected(context)) {
                SyncingWorkManager.startSyncingWithWorkManager(context);
            }
            isActivate = false;
        }
    }

    public boolean isInternetWorking(Context context) {

        boolean success = false;
        try {
            Thread.sleep(5000);
            HttpURLConnection urlc = (HttpURLConnection) (new URL("https://www.google.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(10000);
            urlc.connect();
            return (urlc.getResponseCode() == 200);

        } catch (UnknownHostException e) {
            Log.i("wifistatechange", "this is inside catch " + e.getMessage());

            if (--recheckCounter > 0) {
                if (NetworkUtils.isWifiConnected(context))
                    isInternetWorking(context);
            }
        } catch (Exception e) {

        }
        return success;
    }
}
