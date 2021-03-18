package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;

public class LogFileUtils {
    static File logFile;
    public static List<String> logList;
    public static final int logLimit = 10;

    public static void startSession(SessionManager sessionManager) {
        logList = sessionManager.getLogList();
    }

    public static void logInfo(String text) {
        text = DataFormat.convertTimestampToDate(System.currentTimeMillis()) + ":" + text;
        logList.add(0, text);
    }

    public static File getLogFiles(Context context) {
        try {
            File log = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/cgm/LogFileUtils");
            if (!log.exists()) {
                log.mkdir();
            }

            logFile = new File(AppController.getInstance().getPublicAppDirectory(context), "Logs_file.txt");

            if (logFile.exists()) {
                logFile.delete();
            }
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //BufferedWriter for performance, true to set append to file flag
            Log.i("LogFileUtils", "this is size " + logList.size());
            int lines = logLimit;
            if (logList.size() < logLimit) {
                lines = logList.size();
            }

            for (int i = 0; i < lines; i++) {
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(logList.get(i));
                buf.newLine();
                buf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logFile;
    }
}
