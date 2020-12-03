/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DataFormat {

    public enum TimestampFormat {
        DATE,
        DATE_AND_TIME,
        TIME
    };

    public static String convertFormat(Context context, TimestampFormat format) {
        switch (format) {
            case DATE:
                return getDateFormat(context);
            case DATE_AND_TIME:
                return getDateTimeFormat(context);
            case TIME:
                return getTimeFormat(context);
            default:
                return null;
        }
    }

    public static String convertTimestampToDate(Long timeStamp)
    {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timeStamp);
        String date = DateFormat.format("yyyy-MM-dd hh:mm:ss", cal).toString();
        return date;
    }

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
            return String.format(getCurrentLocale(context), "%.1fh", milliSeconds / 3600000.0f);
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

    public static long timestamp(Context context, TimestampFormat format, String value) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat(convertFormat(context, format), Locale.US);;
            return parser.parse(value).getTime();
        } catch (Exception e) {
        }
        return System.currentTimeMillis();
    }

    public static String timestamp(Context context, TimestampFormat format, long timestamp) {
        if (timestamp > 0) {
            return DateFormat.format(convertFormat(context, format), timestamp).toString();
        }
        return "- - : - -";
    }

    private static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }

    public static String getDateFormat(Context context) {
        char s = getDateSeparator(context);
        return "dd" + s + "MM" + s + "yyyy";
    }

    public static char getDateSeparator(Context context) {
        return '-';
    }

    public static String getDateTimeFormat(Context context) {
        return getTimeFormat(context) + " " + getDateFormat(context);
    }

    public static String getTimeFormat(Context context) {
        if (DateFormat.is24HourFormat(context)) {
            return "kk:mm";
        } else {
            return "hh:mm";
        }
    }
}
