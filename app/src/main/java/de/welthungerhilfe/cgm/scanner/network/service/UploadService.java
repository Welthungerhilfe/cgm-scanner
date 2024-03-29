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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.jcodec.common.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.network.NetworkUtils;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.syncdata.MeasureNotification;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;

import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
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

    public static final int FN_ID_UPLOAD_SERVICE = 100;

    private int onErrorCount = 0;

    private Retrofit retrofit;

    private SessionManager sessionManager;

    public static void forceResume() {
        new Thread(() -> {
            if (service != null) {
                synchronized (service.lock) {
                    if (!running) {
                        service.loadQueueFileLogs();
                    }
                }
            }
        }).start();
    }

    public static void resetRetrofit() {
        if (service != null) {
            service.retrofit = null;
        }
    }

    public static boolean isInitialized() {
        return service != null;
    }

    public void onCreate() {
        service = this;
        running = false;
        onErrorCount = 0;
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
                    startForeground(FN_ID_UPLOAD_SERVICE, MeasureNotification.createForegroundNotification(getApplicationContext(), getApplicationContext().getString(R.string.app_name), getApplicationContext().getString(R.string.scan_is_uploading)));
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
        LogFileUtils.logInfo(TAG, "Stopped");
        service = null;
        running = false;
        onErrorCount = 0;

        if (executor != null) {
            executor.shutdownNow();
            pendingArtefacts = new ArrayList<>();
        }
    }

    private void loadQueueFileLogs() {
        if (!NetworkUtils.isUploadAllowed(this)) {
            LogFileUtils.logInfo(TAG, "Skipped due to not available network");
            stopSelf();
            return;
        }

        if (!sessionManager.isSigned()) {
            stopSelf();
            return;
        }
        LogFileUtils.logInfo(TAG, "Started");
        running = true;

        repository.loadQueuedData(this, sessionManager.getEnvironment());
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        synchronized (lock) {
            remainingCount = list.size();
        }
        LogFileUtils.logInfo(TAG, String.format(Locale.US, "%d artifacts are in queue now", remainingCount));

        Context c = getApplicationContext();
        if (LocalPersistency.getBoolean(c, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
            String measureId = LocalPersistency.getString(c, SettingsPerformanceActivity.KEY_TEST_RESULT_ID);
            boolean finished = true;
            for (FileLog log : list) {
                if (log.getMeasureId() != null) {
                    if (measureId.compareTo(log.getMeasureId()) == 0) {
                        finished = false;
                        break;
                    }
                }
            }
            if (finished) {
                onUploadFinished();
            }
        }

        if (remainingCount <= 0) {
            if (updated) {
                updated = false;
                SyncingWorkManager.startSyncingWithWorkManager(getApplicationContext());

            }
            stopSelf();
        } else {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Runnable worker = new UploadThread(list.get(i));
                    executor.execute(worker);
                } catch (Exception e) {
                    LogFileUtils.logException(e,"onFileLogsLoaded");
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
                if (log.getMeasureId() != null) {
                    if (measureId.compareTo(log.getMeasureId()) == 0) {
                        if (LocalPersistency.getLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START) == 0) {
                            LocalPersistency.setLong(c, SettingsPerformanceActivity.KEY_TEST_RESULT_START, System.currentTimeMillis());
                        }
                    }
                }
            }

            synchronized (lock) {
                if (pendingArtefacts.size() >= MULTI_UPLOAD_BUNCH) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        LogFileUtils.logException(e, "uploadservice uploadthread");
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
                case "consent":
                case "rgb":
                    mime = "image/jpeg";
                    break;
                default:
                    LogFileUtils.logError(TAG, "Data type not supported");
            }

            AppController.sleep(1000);
            while (!NetworkUtils.isUploadAllowed(getApplicationContext())) {
                AppController.sleep(3000);
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
        LogFileUtils.logInfo(TAG, "Uploading file " + file.getPath());

        try {
            FileInputStream inputStream = new FileInputStream(file);
            body = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(
                    MediaType.parse(mime), IOUtils.toByteArray(inputStream)));
            inputStream.close();
            log.setCreateDate(AppController.getInstance().getUniversalTimestamp());
        } catch (FileNotFoundException e) {
            LogFileUtils.logException(e,"uploadservice filenotfound");

            log.setDeleted(true);
            log.setStatus(FILE_NOT_FOUND);
            updateFileLog(log);

        } catch (Exception e) {
            LogFileUtils.logException(e,"uploadservice execption");

            log.setStatus(UPLOAD_ERROR);
            updateFileLog(log);
        }
        RequestBody filename = RequestBody.create(MediaType.parse("multipart/form-data"), file.getName());
        if (retrofit == null) {
            retrofit = SyncingWorkManager.provideRetrofit();
        }
        if(log.isDeleted()){
            LogFileUtils.logInfo(TAG, "Uploading file restapi not calling because deleted " + file.getPath());
            return;
        }
        retrofit.create(ApiService.class).uploadFiles(sessionManager.getAuthTokenWithBearer(), body, filename).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String id) {
                        LogFileUtils.logInfo(TAG, "File " + file.getPath() + " successfully uploaded with server id "+id);
                        log.setUploadDate(AppController.getInstance().getUniversalTimestamp());
                        log.setServerId(id);
                        if(log.getServerId()!=null && !log.getServerId().isEmpty()) {
                            if (file.delete()) {
                                log.setDeleted(true);
                                log.setStatus(UPLOADED_DELETED);
                            } else {
                                log.setStatus(UPLOADED);
                            }

                            updateFileLog(log);
                        }
                        resetOnErrorCount();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogFileUtils.logError(TAG, "File " + file.getPath() + " upload fail - " + e.getMessage());
                        if (NetworkUtils.isExpiredToken(e.getMessage())) {
                            AuthenticationHandler.restoreToken(getApplicationContext());
                            error401();
                            stopSelf();
                        } else {
                            updateFileLog(log);
                        }
                        increaseOnErrorCount();
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
                synchronized (lock) {
                    pendingArtefacts.remove(log.getId());
                    remainingCount--;
                    if (remainingCount <= 0) {
                        loadQueueFileLogs();
                    }
                    lock.notify();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void increaseOnErrorCount() {
        if (++onErrorCount >= 3) {
            stopSelf();
        }
    }

    private void resetOnErrorCount() {
        onErrorCount = 0;
    }

    public void error401(){
        int count = sessionManager.getSessionError()+1;
        sessionManager.setSessionError(count);
        LogFileUtils.logInfo(TAG,"error 401 "+count);
    }
}
