package de.welthungerhilfe.cgm.scanner.utils;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class DataFormat {

    public static String digits(int value, int digits) {
        String output = "" + value;
        while (output.length() < digits) {
            output = "0" + output;
        }
        return output;
    }

    public static String filesize(long bytes) {
        int kB = 1024;
        int MB = 1024 * 1024;

        if (bytes > 10 * MB) {
            return String.format(Locale.US, "%dMB", bytes / MB);
        } else if (bytes > 0.1 * MB) {
            return String.format(Locale.US, "%.1fMB", bytes / (float)MB);
        } else if (bytes > 0) {
            return String.format(Locale.US, "%.1fKB", bytes / (float)kB);
        } else {
            return "-";
        }
    }

    public static String time(long milliSeconds) {
        if (milliSeconds >= 3600000) {
            return String.format(Locale.US,"%.1fh", milliSeconds / 3600000);
        } else if (milliSeconds >= 60000) {
            return String.format(Locale.US,"%.1fmin", milliSeconds / 60000.0f);
        } else if (milliSeconds >= 1000) {
            return String.format(Locale.US,"%.1fs", milliSeconds / 1000.0f);
        } else if (milliSeconds >= 1) {
            return String.format(Locale.US,"%dms", milliSeconds);
        } else {
            return "-";
        }
    }

    public static String timestamp(long milliSeconds) {
        if (milliSeconds > 0) {
            Calendar cal = Calendar.getInstance(Locale.US);
            cal.setTimeInMillis(milliSeconds);
            return DateFormat.format("hh:mm", cal).toString();
        } else {
            return "- - : - -";
        }
    }
}
