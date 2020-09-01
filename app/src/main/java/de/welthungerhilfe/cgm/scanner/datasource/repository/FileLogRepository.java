package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;
import androidx.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;

public class FileLogRepository {
    private static FileLogRepository instance;

    private CgmDatabase database;

    private FileLogRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static FileLogRepository getInstance(Context context) {
        if(instance == null) {
            instance = new FileLogRepository(context);
        }
        return instance;
    }

    @SuppressLint("StaticFieldLeak")
    public void loadQueuedData(OnFileLogsLoad listener) {
        new AsyncTask<Void, Void, List<FileLog>>() {
            @Override
            protected List<FileLog> doInBackground(Void... voids) {
                return database.fileLogDao().loadQueuedData();
            }

            @Override
            public void onPostExecute(List<FileLog> data) {
                listener.onFileLogsLoaded(data);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void insertFileLog(FileLog log) {
        database.fileLogDao().saveFileLog(log);
    }

    public void updateFileLog(FileLog log) {
        database.fileLogDao().updateFileLog(log);
    }

    public long getArtifactCount() {
        return database.fileLogDao().getArtifactCount();
    }

    public long getDeletedArtifactCount() {
        return database.fileLogDao().getDeletedArtifactCount();
    }

    public long getTotalArtifactCount() {
        return database.fileLogDao().getTotalArtifactCount();
    }

    public double getArtifactFileSize() {
        return database.fileLogDao().getArtifactFileSize();
    }

    public double getTotalArtifactFileSize() {
        return database.fileLogDao().getTotalArtifactFileSize();
    }

    public List<FileLog> getAll() {
        return database.fileLogDao().getAll();
    }

    public List<FileLog> getArtifactsForMeasure(String measureId, int offset, int limit) {
        return database.fileLogDao().getArtifactsForMeasure(measureId, offset, limit);
    }

    public long getTotalArtifactCountForMeasure(String measureId) {
        return database.fileLogDao().getTotalArtifactCountForMeasure(measureId);
    }

    public LiveData<UploadStatus> getMeasureUploadProgress(String measureId) {
        return database.fileLogDao().getMeasureUploadProgress(measureId);
    }
}
