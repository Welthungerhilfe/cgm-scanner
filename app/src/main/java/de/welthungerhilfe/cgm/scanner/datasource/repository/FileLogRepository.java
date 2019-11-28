package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;
import android.arch.persistence.room.Query;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_FILE_LOG;

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

    @SuppressLint("StaticFieldLeak")
    public void insertFileLog(FileLog log) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                database.fileLogDao().saveFileLog(log);
                return true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void updateFileLog(FileLog log) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                database.fileLogDao().updateFileLog(log);
                return true;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void getSyncableLog(OnFileLogsLoad listener, long timestamp) {
        new AsyncTask<Void, Void, List<FileLog>>() {
            @Override
            protected List<FileLog> doInBackground(Void... voids) {
                return database.fileLogDao().getSyncableData(timestamp);
            }

            @Override
            public void onPostExecute(List<FileLog> data) {
                listener.onFileLogsLoaded(data);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public int getArtifactCount() {
        return database.fileLogDao().getArtifactCount();
    }

    public int getDeletedArtifactCount() {
        return database.fileLogDao().getDeletedArtifactCount();
    }

    public int getTotalArtifactCount() {
        return database.fileLogDao().getTotalArtifactCount();
    }

    public double getArtifactFileSize() {
        return database.fileLogDao().getArtifactFileSize();
    }

    public double getTotalArtifactFileSize() {
        return database.fileLogDao().getTotalArtifactFileSize();
    }

    public double getMeasureArtifactSize(String measureId) {
        return database.fileLogDao().getMeasureArtifactSize(measureId);
    }

    public double getMeasureArtifactUploadedSize(String measureId) {
        return database.fileLogDao().getMeasureArtifactUploadedSize(measureId);
    }
}
