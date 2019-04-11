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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.events.LocationResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;

/**
 * Created by Emerald on 2/20/2018.
 */

public class LocationDetectActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {
    public static final String EXTRA_LOCATION = "extra_location";
    public static final String KEY_TRANSITION = "key_transition";

    private final int PERMISSION_LOCATION = 0x1001;
    private final int REQUEST_LOCATION = 0x1002;

    private Marker marker = null;

    @BindView(R.id.editAddress)
    EditText editAddress;
    @BindView(R.id.mapView)
    MapView mapView;
    GoogleMap googleMap;

    @OnClick(R.id.lytConfirm)
    void onConfirm(LinearLayout lytConfirm) {
        EventBus.getDefault().post(new LocationResult(location));
        onBackPressed();
    }

    @OnClick(R.id.fabLocation)
    void onMyLocation(FloatingActionButton fabLocation) {
        Location location = googleMap.getMyLocation();

        if (location != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        } else {
            Toast.makeText(LocationDetectActivity.this, R.string.error_location, Toast.LENGTH_SHORT).show();
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(0, 0)));
        }
    }

    @OnClick(R.id.imgClose)
    void onClose(ImageView imgClose) {
        onBackPressed();
    }

    private Loc location;

    public static void navigate(AppCompatActivity activity, View viewAddress, Loc location) {
        Intent intent = new Intent(activity, LocationDetectActivity.class);
        intent.putExtra(EXTRA_LOCATION, location);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, viewAddress, KEY_TRANSITION);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_location_detect);
        ButterKnife.bind(this);

        ViewCompat.setTransitionName(findViewById(R.id.editAddress), KEY_TRANSITION);
        location = (Loc) getIntent().getSerializableExtra(EXTRA_LOCATION);

        if (location != null)
            editAddress.setText(location.getAddress());
        else
            location = new Loc();

        editAddress.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        editAddress.setRawInputType(InputType.TYPE_CLASS_TEXT);
        editAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (editAddress.getText().toString().isEmpty())
                    return false;

                getLocationFromAddress(editAddress.getText().toString());
                return true;
            }
            return false;
        });

        mapView.onCreate(saveBundle);
        mapView.onResume();
        MapsInitializer.initialize(this);
        mapView.getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap mMap) {
        googleMap = mMap;

        drawMarker();
        googleMap.setOnCameraIdleListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_LOCATION);
        } else {
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void drawMarker() {
        LatLng pos = null;
        if (location != null) {
            pos = new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            pos = new LatLng(0, 0);
        }

        CameraPosition cameraPosition = new CameraPosition.Builder().target(pos).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void getLocationFromAddress(String address) {
        runOnUiThread(() -> {
            Geocoder geocoder = new Geocoder(LocationDetectActivity.this, Locale.getDefault());

            List<Address> addressList = null;
            try {
                addressList = geocoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    Address addr = addressList.get(0);
                    location.setAddress(address);
                    location.setLongitude(addr.getLongitude());
                    location.setLatitude(addr.getLatitude());

                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(addr.getLatitude(), addr.getLongitude())));
                } else {
                    Snackbar.make(mapView, R.string.error_location, Snackbar.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Snackbar.make(mapView, R.string.error_location, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void getAddressFromLocation(LatLng latLng) {
        //new AddressTask(location.getLatitude(), location.getLongitude(), this).execute();

        runOnUiThread(() -> {
            Geocoder geocoder = new Geocoder(LocationDetectActivity.this, Locale.getDefault());
            String result = null;
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
                location.setAddress(result);
                editAddress.setText(result);
            }
        });
    }

    @Override
    public void onCameraIdle() {
        LatLng l = googleMap.getCameraPosition().target;

        location.setLatitude(l.latitude);
        location.setLongitude(l.longitude);

        getAddressFromLocation(l);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.setMyLocationEnabled(true);
            }
        }
    }
}
