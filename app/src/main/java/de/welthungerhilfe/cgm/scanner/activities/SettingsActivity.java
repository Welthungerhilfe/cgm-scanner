package de.welthungerhilfe.cgm.scanner.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import de.welthungerhilfe.cgm.scanner.R;
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

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);

        setupActionBar();

        initUI();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Settings");
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
