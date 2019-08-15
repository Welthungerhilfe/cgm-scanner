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
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver;
import de.welthungerhilfe.cgm.scanner.helper.service.AddressService;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonViewModel;
import de.welthungerhilfe.cgm.scanner.ui.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.MD5;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class CreateDataActivity extends BaseActivity {
    private final String TAG = CreateDataActivity.class.getSimpleName();
    private final int PERMISSION_STORAGE = 0x001;
    private final int PERMISSION_LOCATION = 0x002;

    public Person person;
    public List<Measure> measures;

    public String qrCode;

    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.viewpager)
    ViewPager viewpager;

    public Loc location = null;

    private AddressReceiver receiver = new AddressReceiver(new Handler()) {
        @Override
        public void onAddressDetected(String result) {
            location.setAddress(result);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        getCurrentLocation();

        qrCode = getIntent().getStringExtra(AppConstants.EXTRA_QR);

        PersonViewModel viewModel = ViewModelProviders.of(this).get(PersonViewModel.class);
        AppController.getInstance().personRepository.getPerson(qrCode).observe(this, person -> {
            if (person == null) {
                person = new Person();
                person.setId(AppController.getInstance().getPersonId());
                person.setQrcode(qrCode);
                person.setCreatedBy(AppController.getInstance().firebaseUser.getEmail());
            }

            viewModel.setPerson(person);
        });

        setupActionBar();
        initFragments();
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("ID: " + qrCode);
        }
    }

    private void initFragments() {
        PersonalDataFragment personalFragment = new PersonalDataFragment();
        MeasuresDataFragment measureFragment = new MeasuresDataFragment();
        GrowthDataFragment growthFragment = new GrowthDataFragment();

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(personalFragment, getResources().getString(R.string.tab_personal));
        adapter.addFragment(measureFragment, getResources().getString(R.string.tab_measures));
        adapter.addFragment(growthFragment, getResources().getString(R.string.tab_growth));
        viewpager.setOffscreenPageLimit(3);
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
    }

    public void gotoNextStep() {
        viewpager.setCurrentItem(viewpager.getCurrentItem() + 1);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MeasureResult event) {
        final Measure measure = event.getMeasureResult();
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setType(AppConstants.VAL_MEASURE_AUTO);
        measure.setTimestamp(Utils.getUniversalTimestamp());
        measure.setPersonId(person.getId());
        measure.setId(AppController.getInstance().getMeasureId());
        measure.setLocation(location);

        AppController.getInstance().measureRepository.insertMeasure(measure);

        person.setLastLocation(location);
        AppController.getInstance().personRepository.updatePerson(person);
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
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location loc = null;

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (isNetworkEnabled || isGPSEnabled) {
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
                    // new AddressTask(loc.getLatitude(), loc.getLongitude(), this).execute();
                    location = new Loc();
                    location.setLatitude(loc.getLatitude());
                    location.setLongitude(loc.getLongitude());

                    Intent intent = new Intent(this, AddressService.class);
                    intent.putExtra("add_receiver", receiver);
                    intent.putExtra("add_location", loc);
                    startService(intent);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] >= 0)
                getCurrentLocation();
        }
    }

    public void onBackPressed() {
        finish();
    }
}
