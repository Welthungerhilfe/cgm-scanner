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
package de.welthungerhilfe.cgm.scanner.hardware;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;

public class GPS {

    private static final String PACKAGE_GOOGLE_PLAY = "com.android.vending";

    public static String locality = null;

    public static String getAddress(Activity context, Loc location) {
        if (location != null) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = null;

            try {
                if (isPackageInstalled(context, PACKAGE_GOOGLE_PLAY)) {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                }
            } catch (Exception ioException) {
                Log.e("", "Error in getting address for the location");
            }

            if (addresses == null || addresses.size() == 0) {
                return "";
            } else {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                locality = address.getLocality();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                    sb.append(address.getAddressLine(i));

                return sb.toString();
            }
        }
        return "";
    }

    public static String getLocality(){
        return locality;
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

    private static boolean isPackageInstalled(Activity activity, String packageName) {
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
}
