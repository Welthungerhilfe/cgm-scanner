/*
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import de.welthungerhilfe.cgm.scanner.ui.activities.QRScanActivity;

public class Utils {

    public static final String PACKAGE_GOOGLE_PLAY = "com.android.vending";

    public static String getAndroidID(ContentResolver resolver) {
        return Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID);
    }

    public static String getAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public static String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }

        return salt.toString();
    }

    public static long getUniversalTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        return calendar.getTimeInMillis();
    }

    public static String getNameFromEmail(String email) {
        if (email == null || email.isEmpty())
            return "unknown";
        else {
            String[] arr = email.split("@");
            return arr[0];
        }
    }

    public static boolean isPackageInstalled(Activity activity, String packageName) {
        PackageManager pm = activity.getPackageManager();

        for (PackageInfo info : activity.getPackageManager().getInstalledPackages(0)) {
            if (info.packageName.compareTo(packageName) == 0) {
                int state = pm.getApplicationEnabledSetting(packageName);
                switch (state) {
                    case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                    case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                        return false;
                    default:
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean isStdTestQRCode(String qrcode) {
        if (qrcode == null) {
            return false;
        }
        return qrcode.toUpperCase().contains("STD_TEST_");
    }

    public static QRScanActivity.STDTEST isValidateStdTestQrCode(String qrcode) {
        try {
            String[] arrOfStr = qrcode.split("_", 5);
            String date = arrOfStr[2];
            Date c = Calendar.getInstance().getTime();

            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String formattedDate = df.format(c);

            if (date.equals(formattedDate)) {
                return QRScanActivity.STDTEST.VALID;
            } else {

                if(Double.parseDouble(date) > Double.parseDouble(formattedDate)){
                    return QRScanActivity.STDTEST.INFUTURE;
                }
                else if(Double.parseDouble(date) < Double.parseDouble(formattedDate)) {
                    return QRScanActivity.STDTEST.OLDER;
                }
                else {
                    return QRScanActivity.STDTEST.INVALID;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return QRScanActivity.STDTEST.INVALID;
        }
    }

    public static void openPlayStore(Activity activity, String packageName) {
        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }

    public static void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
