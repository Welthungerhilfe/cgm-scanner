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
package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DataFormat {

    public enum TimestampFormat {
        DATE,
        DATE_AND_TIME,
        TIME
    }

    ;

    public static long averageValue(ArrayList<Long> values) {
        long value = 0;
        if (values == null) {
            return value;
        }
        for (long l : values) {
            value += l;
        }
        if (values.size() > 0) {
            value /= values.size();
        }
        return value;
    }

    public static int checkDoubleDecimals(String number) {
        number = number.replace(',', '.');
        int integerPlaces = number.indexOf('.');

        if (integerPlaces < 0)
            return 0;

        return number.length() - integerPlaces - 1;
    }

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

    public static String convertMilliSeconsToServerDate(Long timeStamp) {
        /*TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(timeStamp);
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString();*/

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timeStamp));
    }

    public static String convertMilliSecondToBirthDay(Long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH);
        return sdf.format(new Date(timeStamp));
    }

    public static long convertServerDateToMilliSeconds(String str) {
        if (str == null) {
            return 0;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (str.contains("T")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                Date date = dateFormat.parse(str);//You will get date object relative to server/client timezone wherever it is parsed
                str = format.format(date);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        try {
            Date date = format.parse(str);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static long convertBirthDateToMilliSeconds(String str) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = format.parse(str);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getNameFromEmail(String email) {
        if (email == null || email.isEmpty())
            return "unknown";
        else {
            String[] arr = email.split("@");
            return arr[0];
        }
    }

    public static String filesize(Context context, long bytes) {
        int kB = 1024;
        int MB = 1024 * 1024;

        if (bytes > 10 * MB) {
            return String.format(getCurrentLocale(context), "%dMB", bytes / MB);
        } else if (bytes > 0.1 * MB) {
            return String.format(getCurrentLocale(context), "%.1fMB", bytes / (float) MB);
        } else if (bytes > 0) {
            return String.format(getCurrentLocale(context), "%.1fKB", bytes / (float) kB);
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
            SimpleDateFormat parser = new SimpleDateFormat(convertFormat(context, format), Locale.US);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

    public static boolean isNumber(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c != '.') && (c != ',')) {
                if ((c < '0') || (c > '9')) {
                    return false;
                }
            }
        }
        return true;
    }

    public static double parseDouble(String value) {
        if (value == null) {
            return 0;
        }
        value = value.replace(',', '.');
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
        }
        return 0;
    }

    public static float parseFloat(String value) {
        if (value == null) {
            return 0;
        }
        value = value.replace(',', '.');
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
        }
        return 0;
    }

    public static int monthsBetweenDates(String endDateStr) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String strDate = formatter.format(new Date());
        SimpleDateFormat endDateFormatter = new SimpleDateFormat("dd-MM-yyyy");

        ParsePosition parsePosition = new ParsePosition(0);
        Date endDate = endDateFormatter.parse(endDateStr, parsePosition);
        endDateStr = formatter.format(endDate);

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        ParsePosition parsePosition1 = new ParsePosition(0);
        end.setTime(formatter.parse(strDate, parsePosition1));

        ParsePosition parsePosition2 = new ParsePosition(0);
        start.setTime(formatter.parse(endDateStr, parsePosition2));


        int monthsBetween = 0;
        int dateDiff = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);

        if (dateDiff < 0) {
            int borrrow = end.getActualMaximum(Calendar.DAY_OF_MONTH);
            dateDiff = (end.get(Calendar.DAY_OF_MONTH) + borrrow) - start.get(Calendar.DAY_OF_MONTH);
            monthsBetween--;

            if (dateDiff > 0) {
                monthsBetween++;
            }
        } else {
            monthsBetween++;
        }
        monthsBetween += end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        monthsBetween += (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12;
        return monthsBetween;
    }
}
