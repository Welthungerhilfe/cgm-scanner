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
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.SearchView;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.realsense.librealsense.RsContext;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityMainBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonListViewModel;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.network.service.DeviceService;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.service.WifiStateChangereceiverHelperService;
import de.welthungerhilfe.cgm.scanner.network.syncdata.MeasureNotification;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerPersonAdapter;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.SelectModeDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.StatisticsDialogfragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.DeviceCheckFragment;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.ui.views.TwoLineTextView;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;

import static de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity.KEY_LAST_DEVICE_CHECK_ISSUES;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends BaseActivity implements RecyclerPersonAdapter.OnPersonDetail, DateRangePickerDialog.Callback, SelectModeDialog.SetupmodeListner {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long REQUEST_DEVICE_CHECK_TIME = 1000 * 3600 * 48; //48h

    private static final int STD_TEST_DEACTIVE = 1;


    private PersonListViewModel viewModel;
    private FileLogRepository fileLogRepository;
    ActionBar actionBar;



    public void createData(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            runnable = () -> startActivity(new Intent(MainActivity.this, QRScanActivity.class).putExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.CONSENT_CAPTURED_REQUEST));
            addResultListener(PERMISSION_CAMERA, listener);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else {
            startActivity(new Intent(MainActivity.this, QRScanActivity.class).putExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.CONSENT_CAPTURED_REQUEST));
        }
    }

    RecyclerPersonAdapter adapterData;
    LinearLayoutManager lytManager;

    private ActionBarDrawerToggle mDrawerToggle;

    private DialogPlus sortDialog;

    private Runnable runnable;
    private SessionManager session;

    private ActivityMainBinding activityMainBinding;

    FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileLogRepository = FileLogRepository.getInstance(this);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        session = new SessionManager(MainActivity.this);
        LogFileUtils.startSession(MainActivity.this, session);
        LogFileUtils.logInfo(TAG, "CGM-Scanner " + AppController.getInstance().getAppVersion() + " started");
        viewModel = ViewModelProviders.of(this).get(PersonListViewModel.class);
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(this);
        if (session.getStdTestQrCode() != null) {
            if (QRScanActivity.isValidStdTestQrCode(session.getStdTestQrCode()) != QRScanActivity.STDTEST.VALID) {
                session.setStdTestQrCode(null);
                showStdTestButtonInMenu(false);
            }
        }

        observePersionList();
        if(session.getSelectedMode() == AppConstants.NO_MODE_SELECTED){
            if(session.getEnvironmentMode() == AppConstants.CGM_RST_MODE){
                SelectModeDialog selectModeDialog = new SelectModeDialog();
                selectModeDialog.show(getSupportFragmentManager(),"SelectModeDialog");
            }else if(session.getEnvironmentMode() == AppConstants.CGM_MODE){
                session.setSelectedMode(AppConstants.CGM_MODE);

            } else if(session.getEnvironmentMode() == AppConstants.RST_MODE){
                session.setSelectedMode(AppConstants.RST_MODE);

            }
            else if(session.getEnvironmentMode() == AppConstants.NO_MODE_SELECTED){
                session.setSelectedMode(AppConstants.CGM_MODE);
                session.setEnvironmentMode(AppConstants.CGM_MODE);
            }

        }

        setupSidemenu();
        setupActionBar();
        setupSortDialog();

        lytManager = new LinearLayoutManager(MainActivity.this);
        activityMainBinding.recyclerData.setLayoutManager(lytManager);
        activityMainBinding.recyclerData.setItemAnimator(new DefaultItemAnimator());
        activityMainBinding.recyclerData.setHasFixedSize(true);


        adapterData = new RecyclerPersonAdapter(this, activityMainBinding.recyclerData, viewModel);
        adapterData.setPersonDetailListener(this);
        activityMainBinding.recyclerData.setAdapter(adapterData);

        startService(new Intent(this, DeviceService.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!WifiStateChangereceiverHelperService.isServiceRunning) {
                startForegroundService(new Intent(this, WifiStateChangereceiverHelperService.class));
            }
        }
        activityMainBinding.rltSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearchBar();
            }
        });

        activityMainBinding.rltAddChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    runnable = () -> startActivity(new Intent(MainActivity.this, QRScanActivity.class).putExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.QR_SCAN_REQUEST));
                    addResultListener(PERMISSION_CAMERA, listener);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                } else {
                    startActivity(new Intent(MainActivity.this, QRScanActivity.class).putExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.QR_SCAN_REQUEST));
                }
            }
        });

        LogFileUtils.logInfo(TAG, "this is authtoken" + session.getAuthTokenWithBearer());


        if(session.getSessionError() > 50){
            sessionExpirePopUp();
            session.setSessionError(0);
        }
        RsContext.init(getApplicationContext());

        LogFileUtils.logInfoOffline("MainActivity","this is test message");


    }

    public void observePersionList(){
        final Observer<List<Person>> observer = list -> {
            Log.e("PersonRecycler", "Observer called");

            if (lytManager.getItemCount() == 0 && list != null && list.size() == 0) {
                activityMainBinding.lytNoPerson.setVisibility(View.VISIBLE);
            } else {
                activityMainBinding.lytNoPerson.setVisibility(View.GONE);
                adapterData.clear();
                adapterData.addPersons(list);
                Log.i(TAG,"this is inside list call");
            }
        };
        viewModel.getPersonListLiveData().observe(this, observer);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (adapterData != null)
            adapterData.notifyDataSetChanged();
    }

    private void setupSidemenu() {
        activityMainBinding.navMenu.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menuUploadManager:
                    startActivity(new Intent(MainActivity.this, UploadManagerActivity.class));
                    break;
                case R.id.menuDeviceCheck:
                    startActivity(new Intent(MainActivity.this, DeviceCheckActivity.class));
                    break;
                case R.id.menuTutorial:
                    Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                    intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
                    startActivity(intent);
                    break;
                case R.id.menuSettings:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    break;
                case R.id.menuLanguage:
                    startActivity(new Intent(MainActivity.this, LanguageSelectionActivity.class).putExtra("startFromHomeScreen",true));
                    break;
                case R.id.menuLogout:
                    showLogoutAlertDialog();
                    break;
                case R.id.menuQuitStdTest:
                    showConfirmDialog(R.string.std_test_deactivate, STD_TEST_DEACTIVE);
                    break;
                case R.id.menuSelectMode:
                    if (session.getStdTestQrCode() == null) {
                        SelectModeDialog selectModeDialog = new SelectModeDialog();
                        selectModeDialog.show(getSupportFragmentManager(),"SelectModeDialog");
                        break;
                    }



            }
            activityMainBinding.drawer.closeDrawers();
            return true;
        });
        View headerView = activityMainBinding.navMenu.getHeaderView(0);
        TextView txtUsername = headerView.findViewById(R.id.txtUsername);
        txtUsername.setText(session.getUserEmail());
    }

    public void logout() {
        session.setSigned(false);
        session.setSelectedMode(AppConstants.NO_MODE_SELECTED);
        session.setCurrentLogFilePath(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (WifiStateChangereceiverHelperService.isServiceRunning) {
                startForegroundService(new Intent(this, WifiStateChangereceiverHelperService.class)
                        .putExtra(AppConstants.STOP_SERVICE, true));
            }
        }
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void setupActionBar() {
        activityMainBinding.searchbar.setVisibility(View.GONE);
        setSupportActionBar(activityMainBinding.toolbar);
         actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_scans);
            invalidateOptionsMenu();
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, activityMainBinding.drawer, activityMainBinding.toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        activityMainBinding.drawer.addDrawerListener(mDrawerToggle);
    }

    private void openSearchBar() {
        activityMainBinding.searchbar.setVisibility(View.VISIBLE);
        setSupportActionBar(activityMainBinding.searchbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        invalidateOptionsMenu();

        activityMainBinding.searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {

                viewModel.setFilterQuery(query);
                return false;
            }
        });

        ImageView closeButton = activityMainBinding.searchview.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            activityMainBinding.searchview.setQuery("", false);

            viewModel.clearFilterOwn();
        });

        ImageView magImage = activityMainBinding.searchview.findViewById(R.id.search_mag_icon);
        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
    }

    private void setupSortDialog() {
        sortDialog = DialogPlus.newDialog(MainActivity.this)
                .setContentHolder(new ViewHolder(R.layout.dialog_sort))
                .setCancelable(true)
                .setInAnimation(R.anim.abc_fade_in)
                .setOutAnimation(R.anim.abc_fade_out)
                .create();

        if(session.getSelectedMode()==AppConstants.RST_MODE){
            sortDialog.findViewById(R.id.sortWasting).setVisibility(View.GONE);
            sortDialog.findViewById(R.id.sortStunting).setVisibility(View.GONE);
        }

        //set filter action
        sortDialog.findViewById(R.id.filterData).setOnClickListener(v -> {
            viewModel.setFilterOwn();
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.filterDate).setOnClickListener(v -> {
            doFilterByDate();
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.filterLocation).setOnClickListener(v -> {
            doFilterByLocation();
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.filterClear).setOnClickListener(v -> {
            viewModel.setFilterNo();
            sortDialog.dismiss();
        });

        //set sort action
        sortDialog.findViewById(R.id.sortDate).setOnClickListener(v -> {
            viewModel.setSortType(AppConstants.SORT_DATE);
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.sortLocation).setOnClickListener(v -> {
            adapterData.clear();
            Loc loc = GPS.getLastKnownLocation(getBaseContext());
            if (loc != null) {
                viewModel.setLocation(loc);
                viewModel.setSortType(AppConstants.SORT_LOCATION);
            }
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.sortWasting).setOnClickListener(v -> {
            viewModel.setSortType(AppConstants.SORT_WASTING);
            sortDialog.dismiss();
        });
        sortDialog.findViewById(R.id.sortStunting).setOnClickListener(v -> {
            viewModel.setSortType(AppConstants.SORT_STUNTING);
            sortDialog.dismiss();
        });

        viewModel.getPersonFilterLiveData().observe(this, filter -> {
            View view = sortDialog.getHolderView();

            //filter check icon
            boolean anyFilter = filter.isOwn() || filter.isDate() || filter.isLocation();
            view.findViewById(R.id.filterData).setSelected(filter.isOwn());
            view.findViewById(R.id.filterDate).setSelected(filter.isDate());
            view.findViewById(R.id.filterLocation).setSelected(filter.isLocation());
            view.findViewById(R.id.filterClear).setSelected(!anyFilter);

            //sort check icon
            int sort = filter.getSortType();
            view.findViewById(R.id.sortDate).setSelected(sort == AppConstants.SORT_DATE);
            view.findViewById(R.id.sortLocation).setSelected(sort == AppConstants.SORT_LOCATION);
            view.findViewById(R.id.sortWasting).setSelected(sort == AppConstants.SORT_WASTING);
            view.findViewById(R.id.sortStunting).setSelected(sort == AppConstants.SORT_STUNTING);

            //set date info
            if (filter.isDate()) {
                int diff = (int) Math.ceil((double) (filter.getToDate() - filter.getFromDate()) / 1000 / 3600 / 24);
                TwoLineTextView txtView = view.findViewById(R.id.filterDate);
                txtView.setText(2, getResources().getString(R.string.last_days, diff));
            } else {
                TwoLineTextView txtView = view.findViewById(R.id.filterDate);
                txtView.setText(2, getResources().getString(R.string.last_days).replace("%1$d", "x"));
            }

            //set location info
            if (filter.isLocation()) {
                TwoLineTextView txtView = sortDialog.getHolderView().findViewById(R.id.filterLocation);
                if (filter.getFromLOC() != null) {
                    txtView.setText(2, filter.getFromLOC().getAddress());
                } else {
                    txtView.setText(2, getString(R.string.last_location_error));
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void doFilterByDate() {
        DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
        dateRangePicker.setCallback(this);
        dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        dateRangePicker.show(getFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void doFilterByLocation() {
        Intent intent = new Intent(MainActivity.this, LocationSearchActivity.class);
        startActivityForResult(intent, PERMISSION_LOCATION);
    }

    private void openSort() {
        sortDialog.show();
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        Calendar start = selectedDate.getStartDate();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startDate = start.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(System.currentTimeMillis());
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        long endDate = end.getTimeInMillis();


        viewModel.setFilterDate(startDate, endDate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();

        if (activityMainBinding.searchbar.getVisibility() == View.VISIBLE) {
            menuInflater.inflate(R.menu.menu_search, menu);
        } else {
            menuInflater.inflate(R.menu.menu_tool, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.actionSearch:
                openSearchBar();
                break;
            case R.id.actionQr:
                StatisticsDialogfragment statisticsDialogfragment = new StatisticsDialogfragment();
                statisticsDialogfragment.show(getSupportFragmentManager(),"StatisticsDialogfragment");
                break;
            case R.id.actionFilter:
                openSort();
                break;
            case android.R.id.home:
                if (activityMainBinding.searchbar.getVisibility() == View.VISIBLE) {
                    setupActionBar();
                } else {
                    finish();
                }
                break;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent result) {
        super.onActivityResult(reqCode, resCode, result);
        if (reqCode == PERMISSION_LOCATION && resCode == Activity.RESULT_OK) {
            int radius = result.getIntExtra(AppConstants.EXTRA_RADIUS, 0);


            viewModel.setFilterLocation(session.getLocation(), radius);
        }
    }

    @Override
    public void onPersonDetail(Person person) {
        Intent intent = new Intent(MainActivity.this, CreateDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_QR, person.getQrcode());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MeasureNotification.dismissNotification(this);
        PersonRepository repository = PersonRepository.getInstance(this);
        if (repository.isUpdated()) {
            viewModel.setFilterOwn();
            viewModel.setSortType(AppConstants.SORT_DATE);
            repository.setUpdated(false);
        }
        SyncingWorkManager.startSyncingWithWorkManager(getApplicationContext());
        deviceCheckPopup();
        checkIfStdTestActive();
        setUpSelectedMode();
    }


    private final BaseActivity.ResultListener listener = new BaseActivity.ResultListener() {
        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (grantResults.length > 0) {
                runnable.run();
                runnable = null;
            }
        }
    };

    private void deviceCheckPopup() {
        long timestamp = LocalPersistency.getLong(this, DeviceCheckActivity.KEY_LAST_DEVICE_CHECK);
        if (System.currentTimeMillis() - timestamp > REQUEST_DEVICE_CHECK_TIME) {
            ConfirmDialog confirmDialog = new ConfirmDialog(this);
            confirmDialog.setMessage(getString(R.string.device_check_reminder));
            confirmDialog.setConfirmListener(result -> {
                if (result) {
                    startActivity(new Intent(MainActivity.this, DeviceCheckActivity.class));
                } else {
                    LocalPersistency.setString(this, KEY_LAST_DEVICE_CHECK_ISSUES, DeviceCheckFragment.IssueType.CHECK_REFUSED.toString());
                }
            });
            confirmDialog.show();
        }
    }

    private void showStdTestButtonInMenu(boolean visible) {
        activityMainBinding.navMenu.getMenu().findItem(R.id.menuQuitStdTest).setVisible(visible);
    }

    private void checkIfStdTestActive() {
        if (session.getStdTestQrCode() != null) {
            activityMainBinding.toolbar.setBackgroundResource(R.color.colorPink);
            showStdTestButtonInMenu(true);
        } else {
            activityMainBinding.toolbar.setBackgroundResource(R.color.colorPrimary);
            showStdTestButtonInMenu(false);
        }
    }

    private void showConfirmDialog(int message, int step) {
        try {
            ConfirmDialog confirmDialog = new ConfirmDialog(this);
            confirmDialog.setMessage(message);
            confirmDialog.setConfirmListener(result -> {
                if (result) {
                    session.setStdTestQrCode(null);
                    adapterData.notifyDataSetChanged();
                    checkIfStdTestActive();
                    firebaseAnalytics.logEvent(FirebaseService.STD_TEST_STOP, null);
                }
            });
            confirmDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showLogoutAlertDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.logout_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    public void sessionExpirePopUp(){
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Your session has been expired. Please logout and login again")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                    }
                }).show();
    }

    public void setUpSelectedMode(){
        Log.i(TAG,"this is inside setupSelectedmode");
        if(session.getSelectedMode() == AppConstants.NO_MODE_SELECTED){
            return;
        }
        String title = null;
        if(session.getSelectedMode() == AppConstants.CGM_MODE){
            title = getString(R.string.growth_monitor);
            activityMainBinding.navMenu.getMenu().findItem(R.id.menuScans).setIcon(R.drawable.menu_scans);
        }else {
            title = getString(R.string.rapid_survey_tool);
            activityMainBinding.navMenu.getMenu().findItem(R.id.menuScans).setIcon(R.drawable.ic_rapid_survey);

        }

        if(session.getEnvironmentMode() == AppConstants.CGM_RST_MODE) {
            activityMainBinding.navMenu.getMenu().findItem(R.id.menuSelectMode).setVisible(true);

        }else {
            activityMainBinding.navMenu.getMenu().findItem(R.id.menuSelectMode).setVisible(false);

        }
            activityMainBinding.navMenu.getMenu().findItem(R.id.menuScans).setTitle(title);
        actionBar.setTitle(title);
    }

    @Override
    public void changeSetupMode() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);

    }
}
