package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
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

    public void loadQueuedData(OnFileLogsLoad listener) {
        ((Runnable) () -> {
            List<FileLog> data = database.fileLogDao().loadQueuedData();
            listener.onFileLogsLoaded(data);
        }).run();
    }

    public void insertFileLog(FileLog log) {
        ((Runnable) () -> {
            database.fileLogDao().saveFileLog(log);
        }).run();
    }

    public void updateFileLog(FileLog log) {
        ((Runnable) () -> {
            database.fileLogDao().updateFileLog(log);
        }).run();
    }

    public void getSyncableLog(OnFileLogsLoad listener, long timestamp) {
        ((Runnable) () -> {
            List<FileLog> data = database.fileLogDao().getSyncableData(timestamp);
            listener.onFileLogsLoaded(data);
        }).run();
    }
}
