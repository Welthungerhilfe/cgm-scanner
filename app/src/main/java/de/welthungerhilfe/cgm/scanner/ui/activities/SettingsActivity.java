/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;



import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivitySettingsBinding;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.database.BackupManager;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SettingsActivity extends BaseActivity {

    public static final String KEY_SHOW_DEPTH = "KEY_SHOW_DEPTH";
    public static final String KEY_UPLOAD_WIFI = "KEY_UPLOAD_WIFI";
    FirebaseAnalytics firebaseAnalytics;

    public void openPerformanceMeasurement(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsPerformanceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    public void openRemoteConfig(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsRemoteConfigActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private SessionManager session;
    private AlertDialog progressDialog;
    ActivitySettingsBinding activitySettingsBinding;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activitySettingsBinding = DataBindingUtil.setContentView(this,R.layout.activity_settings);
        session = new SessionManager(this);
        RemoteConfig config = session.getRemoteConfig();
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(this);
        setupActionBar();

        initUI();

        progressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_loading)
                .create();
    }

    private void setupActionBar() {
        setSupportActionBar(activitySettingsBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_settings);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initUI() {
        boolean devUser = session.getUserEmail().endsWith("@childgrowthmonitor.org");
        boolean devVersion = BuildConfig.VERSION_NAME.endsWith("dev");
        boolean showQA = BuildConfig.DEBUG || devUser || devVersion;
        activitySettingsBinding.testQAlayout.setVisibility(showQA ? View.VISIBLE : View.GONE);

        activitySettingsBinding.txtSettingUuid.setText(2, Utils.getAndroidID(getContentResolver()));
        activitySettingsBinding.showDepthData.setChecked(LocalPersistency.getBoolean(this, KEY_SHOW_DEPTH));
        activitySettingsBinding.showDepthData.setOnCheckedChangeListener((compoundButton, value) -> LocalPersistency.setBoolean(SettingsActivity.this, KEY_SHOW_DEPTH, value));

        activitySettingsBinding.uploadOverWifi.setChecked(LocalPersistency.getBoolean(this, KEY_UPLOAD_WIFI));
        activitySettingsBinding.uploadOverWifi.setOnCheckedChangeListener((compoundButton, value) -> LocalPersistency.setBoolean(SettingsActivity.this, KEY_UPLOAD_WIFI, value));

        activitySettingsBinding.txtSettingVersion.setText(2, Utils.getAppVersion(this));
        activitySettingsBinding.txtSettingAccount.setText(1, session.getUserEmail());
        activitySettingsBinding.txtSettingAzureAccount.setText(1, SyncingWorkManager.getAPI());

        String code = session.getLanguage();
        switch (code) {
            case AppConstants.LANG_ENGLISH:
                activitySettingsBinding.radioEnglish.setChecked(true);
                break;
            case AppConstants.LANG_GERMAN:
                activitySettingsBinding.radioGerman.setChecked(true);
                break;
            case AppConstants.LANG_HINDI:
                activitySettingsBinding.radioHindi.setChecked(true);
                break;
        }
        activitySettingsBinding.radioEnglish.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_ENGLISH));
        activitySettingsBinding.radioGerman.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_GERMAN));
        activitySettingsBinding.radioHindi.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_HINDI));

        if (session.getBackupTimestamp() == 0)
            activitySettingsBinding.txtSettingBackupDate.setText(2, getString(R.string.no_backups));
        else
            activitySettingsBinding.txtSettingBackupDate.setText(2, DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, session.getBackupTimestamp()));

        findViewById(R.id.btnBackupNow).setOnClickListener(view -> {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                addResultListener(PERMISSION_STORAGE, new ResultListener() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

                    }

                    @Override
                    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                        if (grantResults.length > 0) {
                            doBackup();
                        }
                    }
                });
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                return;
            }
            doBackup();
        });

        findViewById(R.id.btnContactSupport).setOnClickListener(view -> {
            ContactSupportDialog.show(this, null, null);
            firebaseAnalytics.logEvent("contact_support",null);
        });
    }

    private void changeLanguage(String code) {
        session.setLanguage(code);

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void doBackup() {
        progressDialog.show();
        long timestamp = System.currentTimeMillis();
        File dir = AppController.getInstance().getPublicAppDirectory(this);
        BackupManager.doBackup(this, dir, timestamp, () -> {
            session.setBackupTimestamp(timestamp);
            activitySettingsBinding.txtSettingBackupDate.setText(2, DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, timestamp));
            progressDialog.dismiss();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(session.getStdTestQrCode()!=null){
            activitySettingsBinding.toolbar.setBackgroundResource(R.color.colorPink);
        } else {
            activitySettingsBinding.toolbar.setBackgroundResource(R.color.colorPrimary);
        }
    }
}
