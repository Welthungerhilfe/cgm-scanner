/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityLocationSearchBinding;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.AppConstants;

public class LocationSearchActivity extends BaseActivity implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener {

    private Location location = null;
    private SessionManager session;


    GoogleMap googleMap;

    ActivityLocationSearchBinding activityLocationSearchBinding;

    public void onCancel(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    public void onOK(View view) {
        setResult(RESULT_OK, LocationSearchActivity.this.getIntent().putExtra(AppConstants.EXTRA_RADIUS, radius));
        finish();
    }

    private int radius;
    private Circle circleRange;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityLocationSearchBinding = DataBindingUtil.setContentView(this,R.layout.activity_location_search);
        session = new SessionManager(LocationSearchActivity.this);

        activityLocationSearchBinding.mapView.onCreate(saveBundle);
        activityLocationSearchBinding.mapView.onResume();
        MapsInitializer.initialize(this);
        activityLocationSearchBinding.mapView.getMapAsync(this);

        radius = activityLocationSearchBinding.seekRadius.getProgress();
        activityLocationSearchBinding.txtRadius.setText(Integer.toString(radius));
        activityLocationSearchBinding.seekRadius.setOnSeekBarChangeListener(this);

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            GPS.openLocationSettings(this, PERMISSION_LOCATION);
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (isNetworkEnabled || isGPSEnabled) {
                List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (location == null || l.getAccuracy() < location.getAccuracy()) {
                        location = l;
                    }
                }
                if (location != null) {
                    getAddressFromLocation(new LatLng(location.getLatitude(), location.getLongitude()));

                    if (googleMap != null)
                        drawCircle();
                }
            }
        }
    }

    private void getAddressFromLocation(LatLng latLng) {
        runOnUiThread(() -> {
            Geocoder geocoder = new Geocoder(LocationSearchActivity.this, Locale.getDefault());
            String result = getString(R.string.address_parse_error);
            try {
                List <Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addressList != null && addressList.size() > 0) {
                    Address address = addressList.get(0);
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                        sb.append(address.getAddressLine(i));

                    result = sb.toString();
                }
            } catch (IOException e) {
                Log.e("Location Address Loader", "Unable connect to Geocoder", e);
            } finally {
                activityLocationSearchBinding.txtAddress.setText(result);

                Loc loc = new Loc();
                loc.setLatitude(location.getLatitude());
                loc.setLongitude(location.getLongitude());
                loc.setAddress(result);
                session.setLocation(loc);
            }
        });
    }

    private void drawCircle() {
        circleRange = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius(radius * 1000)
                .strokeColor(Color.WHITE)
                .strokeWidth(4.0f)
                .fillColor(getResources().getColor(R.color.colorGreenTransparent, getTheme())));
        circleRange.setVisible(true);

        googleMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onResume() {
        super.onResume();
        activityLocationSearchBinding.mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        activityLocationSearchBinding.mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityLocationSearchBinding.mapView != null)
            activityLocationSearchBinding.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        activityLocationSearchBinding.mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap mMap) {
        googleMap = mMap;

        if (location != null) {
            drawCircle();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        radius = i == 0 ? 1 : i;
        activityLocationSearchBinding.txtRadius.setText(Integer.toString(radius));
        if (circleRange != null) {
            circleRange.setRadius(radius * 1000);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        radius = seekBar.getProgress() == 0 ? 1 : seekBar.getProgress();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_LOCATION) {
            getCurrentLocation();
        }
    }
}
