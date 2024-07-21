package de.welthungerhilfe.cgm.scanner.hardware.io;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.appcenter.analytics.Analytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

public class LogFileUtils {
    static File logFile;
    public static final int maxFiles = 10;
    public static final String TAG = LogFileUtils.class.getSimpleName();
    public static WriteToLogFileAsynck writeToLogFileAsynck;


    public static void startSession(Context context, SessionManager sessionManager) {

        File extFileDir = AppController.getInstance().getRootDirectory(context);
        File logFilesFolder = new File(extFileDir, AppConstants.LOG_FILE_FOLDER);
        if (!logFilesFolder.exists()) {
            logFilesFolder.mkdir();
        }
        String fileName = new SimpleDateFormat("yyyy-MM-dd'.txt'").format(new Date());

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

    public static void logInfo(String tag, String text) {
        Log.i(tag, text);
        String message = DataFormat.convertMilliSeconsToServerDate(System.currentTimeMillis());
        message += " : Info-" + tag + " -> " + text;
        startAsyncToWrite(message);

    }

    public static void logInfo2(String tag, String text) {
        Log.i(tag, text);
        String message = DataFormat.convertMilliSeconsToServerDate(System.currentTimeMillis());
        message += " : Info-" + tag + " -> " + text;
        startAsyncToWrite(message);

    }
    public static void logError(String tag, String text) {
        Log.e(tag, text);
        String message = DataFormat.convertMilliSeconsToServerDate(System.currentTimeMillis());
        message += " : Error-" + tag + " -> " + text;
        startAsyncToWrite(message);
    }

    public static void logException(Throwable exception,String title) {
        String text = DataFormat.convertMilliSeconsToServerDate(System.currentTimeMillis()) + " :Exception-> "+title+": " + Log.getStackTraceString(exception);
        startAsyncToWrite(text);
    }

    public static void maintainLogFiles(File folder) {
        final File[] sortedByDate = folder.listFiles();
        if (sortedByDate == null) {
            return;
        }

        if (sortedByDate.length > 1) {
            Arrays.sort(sortedByDate, (object1, object2) -> Long.compare(object2.lastModified(), object1.lastModified()));
        }
        dirSize(sortedByDate);
        if (sortedByDate.length > maxFiles) {
            for (int i = maxFiles; i < sortedByDate.length; i++) {
                File deleteFile = new File(sortedByDate[i].getAbsolutePath());
                if (deleteFile.exists()) {
                    deleteFile.delete();
                }
            }
        }
    }

    public static void startAsyncToWrite(String str) {
        writeToLogFileAsynck = new WriteToLogFileAsynck();
        writeToLogFileAsynck.execute(str);
    }

    public static void writeAppCenter(SessionManager sessionManager, String TAG, String msg) {
        Map<String, String> properties = new HashMap<>();
        properties.put("User", sessionManager.getUserEmail());
        if (sessionManager.getStdTestQrCode() != null) {
            properties.put("Msg", msg + " " + sessionManager.getUserEmail() + " - " + sessionManager.getStdTestQrCode() + " - " + sessionManager.getEnvironment());
        } else {
            properties.put("Msg", msg + " " + sessionManager.getUserEmail() + " - " + sessionManager.getEnvironment());
        }
        Analytics.trackEvent(TAG, properties);
    }

    static class WriteToLogFileAsynck extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                if (params[0] != null) {
                    addTextToFile(params[0]);
                }
            } catch (Exception e) {
                e.getStackTrace();
            }
            return null;
        }

        public synchronized void addTextToFile(String data) {
            try {
                FileOutputStream output = new FileOutputStream(logFile, true);
                byte[] array = data.getBytes();
                String lineSeparator = System.getProperty("line.separator");
                output.write(array);
                output.write(lineSeparator.getBytes());
                output.flush();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void dirSize(File[] fileList) {


        long result = 0;

        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                result = fileList[i].length();
                Log.i("LogfileUtils ", "this is size of file" + fileList[i].getName() + " " + fileList[i].length());


            }
        }
        Log.i("LogfileUtils ", "this is size of file" + result / 1048576);

        if ((result / 1048576) > 3060) {
            for (int i = 1; i < fileList.length; i++) {
                Log.i("LogfileUtils ", "this is size of file" + fileList[i].getName() + " " + fileList[i].length());
                File deleteFile = new File(fileList[i].getAbsolutePath());
                if (deleteFile.exists()) {
                    deleteFile.delete();
                }

            }

        }


    }
}

