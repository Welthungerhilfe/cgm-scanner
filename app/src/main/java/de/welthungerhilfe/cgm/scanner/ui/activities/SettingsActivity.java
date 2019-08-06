package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SettingsActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txtSettingVersion)
    TextView txtSettingVersion;
    @BindView(R.id.txtSettingUuid)
    TextView txtSettingUuid;
    @BindView(R.id.txtSettingAccount)
    TextView txtSettingAccount;
    @BindView(R.id.txtSettingAzureAccount)
    TextView txtSettingAzureAccount;

    @BindView(R.id.radioEnglish)
    AppCompatRadioButton radioEnglish;
    @BindView(R.id.radioGerman)
    AppCompatRadioButton radioGerman;
    @BindView(R.id.radioHindi)
    AppCompatRadioButton radioHindi;

    @BindView(R.id.txtSettingDebug)
    TextView txtSettingDebug;
    @BindView(R.id.txtSettingSyncPeriod)
    TextView txtSettingSyncPeriod;
    @BindView(R.id.txtSettingAllowDelete)
    TextView txtSettingAllowDelete;
    @BindView(R.id.txtSettingAllowEdit)
    TextView txtSettingAllowEdit;
    @BindView(R.id.txtSettingEditTime)
    TextView txtSettingEditTime;
    @BindView(R.id.txtSettingMeasureVisibility)
    TextView txtSettingMeasureVisibility;

    @OnClick(R.id.lytLangEnglish)
    void onEnglish(LinearLayout lytLangEnglish) {
        changeLanguage("en");
    }

    @OnClick(R.id.lytLangGerman)
    void onGerman(LinearLayout lytLangGerman) {
        changeLanguage("de");
    }

    @OnClick(R.id.lytLangHindi)
    void onHindi(LinearLayout lytLangHindi) {
        changeLanguage("hi");
    }

    private SessionManager session;
    private RemoteConfig config;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);

        session = new SessionManager(this);
        config = session.getRemoteConfig();

        setupActionBar();

        initUI();
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

    private void initUI() {
        txtSettingUuid.setText(Utils.getAndroidID(getContentResolver()));

        try {
            txtSettingVersion.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            txtSettingVersion.setText("1.0");
        }

        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccounts();

        if (accounts.length > 0) {
            txtSettingAccount.setText(accounts[0].name);
        }

        txtSettingAzureAccount.setText(AppConstants.AZURE_ACCOUNT_NAME);

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

        txtSettingDebug.setText(String.valueOf(config.isDebug()));
        txtSettingSyncPeriod.setText(String.valueOf(config.getSync_period()));
        txtSettingAllowDelete.setText(String.valueOf(config.isAllow_delete()));
        txtSettingAllowEdit.setText(String.valueOf(config.isAllow_edit()));
        txtSettingEditTime.setText(String.valueOf(config.getTime_to_allow_editing()));
        txtSettingMeasureVisibility.setText(String.valueOf(config.isMeasure_visibility()));
    }

    private void changeLanguage(String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        session.setLanguage(code);

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void onBackPressed() {
        finish();
    }
}
