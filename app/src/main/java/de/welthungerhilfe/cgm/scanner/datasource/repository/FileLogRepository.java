package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonLoad;

public class FileLogRepository {
    private static FileLogRepository instance;

    private CgmDatabase database;

    public FileLogRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static FileLogRepository getInstance(Context context) {
        if(instance == null) {
            instance = new FileLogRepository(context);
        }
        return instance;
    }

    public void loadQueuedData(OnFileLogLoad listener) {
        new AsyncTask<Void, Void, List<FileLog>>() {
            @Override
            protected List<FileLog> doInBackground(Void... voids) {
                return database.fileLogDao().loadQueuedData();
            }

            @Override
            public void onPostExecute(List<FileLog> data) {
                listener.onFileLogLoaded(data);
            }
        }.execute();
    }

    public void insertFileLog(FileLog log) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                database.fileLogDao().saveFileLog(log);
                return true;
            }
        }.execute();
    }

    public void updateFileLog(FileLog log) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                database.fileLogDao().updateFileLog(log);
                return true;
            }
        }.execute();
    }

    public void getSyncableLog(OnFileLogLoad listener, long timestamp) {
        new AsyncTask<Long, Void, List<FileLog>>() {
            @Override
            protected List<FileLog> doInBackground(Long... timestamp) {
                return database.fileLogDao().getSyncableData(timestamp[0]);
            }

            @Override
            public void onPostExecute(List<FileLog> data) {
                listener.onFileLogLoaded(data);
            }
        }.execute(timestamp);
    }
}
