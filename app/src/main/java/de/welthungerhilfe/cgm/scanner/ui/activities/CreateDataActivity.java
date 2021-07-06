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
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;

import android.util.Log;
import android.view.MenuItem;

import java.util.List;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityCreateBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModelProvideFactory;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCreateBinding = DataBindingUtil.setContentView(this,R.layout.activity_create);


        getCurrentLocation();

        qrCode = getIntent().getStringExtra(AppConstants.EXTRA_QR);

        setupActionBar();
        initFragments();


        factory = new CreateDataViewModelProvideFactory(this);
        viewModel = new ViewModelProvider(this,factory).get(CreateDataViewModel.class);
        viewModel.getCurrentTab().observe(this, tab -> {
            activityCreateBinding.viewpager.setCurrentItem(tab);
        });
        viewModel.syncManualMeasurements(qrCode);

    }

    private void setupActionBar() {
        setSupportActionBar(activityCreateBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("ID: " + qrCode);
        }
    }

    private void initFragments() {
        personalFragment = PersonalDataFragment.getInstance(qrCode);
        measureFragment = MeasuresDataFragment.getInstance(qrCode);
        growthFragment = GrowthDataFragment.getInstance(qrCode);

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(personalFragment, getResources().getString(R.string.tab_personal));
        adapter.addFragment(measureFragment, getResources().getString(R.string.tab_measures));
        adapter.addFragment(growthFragment, getResources().getString(R.string.tab_growth));
        activityCreateBinding.viewpager.setOffscreenPageLimit(3);
        activityCreateBinding.viewpager.setAdapter(adapter);

        activityCreateBinding.tabs.setupWithViewPager(activityCreateBinding.viewpager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();

            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            Utils.openLocationSettings(this, PERMISSION_LOCATION);
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

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
                    location.setAddress(Utils.getAddress(this, location));

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
}
