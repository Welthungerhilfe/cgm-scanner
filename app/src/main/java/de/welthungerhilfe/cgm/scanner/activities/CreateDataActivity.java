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

package de.welthungerhilfe.cgm.scanner.activities;

import android.Manifest;
import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.helper.service.FirebaseUploadService;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.viewmodels.PersonListViewModel;

/**
 * Created by Emerald on 2/19/2018.
 */

public class CreateDataActivity extends BaseActivity {
    private final String TAG = CreateDataActivity.class.getSimpleName();
    private final int PERMISSION_STORAGE = 0x001;

    public Person person;
    public List<Measure> measures;
    public ArrayList<Consent> consents;

    public String qrCode;
    public byte[] qrBitmapByteArray;
    public String qrPath;

    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.viewpager)
    ViewPager viewpager;

    private PersonalDataFragment personalFragment;
    private MeasuresDataFragment measureFragment;
    private GrowthDataFragment growthFragment;

    public PersonListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        viewModel = ViewModelProviders.of(this).get(PersonListViewModel.class);

        qrCode = getIntent().getStringExtra(AppConstants.EXTRA_QR);
        qrBitmapByteArray = getIntent().getByteArrayExtra(AppConstants.EXTRA_QR_BITMAP);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measures = new ArrayList<>();
        consents = new ArrayList<>();

        if (qrCode != null) {
            checkQR();
        } else {
            viewModel.getObservableMeasureList(person).observe(this, measures->{
                this.measures = measures;

                if (measures != null)
                    measureFragment.refreshMeasures(measures);
                growthFragment.setChartData();
            });
        }

        setupActionBar();
        initFragments();
        initUI();

        uploadQR();
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        if (person != null)
            actionBar.setTitle("ID: " + person.getQrcode());
        else
            actionBar.setTitle("ID: " + qrCode);
    }

    private void initFragments() {
        if (personalFragment == null)
            personalFragment = PersonalDataFragment.newInstance(this);
        if (measureFragment == null)
            measureFragment = MeasuresDataFragment.newInstance(this);
        if (growthFragment == null)
            growthFragment = GrowthDataFragment.newInstance(this);
    }

    private void initUI() {
        FragmentAdapter adapter = new FragmentAdapter(getFragmentManager());
        adapter.addFragment(personalFragment, getResources().getString(R.string.tab_personal));
        adapter.addFragment(measureFragment, getResources().getString(R.string.tab_measures));
        adapter.addFragment(growthFragment, getResources().getString(R.string.tab_growth));
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
    }

    public void setPersonalData(String name, String surName, long birthday, boolean age, String sex, Loc loc, String guardian) {
        boolean isNew = false;
        if (person == null) {
            isNew = true;

            person = new Person();
            person.setId(AppController.getInstance().getPersonId(name));
            person.setQrcode(qrCode);
            person.setCreated(System.currentTimeMillis());
        }

        person.setName(name);
        person.setSurname(surName);
        person.setLastLocation(loc);
        person.setBirthday(birthday);
        person.setGuardian(guardian);
        person.setSex(sex);
        person.setAgeEstimated(age);
        person.setTimestamp(Utils.getUniversalTimestamp());
        person.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());

        if (isNew)
            createPerson();
        else
            updatePerson();

        viewpager.setCurrentItem(1);
    }

    public void setMeasureData(double height, double weight, double muac, double headCircumference, String additional, Loc location) {
        final Measure measure = new Measure();
        measure.setId(AppController.getInstance().getMeasureId());
        measure.setDate(System.currentTimeMillis());
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setHeight(height);
        measure.setWeight(weight);
        measure.setMuac(muac);
        measure.setHeadCircumference(headCircumference);
        measure.setArtifact(additional);
        measure.setLocation(location);
        measure.setType(AppConstants.VAL_MEASURE_MANUAL);
        measure.setPersonId(person.getId());
        measure.setTimestamp(Utils.getUniversalTimestamp());
        measure.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());

        new OfflineTask().saveMeasure(measure);

        person.setLastLocation(location);
        new OfflineTask().updatePerson(person);
    }

    private void createPerson() {
        viewModel.createPerson(person);

        viewModel.getObservablePerson(person.getId()).observe(this, p -> {
            this.person = p;

            if (personalFragment != null) {
                personalFragment.initUI();
            }
        });
    }

    private void updatePerson() {
        OfflineRepository.getInstance().updatePerson(person);
    }

    private void checkQR() {
        viewModel.getObservablePersonByQr(qrCode).observe(this, p->{
            person = p;

            if (person != null) {
                viewModel.getObservableMeasureList(person).observe(this, measures->{
                    this.measures = measures;
                    if (measures != null)
                        measureFragment.refreshMeasures(measures);
                });
            }
        });
    }

    private void uploadQR() {
        if (qrBitmapByteArray == null)
            return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSION_STORAGE);
        } else {
            final long timestamp = Utils.getUniversalTimestamp();
            final String consentFileString = timestamp + "_" + qrCode + ".png";
            File extFileDir = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
            File consentFileFolder = new File(extFileDir, AppConstants.LOCAL_CONSENT_URL.replace("{qrcode}",qrCode));
            File consentFile = new File(consentFileFolder, consentFileString);
            if(!consentFileFolder.exists()) {
                boolean created = consentFileFolder.mkdirs();
                if (created) {
                    Log.i(TAG, "Folder: \"" + consentFileFolder + "\" created\n");
                } else {
                    Log.e(TAG,"Folder: \"" + consentFileFolder + "\" could not be created!\n");
                }
            }

            try (FileOutputStream fos = new FileOutputStream(consentFile)) {
                fos.write(qrBitmapByteArray);
                fos.flush();
                fos.close();

                // Start MyUploadService to upload the file, so that the file is uploaded
                // even if this Activity is killed or put in the background
                startService(new Intent(this, FirebaseUploadService.class)
                        .putExtra(FirebaseUploadService.EXTRA_FILE_URI, Uri.fromFile(consentFile))
                        .putExtra(AppConstants.EXTRA_QR, qrCode)
                        .putExtra(AppConstants.EXTRA_SCANTIMESTAMP, "")
                        .putExtra(AppConstants.EXTRA_SCANARTEFACT_SUBFOLDER, AppConstants.STORAGE_CONSENT_URL)
                        .setAction(FirebaseUploadService.ACTION_UPLOAD));

            } catch (Exception e) {
                e.printStackTrace();
            }


            if (person != null) {
                Consent consent = new Consent();
                consent.setCreated(timestamp);
                consent.setConsent(consentFile.getAbsolutePath());
                if (qrCode != null)
                    consent.setConsent(qrPath);
                else
                    consent.setQrcode(qrCode);
            }
        }
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

        new OfflineTask().saveMeasure(measure);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        SearchManager searchManager = (SearchManager) CreateDataActivity.this.getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(CreateDataActivity.this.getComponentName()));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
