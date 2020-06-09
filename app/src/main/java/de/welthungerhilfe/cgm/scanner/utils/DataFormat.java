package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;

import java.util.Locale;

public class DataFormat {

    public static String filesize(Context context, long bytes) {
        int kB = 1024;
        int MB = 1024 * 1024;

        if (bytes > 10 * MB) {
            return String.format(getCurrentLocale(context), "%dMB", bytes / MB);
        } else if (bytes > 0.1 * MB) {
            return String.format(getCurrentLocale(context), "%.1fMB", bytes / (float)MB);
        } else if (bytes > 0) {
            return String.format(getCurrentLocale(context), "%.1fKB", bytes / (float)kB);
        } else {
            return "-";
        }
    }

    public static String time(Context context, long milliSeconds) {
        if (milliSeconds >= 3600000) {
            return String.format(getCurrentLocale(context), "%.1fh", milliSeconds / 3600000);
        } else if (milliSeconds >= 60000) {
            return String.format(getCurrentLocale(context), "%.1fmin", milliSeconds / 60000.0f);
        } else if (milliSeconds >= 1000) {
            return String.format(getCurrentLocale(context), "%.1fs", milliSeconds / 1000.0f);
        } else if (milliSeconds >= 1) {
            return String.format(getCurrentLocale(context), "%dms", milliSeconds);
        } else {
            return "-";
        }
    }

    public static String timestamp(Context context, long milliSeconds) {
        if (milliSeconds > 0) {
            if (DateFormat.is24HourFormat(context)) {
                return DateFormat.format("kk:mm", milliSeconds).toString();
            } else {
                return DateFormat.format("hh:mm", milliSeconds).toString();
            }
        } else {
            return "- - : - -";
        }
    }

    private static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }
}
