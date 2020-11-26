package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import org.jcodec.common.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.SuccessResponse;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.remote.ApiService;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsPerformanceActivity;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;
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

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.FILE_NOT_FOUND;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOADED_DELETED;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.UPLOAD_ERROR;

public class UploadService extends Service implements OnFileLogsLoad {

    private static final String TAG = UploadService.class.getSimpleName();

    private List<String> pendingArtefacts;
    private int remainingCount = 0;

    private static boolean running = false;
    private static UploadService service = null;

    private FileLogRepository repository;

    private final Object lock = new Object();
    private ExecutorService executor;

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
    public int onStartCommand(final Intent intent, int flags, int startId) {
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
            return;
        }

        Log.e(TAG, "Started");
        running = true;

        repository.loadQueuedData(this);
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        synchronized (lock) {
            remainingCount = list.size();
        }
        Log.e(TAG, String.format(Locale.US, "%d artifacts are in queue now", remainingCount));
        Log.i("UploadService ","this is inside onFileLoaded  "+remainingCount);

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
            stopSelf();
        } else {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Runnable worker = new UploadThread(list.get(i));
                    executor.execute(worker);
                } catch (Exception ex) {
                    remainingCount --;
                }
            }
        }
    }

    private class UploadThread implements Runnable {
        private FileLog log;

        UploadThread (FileLog log) {
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
                    mime = "image/png";
                    break;
                default:
                    Log.e(TAG, "Data type not supported");
            }

            Utils.sleep(3000);
            while (!Utils.isUploadAllowed(getBaseContext())) {
                Utils.sleep(3000);
            }

            try {
                uploadFiles(log, mime);
            } catch (FileNotFoundException e) {
                Log.i(TAG,"this is exception "+e.getMessage());

                log.setDeleted(true);
                log.setStatus(FILE_NOT_FOUND);
            } catch (Exception e) {
                Log.i(TAG,"this is exception "+e.getMessage());

                log.setStatus(UPLOAD_ERROR);
            }
            log.setCreateDate(Utils.getUniversalTimestamp());

            repository.updateFileLog(log);

            synchronized (lock) {
                pendingArtefacts.remove(log.getId());
                remainingCount--;
                Log.i(TAG,"this is artifacts in queue "+remainingCount);
                Log.e(TAG, String.format(Locale.US, "%d artifacts are in queue now", remainingCount));
                if (remainingCount <= 0) {
                    loadQueueFileLogs();
                }
                lock.notify();
            }
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

    public void uploadFiles(FileLog log, String mime) throws IOException {
        final File file = new File(log.getPath());
        FileInputStream inputStream = new FileInputStream(file);

        MultipartBody.Part body = null;
        try {
            body = MultipartBody.Part.createFormData(
                    "file", file.getName(), RequestBody.create(
                            MediaType.parse(mime), IOUtils.toByteArray(inputStream)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        inputStream.close();

        RequestBody id = RequestBody.create(MediaType.parse("multipart/form-data"), log.getId());
        retrofit.create(ApiService.class).uploadFiles(sessionManager.getAuthToken(),body,id).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<SuccessResponse>() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {

                }

                @Override
                public void onNext(@NonNull SuccessResponse posts) {
                    Log.i(TAG, "this is response uploadfiles " + posts.getMessage()+ file.getPath());
                    //TODO:parse response: if (success)
                    {
                        log.setUploadDate(Utils.getUniversalTimestamp());

                        if (file.delete()) {
                            log.setDeleted(true);
                            log.setStatus(UPLOADED_DELETED);
                        } else {
                            log.setStatus(UPLOADED);
                        }
                        //TODO:store received ID into FileLog RoomDB
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {

                    Log.i(TAG, "this is response onError uploadfiles " + e.getMessage() + file.getPath());
                }

                @Override
                public void onComplete() {

                }
            });
    }
}
