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

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.MenuItem;


import java.util.ArrayList;
import java.util.Locale;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityUploadManagerBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.UploadManagerViewModel;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerUploadAdapter;

public class UploadManagerActivity extends BaseActivity implements Runnable {


    private int progress = 0;
    private double uploadedSize = 0;
    private double totalSize = 0;
    private boolean running = false;
    private UploadManagerViewModel viewModel;

    private double previousUploadSize = 0;
    private ArrayList<Double> secSpeedQueue = new ArrayList<>();
    private static final int SPEED_CALC_INTERVAL = 10; // calculate average upload speed of 10 secs

    ActivityUploadManagerBinding activityUploadManagerBinding;

    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        activityUploadManagerBinding = DataBindingUtil.setContentView(this, R.layout.activity_upload_manager);

        setupToolbar();

        if (viewModel == null) {
            viewModel = ViewModelProviders.of(this).get(UploadManagerViewModel.class);
        }

        RecyclerUploadAdapter adapter = new RecyclerUploadAdapter(this);
        activityUploadManagerBinding.recyclerScans.setAdapter(adapter);
        activityUploadManagerBinding.recyclerScans.setLayoutManager(new LinearLayoutManager(this));

        MeasureRepository repository = MeasureRepository.getInstance(this);
        repository.getUploadMeasures().observe(this, measures -> {
            adapter.setData(measures);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(activityUploadManagerBinding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.menuUploadManager);
        }
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
    protected synchronized void onPause() {
        super.onPause();
        viewModel.getUploadStatusLiveData().removeObserver(uploadStatusObserver);

        running = false;
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        viewModel.getUploadStatusLiveData().observe(this, uploadStatusObserver);

        running = true;
        new Thread(this).start();
    }

    private Observer<UploadStatus> uploadStatusObserver = status -> {
        totalSize = status.getTotal();
        activityUploadManagerBinding.txtTotalSize.setText(String.format("%.2f MB", totalSize / 1024 / 1024));

        uploadedSize = status.getUploaded();
        activityUploadManagerBinding.txtUploadedSize.setText(String.format("%.2f MB", uploadedSize / 1024 / 1024));

        progress = (int) (uploadedSize / totalSize * 100);
        activityUploadManagerBinding.progressOverall.setProgress(progress);

        if (progress >= 100) {
            running = false;
            previousUploadSize = 0;
            activityUploadManagerBinding.txtUploadSpeed.setText("");
            activityUploadManagerBinding.txtExpectTime.setText(R.string.upload_completed);
        } else {
            running = true;
        }
    };

    @Override
    public void run() {
        while (running) {
            //update UI
            runOnUiThread(() -> {
                if (running) {
                    if (previousUploadSize > 0) {
                        double secSpeed = uploadedSize - previousUploadSize;
                        secSpeedQueue.add(secSpeed);
                        if (secSpeedQueue.size() > SPEED_CALC_INTERVAL) {
                            secSpeedQueue.remove(0);
                        }

                        double speed = 0;
                        for (double sp : secSpeedQueue) {
                            speed += sp;
                        }
                        speed /= secSpeedQueue.size();

                        if (speed / 1024 / 1024 > 1)
                            activityUploadManagerBinding.txtUploadSpeed.setText(String.format(Locale.US, "%.2fMB/S", speed / 1024 / 1024));
                        else if (speed / 1024 > 1)
                            activityUploadManagerBinding.txtUploadSpeed.setText(String.format(Locale.US, "%dKB/S", (int) (speed / 1024)));
                        else
                            activityUploadManagerBinding.txtUploadSpeed.setText(String.format(Locale.US, "%dB/S", (int) (speed)));

                        if (speed / 1024 > 1) {
                            double time = (totalSize - uploadedSize) / speed;

                            if (time < 60)
                                activityUploadManagerBinding.txtExpectTime.setText(R.string.less_than_minute);
                            else if (time < 60 * 60)
                                activityUploadManagerBinding.txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.minutes), (int) (time / 60)));
                            else if (time < 60 * 60 * 24)
                                activityUploadManagerBinding.txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.hours), (int) (time / 60 / 60)));
                            else
                                activityUploadManagerBinding.txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.days), (int) (time / 60 / 60 / 24)));
                        }
                    }
                    previousUploadSize = uploadedSize;
                }
            });

            //wait a second before next update
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
