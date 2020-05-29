package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class SettingsRemoteConfigActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

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

    private RemoteConfig config;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_test_remoteconfig);

        ButterKnife.bind(this);

        config = new SessionManager(this).getRemoteConfig();

        setupActionBar();

        initUI();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.remote_config);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initUI() {

        txtSettingDebug.setText(String.valueOf(config.isDebug()));
        txtSettingSyncPeriod.setText(String.valueOf(config.getSync_period()));
        txtSettingAllowDelete.setText(String.valueOf(config.isAllow_delete()));
        txtSettingAllowEdit.setText(String.valueOf(config.isAllow_edit()));
        txtSettingEditTime.setText(String.valueOf(config.getTime_to_allow_editing()));
        txtSettingMeasureVisibility.setText(String.valueOf(config.isMeasure_visibility()));
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
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
}
