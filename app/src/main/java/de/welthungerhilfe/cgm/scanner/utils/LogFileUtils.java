package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;

public class LogFileUtils {
    static File logFile;
    public static final int maxFiles = 10;
    public static final String TAG = LogFileUtils.class.getSimpleName();

    public static void startSession(Context context, SessionManager sessionManager) {

        File extFileDir = AppController.getInstance().getRootDirectory(context);
        File logFilesFolder = new File(extFileDir, AppConstants.LOG_FILE_FOLDER);
        if (!logFilesFolder.exists()) {
            logFilesFolder.mkdir();
        }
        String fileName = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss'.txt'").format(new Date());

        logFile = new File(logFilesFolder, fileName);
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            sessionManager.setCurrentLogFilePath(logFile.getAbsolutePath());
            maintainLogFiles(logFilesFolder);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void initLogFile(SessionManager sessionManager, Context context) {
        if (sessionManager.getCurrentLogFilePath() == null) {
            startSession(context, sessionManager);
        }
        logFile = new File(sessionManager.getCurrentLogFilePath());
        if (!logFile.exists()) {
            startSession(context, sessionManager);
        }
    }

    public static void logInfo(String text) {
        text = DataFormat.convertTimestampToDate(System.currentTimeMillis()) + ":" + text;
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {

        }
    }

    public static void maintainLogFiles(File folder) {
        final File[] sortedByDate = folder.listFiles();

        if (sortedByDate != null && sortedByDate.length > 1) {
            Arrays.sort(sortedByDate, (object1, object2) -> Long.compare(object2.lastModified(), object1.lastModified()));
        }
        if (sortedByDate.length > maxFiles) {
            File deleteFile = null;
            for (int i = maxFiles; i < sortedByDate.length; i++) {
                deleteFile = new File(sortedByDate[i].getAbsolutePath());
                if (deleteFile.exists()) {
                    deleteFile.delete();
                }
            }
        }
        Log.i(TAG, "this is value of filles " + sortedByDate.length);

    }
}
