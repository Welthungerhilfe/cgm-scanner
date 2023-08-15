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

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;

import java.util.List;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityCreateBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModelProvideFactory;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class CreateDataActivity extends BaseActivity {

    String TAG = CreateDataActivity.class.getSimpleName();

    public String qrCode;

    PersonalDataFragment personalFragment;
    MeasuresDataFragment measureFragment;
    GrowthDataFragment growthFragment;

    ViewModelProvider.Factory factory;

    CreateDataViewModel viewModel;

    public Loc location = null;

    ActivityCreateBinding activityCreateBinding;

    SessionManager sessionManager;
    boolean requestingLocationUpdates = false;
    LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    Person person;
    PersonRepository personRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCreateBinding = DataBindingUtil.setContentView(this, R.layout.activity_create);

        sessionManager = new SessionManager(this);
        personRepository = PersonRepository.getInstance(this);
        getCurrentLocation();

        qrCode = getIntent().getStringExtra(AppConstants.EXTRA_QR);
        person = personRepository.findPersonByQr(qrCode,sessionManager.getEnvironment());


        setupActionBar();
        initFragments();

        activityCreateBinding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        factory = new CreateDataViewModelProvideFactory(this);
        viewModel = new ViewModelProvider(this, factory).get(CreateDataViewModel.class);
        viewModel.getCurrentTab().observe(this, tab -> {
            activityCreateBinding.viewpager.setCurrentItem(tab);
        });
        viewModel.syncManualMeasurements(qrCode,sessionManager.getEnvironment());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                /*for (Location location : locationResult.getLocations()) {
                    Toast.makeText(CreateDataActivity.this, "data -> " + location.getLongitude() + " " + location.getLatitude(), Toast.LENGTH_LONG).show();
                }*/
                getCurrentLocation();
                stopLocationUpdates();

            }

        };


    }

    private void setupActionBar() {
        setSupportActionBar(activityCreateBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
           /* actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);*/
            //actionBar.setTitle("ID: " + qrCode);
            actionBar.setTitle("");
            activityCreateBinding.tvTitle.setText("ID: " + qrCode);

            invalidateOptionsMenu();
        }
    }

    private void initFragments() {
        personalFragment = PersonalDataFragment.getInstance(qrCode);
        measureFragment = MeasuresDataFragment.getInstance(qrCode);
        growthFragment = GrowthDataFragment.getInstance(qrCode);

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(personalFragment, getResources().getString(R.string.tab_personal));
        adapter.addFragment(measureFragment, getResources().getString(R.string.tab_measures));
        if(person!=null) {
            if (person.isBelongs_to_rst()) {
                activityCreateBinding.viewpager.setOffscreenPageLimit(2);

            } else {
                adapter.addFragment(growthFragment, getResources().getString(R.string.tab_growth));
                activityCreateBinding.viewpager.setOffscreenPageLimit(3);
            }
        }else{
            if (sessionManager.getSelectedMode() == AppConstants.RST_MODE) {
                activityCreateBinding.viewpager.setOffscreenPageLimit(2);

            } else {
                adapter.addFragment(growthFragment, getResources().getString(R.string.tab_growth));
                activityCreateBinding.viewpager.setOffscreenPageLimit(3);
            }        }
        activityCreateBinding.viewpager.setAdapter(adapter);

        activityCreateBinding.tabs.setupWithViewPager(activityCreateBinding.viewpager);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            GPS.openLocationSettings(this, PERMISSION_LOCATION);
            LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location loc = null;

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else {
                List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (loc == null || l.getAccuracy() < loc.getAccuracy()) {
                        loc = l;
                    }
                }
                if (loc != null) {
                    location = new Loc();
                    location.setLatitude(loc.getLatitude());
                    location.setLongitude(loc.getLongitude());
                    location.setAddress(GPS.getAddress(this, location));

                    if (personalFragment != null) {
                        personalFragment.setLocation(location);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            getCurrentLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.getStdTestQrCode() != null) {
            activityCreateBinding.toolbar.setBackgroundResource(R.color.colorPink);
            activityCreateBinding.tabs.setBackgroundResource(R.color.colorPink);
        } else {
            activityCreateBinding.toolbar.setBackgroundResource(R.color.colorPrimary);
            activityCreateBinding.tabs.setBackgroundResource(R.color.colorPrimary);
        }
        if (!requestingLocationUpdates) {
            requestingLocationUpdates = true;
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    protected void createLocationRequest() {

             locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if(requestingLocationUpdates) {
            requestingLocationUpdates = false;
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }}
