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

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;

import androidx.databinding.DataBindingUtil;

import android.view.MenuItem;
import android.widget.CompoundButton;


import java.util.ArrayList;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityTestPerformanceBinding;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SettingsPerformanceActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, Runnable {

    public static final String KEY_TEST_PERFORMANCE = "KEY_TEST_PERFORMANCE";
    public static final String KEY_TEST_PERFORMANCE_COLOR_SIZE = "KEY_TEST_PERFORMANCE_COLOR_SIZE";
    public static final String KEY_TEST_PERFORMANCE_DEPTH_SIZE = "KEY_TEST_PERFORMANCE_DEPTH_SIZE";
    public static final String KEY_TEST_PERFORMANCE_COLOR_TIME = "KEY_TEST_PERFORMANCE_COLOR_TIME";
    public static final String KEY_TEST_PERFORMANCE_DEPTH_TIME = "KEY_TEST_PERFORMANCE_DEPTH_TIME";

    public static final String KEY_TEST_RESULT = "KEY_TEST_RESULT";
    public static final String KEY_TEST_RESULT_ID = "KEY_TEST_RESULT_ID";
    public static final String KEY_TEST_RESULT_SCAN = "KEY_TEST_RESULT_SCAN";
    public static final String KEY_TEST_RESULT_START = "KEY_TEST_RESULT_START";
    public static final String KEY_TEST_RESULT_END = "KEY_TEST_RESULT_END";
    public static final String KEY_TEST_RESULT_RECEIVE = "KEY_TEST_RESULT_RECEIVE";
    public static final String KEY_TEST_RESULT_AVERAGE = "KEY_TEST_RESULT_AVERAGE";

    private boolean running = false;

    ActivityTestPerformanceBinding activityTestPerformanceBinding;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityTestPerformanceBinding = DataBindingUtil.setContentView(this,R.layout.activity_test_performance);

        setupActionBar();

        initUI();
    }

    private void setupActionBar() {
        setSupportActionBar(activityTestPerformanceBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.performance_measurement);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initUI() {
        activityTestPerformanceBinding.profilePerformanceSwitch.setChecked(LocalPersistency.getBoolean(this, KEY_TEST_PERFORMANCE));
        activityTestPerformanceBinding.profileResultSwitch.setChecked(LocalPersistency.getBoolean(this, KEY_TEST_RESULT));

        activityTestPerformanceBinding.profilePerformanceSwitch.setOnCheckedChangeListener(this);
        activityTestPerformanceBinding.profileResultSwitch.setOnCheckedChangeListener(this);
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

        running = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        running = true;
        new Thread(this).start();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
        switch(compoundButton.getId()) {
            case R.id.profile_performance_switch:
                LocalPersistency.setBoolean(this, KEY_TEST_PERFORMANCE, value);
                if (!value) {
                    LocalPersistency.setLong(this, KEY_TEST_PERFORMANCE_COLOR_SIZE, 0);
                    LocalPersistency.setLong(this, KEY_TEST_PERFORMANCE_DEPTH_SIZE, 0);
                    LocalPersistency.setLong(this, KEY_TEST_PERFORMANCE_COLOR_TIME, 0);
                    LocalPersistency.setLong(this, KEY_TEST_PERFORMANCE_DEPTH_TIME, 0);
                }
                break;
            case R.id.profile_result_switch:
                LocalPersistency.setBoolean(this, KEY_TEST_RESULT, value);
                if (!value) {
                    LocalPersistency.setLong(this, KEY_TEST_RESULT_SCAN, 0);
                    LocalPersistency.setLong(this, KEY_TEST_RESULT_START, 0);
                    LocalPersistency.setLong(this, KEY_TEST_RESULT_END, 0);
                    LocalPersistency.setLong(this, KEY_TEST_RESULT_RECEIVE, 0);
                    LocalPersistency.setLongArray(this, KEY_TEST_RESULT_AVERAGE, new ArrayList<>());
                }
                break;
        }
    }

    @Override
    public void run() {
        while (running) {
            //get data
            long profileColorSize = LocalPersistency.getLong(this, KEY_TEST_PERFORMANCE_COLOR_SIZE);
            long profileDepthSize = LocalPersistency.getLong(this, KEY_TEST_PERFORMANCE_DEPTH_SIZE);
            long profileColorTime = LocalPersistency.getLong(this, KEY_TEST_PERFORMANCE_COLOR_TIME);
            long profileDepthTime = LocalPersistency.getLong(this, KEY_TEST_PERFORMANCE_DEPTH_TIME);
            long resultScan = LocalPersistency.getLong(this, KEY_TEST_RESULT_SCAN);
            long resultStart = LocalPersistency.getLong(this, KEY_TEST_RESULT_START);
            long resultEnd = LocalPersistency.getLong(this, KEY_TEST_RESULT_END);
            long resultReceive = LocalPersistency.getLong(this, KEY_TEST_RESULT_RECEIVE);
            long resultAverage = DataFormat.averageValue(LocalPersistency.getLongArray(this, KEY_TEST_RESULT_AVERAGE));

            //update UI
            runOnUiThread(() -> {
                if (running) {
                    activityTestPerformanceBinding.profilePerformanceColorSize.setValue(DataFormat.filesize(this, profileColorSize));
                    activityTestPerformanceBinding.profilePerformanceDepthSize.setValue(DataFormat.filesize(this, profileDepthSize));
                    activityTestPerformanceBinding.profilePerformanceColorTime.setValue(DataFormat.time(this, profileColorTime));
                    activityTestPerformanceBinding.profilePerformanceDepthTime.setValue(DataFormat.time(this, profileDepthTime));
                    activityTestPerformanceBinding.profileResultTimestampScan.setValue(DataFormat.timestamp(this, DataFormat.TimestampFormat.TIME, resultScan));
                    activityTestPerformanceBinding.profileResultTimestampStart.setValue(DataFormat.timestamp(this, DataFormat.TimestampFormat.TIME, resultStart));
                    activityTestPerformanceBinding.profileResultTimestampEnd.setValue(DataFormat.timestamp(this, DataFormat.TimestampFormat.TIME, resultEnd));
                    activityTestPerformanceBinding.profileResultTimestampReceive.setValue(DataFormat.timestamp(this, DataFormat.TimestampFormat.TIME, resultReceive));
                    activityTestPerformanceBinding.profileResultTimestampAverage.setValue(DataFormat.time(this, resultAverage));
                }
            });

            //wait a second before updating again
            Utils.sleep(1000);
        }
    }
}
