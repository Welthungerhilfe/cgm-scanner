/**
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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;

public class Utils {

    public static long averageValue(ArrayList<Long> values) {
        long value = 0;
        for (long l : values) {
            value += l;
        }
        if (values.size() > 0) {
            value /= values.size();
        }
        return value;
    }

    public static void overrideFont(Context context, String defaultFontNameToOverride, String customFontFileNameInAssets) {
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), customFontFileNameInAssets);

            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");  // Split on one or more spaces

            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");

            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public static boolean isNetworkConnectionAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    public static String getAndroidID(ContentResolver resolver) {
        return Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID);
    }

    public static String getUUID() {
        return UUID.randomUUID().toString();
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
        if (email == null)
            return "unknown";
        else {
            String[] arr = email.split("@");
            return arr[0];
        }
    }

    public static double distanceBetweenLocs(Loc l1, Loc l2) {
        Location loc1 = new Location("");
        loc1.setLatitude(l1.getLatitude());
        loc1.setLongitude(l1.getLongitude());

        Location loc2 = new Location("");
        loc2.setLatitude(l2.getLatitude());
        loc2.setLongitude(l2.getLongitude());

        return loc1.distanceTo(loc2);
    }

    public static boolean checkDouble(String number) {
        try {
            Integer.parseInt(number);

            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    public static int checkDoubleDecimals(String number) {
        int integerPlaces = number.indexOf('.');

        if (integerPlaces < 0)
            return 0;

        int decimalPlaces = number.length() - integerPlaces - 1;

        return decimalPlaces;
    }

    public static byte[] readFile(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }

        return bytes;
    }

    public static float interpolate(final float a, final float b, final float proportion) {
        return (a + ((b - a) * proportion));
    }

    public static int interpolateColor(final int a, final int b, final float proportion) {
        final float[] hsva = new float[3];
        final float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }

        return Color.HSVToColor(hsvb);
    }

    public static int getColorWithAlpha(int color, float ratio) {
        int newColor = 0;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        newColor = Color.argb(alpha, r, g, b);
        return newColor;
    }

    public static String getAddress(Context context, Loc location) {
        if (location != null) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            } catch (Exception ioException) {
                Log.e("", "Error in getting address for the location");
            }

            if (addresses == null || addresses.size() == 0) {
                return "";
            } else {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                    sb.append(address.getAddressLine(i));

                return sb.toString();
            }
        }
        return "";
    }
}
