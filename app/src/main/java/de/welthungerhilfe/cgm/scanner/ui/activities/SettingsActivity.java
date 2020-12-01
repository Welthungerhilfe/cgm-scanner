package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.android.AndroidInjection;
import dagger.android.support.AndroidSupportInjection;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.repository.BackupManager;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.camera.TangoUtils;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.LanguageRadioView;
import de.welthungerhilfe.cgm.scanner.ui.views.ToggleView;
import de.welthungerhilfe.cgm.scanner.ui.views.TwoLineTextView;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import retrofit2.Retrofit;

public class SettingsActivity extends BaseActivity {

    private final int PERMISSION_STORAGE = 0x0003;

    public static final String KEY_SHOW_DEPTH = "KEY_SHOW_DEPTH";
    public static final String KEY_UPLOAD_WIFI = "KEY_UPLOAD_WIFI";

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txtSettingVersion)
    TwoLineTextView txtSettingVersion;
    @BindView(R.id.txtSettingUuid)
    TwoLineTextView txtSettingUuid;
    @BindView(R.id.txtSettingAccount)
    TwoLineTextView txtSettingAccount;
    @BindView(R.id.txtSettingAzureAccount)
    TwoLineTextView txtSettingAzureAccount;

    @BindView(R.id.radioEnglish)
    LanguageRadioView radioEnglish;
    @BindView(R.id.radioGerman)
    LanguageRadioView radioGerman;
    @BindView(R.id.radioHindi)
    LanguageRadioView radioHindi;

    @BindView(R.id.txtSettingBackupDate)
    TwoLineTextView txtSettingBackupDate;

    @BindView(R.id.upload_over_wifi)
    ToggleView switchUploadOverWifi;

    @BindView(R.id.testQAlayout)
    LinearLayout layoutTestQA;
    @BindView(R.id.show_depth_data)
    ToggleView switchShowDepth;

    @Inject
    Retrofit retrofit;

    @OnClick(R.id.submenu_performance_measurement)
    void openPerformanceMeasurement(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsPerformanceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @OnClick(R.id.submenu_remote_config)
    void openRemoteConfig(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsRemoteConfigActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private SessionManager session;
    private AlertDialog progressDialog;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);
        AndroidInjection.inject(this);
        session = new SessionManager(this);
        RemoteConfig config = session.getRemoteConfig();

        setupActionBar();

        initUI();

        progressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_loading)
                .create();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_settings);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initUI() {
        boolean devBackend = false; //TODO:detect backend name
        boolean devVersion = BuildConfig.VERSION_NAME.endsWith("dev");
        boolean showQA = BuildConfig.DEBUG || devBackend || devVersion;
        layoutTestQA.setVisibility(showQA ? View.VISIBLE : View.GONE);

        txtSettingUuid.setText(2, Utils.getAndroidID(getContentResolver()));
        if (TangoUtils.isTangoSupported()) {
            switchShowDepth.setVisibility(View.GONE);
        } else {
            switchShowDepth.setChecked(LocalPersistency.getBoolean(this, KEY_SHOW_DEPTH));
            switchShowDepth.setOnCheckedChangeListener((compoundButton, value) -> LocalPersistency.setBoolean(SettingsActivity.this, KEY_SHOW_DEPTH, value));
        }

        switchUploadOverWifi.setChecked(LocalPersistency.getBoolean(this, KEY_UPLOAD_WIFI));
        switchUploadOverWifi.setOnCheckedChangeListener((compoundButton, value) -> LocalPersistency.setBoolean(SettingsActivity.this, KEY_UPLOAD_WIFI, value));

        txtSettingVersion.setText(2, Utils.getAppVersion(this));

        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccounts();

        if (accounts.length > 0) {
            txtSettingAccount.setText(1, accounts[0].name);
        }

        txtSettingAzureAccount.setText(1, "");//TODO:detect backend name

        String code = session.getLanguage();
        switch (code) {
            case AppConstants.LANG_ENGLISH:
                radioEnglish.setChecked(true);
                break;
            case AppConstants.LANG_GERMAN:
                radioGerman.setChecked(true);
                break;
            case AppConstants.LANG_HINDI:
                radioHindi.setChecked(true);
                break;
        }
        radioEnglish.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_ENGLISH));
        radioGerman.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_GERMAN));
        radioHindi.setOnCheckedChangeListener((compoundButton, b) -> changeLanguage(AppConstants.LANG_HINDI));

        if (session.getBackupTimestamp() == 0) txtSettingBackupDate.setText(2, getString(R.string.no_backups));
        else txtSettingBackupDate.setText(2, DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, session.getBackupTimestamp()));

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
        });
    }

    private void changeLanguage(String code) {
        session.setLanguage(code);

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void doBackup() {
        progressDialog.show();
        long timestamp = System.currentTimeMillis();
        File dir = AppController.getInstance().getRootDirectory();
        BackupManager.doBackup(this, dir, timestamp, retrofit,() -> {
            session.setBackupTimestamp(timestamp);
            txtSettingBackupDate.setText(2, DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, timestamp));
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
}
