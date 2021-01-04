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

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsActivity;

public class Utils {

    private static MediaActionSound sound = null;

    public static long averageValue(ArrayList<Long> values) {
        long value = 0;
        if(values==null)
        {
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

    public static double parseDouble(String value) {
        if(value==null)
        {
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
        if(value==null)
        {
            return 0;
        }
        value = value.replace(',', '.');
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
        }
        return 0;
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
        if (email==null || email.isEmpty())
            return "unknown";
        else {
            String[] arr = email.split("@");
            return arr[0];
        }
    }

    public static int checkDoubleDecimals(String number) {
        number = number.replace(',', '.');
        int integerPlaces = number.indexOf('.');

        if (integerPlaces < 0)
            return 0;

        return number.length() - integerPlaces - 1;
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

    public static Loc getLastKnownLocation(Context context) {
        boolean coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            return null;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location:
                bestLocation = l;
            }
        }

        if (bestLocation == null) {
            return null;
        }

        Loc loc = new Loc();
        loc.setLatitude(bestLocation.getLatitude());
        loc.setLongitude(bestLocation.getLongitude());
        return loc;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi != null && wifi.isConnected();
    }

    public static void openLocationSettings(Activity activity, int resultCode) {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(60 * 1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        LocationServices
                .getSettingsClient(activity)
                .checkLocationSettings(builder.build())
                .addOnFailureListener(activity, ex -> {
                    if (ex instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(activity, resultCode);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public static int dpToPx(float dp, Context context) {
        return (int) (dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
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

    public static boolean isUploadAllowed(Context context) {
        boolean wifiOnly = LocalPersistency.getBoolean(context, SettingsActivity.KEY_UPLOAD_WIFI);
        if (wifiOnly) {
            if (Utils.isWifiConnected(context)) {
                return true;
            }
        } else if (Utils.isNetworkAvailable(context)) {
            return true;
        }
        return false;
    }

    public static void openPlayStore(Activity activity, String packageName) {
        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        activity.startActivity(intent);
    }

    public static void playShooterSound(Context context, int sample) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_NORMAL:
                if (sound == null) {
                    sound = new MediaActionSound();
                }
                sound.play(sample);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
        }
    }

    public static void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<TutorialData> getTutorialData(Context context) {
        ArrayList<TutorialData> tutorialDataList = new ArrayList<TutorialData>();
        TutorialData tutorialData = new TutorialData(R.drawable.tutorial1,context.getResources().getString(R.string.tutorial1),context.getResources().getString(R.string.tutorial11),context.getResources().getString(R.string.tutorial12),0);
        tutorialDataList.add(tutorialData);
        tutorialData = new TutorialData(R.drawable.tutorial2,context.getResources().getString(R.string.tutorial2),context.getResources().getString(R.string.tutorial21),context.getResources().getString(R.string.tutorial22),1);
        tutorialDataList.add(tutorialData);
        tutorialData = new TutorialData(R.drawable.tutorial3,context.getResources().getString(R.string.tutorial3),context.getResources().getString(R.string.tutorial31),context.getResources().getString(R.string.tutorial32),2);
        tutorialDataList.add(tutorialData);
        tutorialData = new TutorialData(R.drawable.tutorial4,context.getResources().getString(R.string.tutorial4),context.getResources().getString(R.string.tutorial41),context.getResources().getString(R.string.tutorial42),3);
        tutorialDataList.add(tutorialData);
        return tutorialDataList;


    }

    //for checking-> MSAL authtoken expired or not
    public static boolean isExpiredToken(String message) {
        return message.contains("401");
    }
}
