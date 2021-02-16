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

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import org.jcodec.common.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AccountUtils;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.utils.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;

import static de.welthungerhilfe.cgm.scanner.AppConstants.FILE_NOT_FOUND;
import static de.welthungerhilfe.cgm.scanner.AppConstants.MULTI_UPLOAD_BUNCH;
import static de.welthungerhilfe.cgm.scanner.AppConstants.UPLOADED;
import static de.welthungerhilfe.cgm.scanner.AppConstants.UPLOADED_DELETED;
import static de.welthungerhilfe.cgm.scanner.AppConstants.UPLOAD_ERROR;

public class UploadService extends Service implements FileLogRepository.OnFileLogsLoad {

    private static final String TAG = UploadService.class.getSimpleName();

    private List<String> pendingArtefacts;
    private int remainingCount = 0;
    private boolean updated = false;

    private static boolean running = false;
    private static UploadService service = null;

    private FileLogRepository repository;

    private final Object lock = new Object();
    private ExecutorService executor;

    public static final int FOREGROUND_NOTIFICATION_ID = 100;


    @Inject
    Retrofit retrofit;

    SessionManager sessionManager;

    public static void forceResume() {
        if (service != null) {
            synchronized (service.lock) {
                if (!running) {
                    service.loadQueueFileLogs();
                }
            }
        }
    }

    public static boolean isInitialized() {
        return service != null;
    }

    public void onCreate() {
        service = this;
        running = false;

        AndroidInjection.inject(this);
        repository = FileLogRepository.getInstance(getApplicationContext());
        sessionManager = new SessionManager(getApplication());

        pendingArtefacts = new ArrayList<>();

        executor = Executors.newFixedThreadPool(MULTI_UPLOAD_BUNCH);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (intent != null && intent.hasExtra(AppConstants.IS_FOREGROUND)) {
                if (intent.getBooleanExtra(AppConstants.IS_FOREGROUND, false)) {
                    String NOTIFICATION_CHANNEL_ID = "de.welthungerhilfe.cgm.scanner";
                    String channelName = "UploadService";
                    NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                    chan.setLightColor(Color.BLUE);
                    chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    assert manager != null;
                    manager.createNotificationChannel(chan);

                    Intent notificationIntent = new Intent(this, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this,
                            0, notificationIntent, 0);
                    Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(getApplicationContext().getString(R.string.app_name))
                            .setContentText(getApplicationContext().getString(R.string.scan_is_uploading))
                            .setSmallIcon(R.drawable.icon_notif)
                            .setContentIntent(pendingIntent)
                            .build();
                    startForeground(FOREGROUND_NOTIFICATION_ID, notification);
                }
            }
        }

        if (remainingCount <= 0) {
            loadQueueFileLogs();
            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Stopped");
        service = null;
        running = false;

        if (executor != null) {
            executor.shutdownNow();
            pendingArtefacts = new ArrayList<>();
        }
    }

    private void loadQueueFileLogs() {
        if (!Utils.isUploadAllowed(this)) {
            Log.e(TAG, "Skipped");
            stopSelf();
            return;
        }

        if (!sessionManager.isSigned()) {
            stopSelf();
            return;
        }
        Log.e(TAG, "Started");
        running = true;

        repository.loadQueuedData(this, sessionManager);
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        synchronized (lock) {
            remainingCount = list.size();
        }
        Log.e(TAG, String.format(Locale.US, "%d artifacts are in queue now", remainingCount));
        Log.i("UploadService ", "this is inside onFileLoaded  " + remainingCount);

        Context c = getApplicationContext();
        if (LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
            String measureId = LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID);
            boolean finished = true;
            for (FileLog log : list) {
                if (measureId.compareTo(log.getMeasureId()) == 0) {
                    finished = false;
                    break;
                }
            }
            if (finished) {
                onUploadFinished();
            }
        }

        if (remainingCount <= 0) {
            if (updated) {
                updated = false;
                Account accountData = AccountUtils.getAccount(getApplicationContext(), sessionManager);
                SyncAdapter.startPeriodicSync(accountData, getApplicationContext());
            }
            stopSelf();
        } else {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Runnable worker = new UploadThread(list.get(i));
                    executor.execute(worker);
                } catch (Exception ex) {
                    remainingCount--;
                }
            }
        }
    }

    private class UploadThread implements Runnable {
        private FileLog log;

        UploadThread(FileLog log) {
            this.log = log;
        }

        @Override
        public void run() {
            Context c = getApplicationContext();
            if (LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
                String measureId = LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID);
                if (measureId.compareTo(log.getMeasureId()) == 0) {
                    if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START) == 0) {
                        LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START, System.currentTimeMillis());
                    }
                }
            }

            synchronized (lock) {
                if (pendingArtefacts.size() >= MULTI_UPLOAD_BUNCH) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            pendingArtefacts.add(log.getId());

            String mime = "";
            switch (log.getType()) {
                case "calibration":
                    mime = "text/plain";
                    break;
                case "depth":
                    mime = "application/zip";
                    break;
                case "rgb":
                    mime = "image/jpeg";
                    break;
                case "consent":
                    mime = "image/jpeg";
                    //mime = "image/png";
                    break;
                default:
                    Log.e(TAG, "Data type not supported");
            }

            Utils.sleep(1000);
            while (!Utils.isUploadAllowed(getBaseContext())) {
                Utils.sleep(3000);
            }

            uploadFile(log, mime);
        }
    }

    private void onUploadFinished() {

        //do not continue if the previous timestamps are missing
        Context c = getApplicationContext();
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_SCAN) == 0) {
            return;
        }
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START) == 0) {
            return;
        }

        //set timestamp for the end of upload
        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_END) == 0) {
            LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_END, System.currentTimeMillis());
        }
    }

    public void uploadFile(FileLog log, String mime) {
        if (!sessionManager.isSigned()) {
            stopSelf();
            return;
        }

        updated = true;
        MultipartBody.Part body = null;
        final File file = new File(log.getPath());
        Log.i(TAG, "this is file inside uploadFile " + file.getPath());

        try {
            FileInputStream inputStream = new FileInputStream(file);
            body = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(
                    MediaType.parse(mime), IOUtils.toByteArray(inputStream)));
            inputStream.close();
            log.setCreateDate(Utils.getUniversalTimestamp());
        } catch (FileNotFoundException e) {
            Log.i(TAG, "this is file inside FileNotFoundException " + e.getMessage());

            log.setDeleted(true);
            log.setStatus(FILE_NOT_FOUND);
            updateFileLog(log);

        } catch (Exception e) {
            Log.i(TAG, "this is file inside exception " + e.getMessage());

            log.setStatus(UPLOAD_ERROR);
            updateFileLog(log);
        }
        RequestBody filename = RequestBody.create(MediaType.parse("multipart/form-data"), file.getName());
        retrofit.create(ApiService.class).uploadFiles(sessionManager.getAuthTokenWithBearer(), body, filename).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String id) {
                        Log.i(TAG, "this is file inside onNext " + id + file.getPath());

                        log.setUploadDate(Utils.getUniversalTimestamp());
                        log.setServerId(id);

                        if (file.delete()) {
                            log.setDeleted(true);
                            log.setStatus(UPLOADED_DELETED);
                        } else {
                            log.setStatus(UPLOADED);
                        }

                        updateFileLog(log);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                        Log.i(TAG, "this is file inside onError" + e.getMessage() + file.getPath());
                        if (Utils.isExpiredToken(e.getMessage())) {
                            AuthenticationHandler.restoreToken(getBaseContext());
                            stopSelf();
                        } else {
                            updateFileLog(log);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void updateFileLog(FileLog log) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                repository.updateFileLog(log);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.i(TAG, "this is file saved " + log.getServerId() + log.getPath());

                synchronized (lock) {
                    pendingArtefacts.remove(log.getId());
                    remainingCount--;
                    Log.i(TAG, "this is artifacts in queue " + remainingCount);
                    Log.e(TAG, String.format(Locale.US, "%d artifacts are in queue now", remainingCount));
                    if (remainingCount <= 0) {
                        loadQueueFileLogs();
                    }
                    lock.notify();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
