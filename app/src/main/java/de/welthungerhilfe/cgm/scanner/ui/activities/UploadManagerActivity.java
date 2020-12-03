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

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.UploadManagerViewModel;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerUploadAdapter;

public class UploadManagerActivity extends AppCompatActivity implements Runnable {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recyclerScans)
    RecyclerView recyclerScans;
    @BindView(R.id.progressOverall)
    ProgressBar progressOverall;
    @BindView(R.id.txtTotalSize)
    TextView txtTotalSize;
    @BindView(R.id.txtUploadedSize)
    TextView txtUploadedSize;
    @BindView(R.id.txtUploadSpeed)
    TextView txtUploadSpeed;
    @BindView(R.id.txtExpectTime)
    TextView txtExpectTime;

    private int progress = 0;
    private double uploadedSize = 0;
    private double totalSize = 0;
    private boolean running = false;
    private UploadManagerViewModel viewModel;

    private double previousUploadSize = 0;
    private ArrayList<Double> secSpeedQueue = new ArrayList<>();
    private static final int SPEED_CALC_INTERVAL = 10; // calculate average upload speed of 10 secs

    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        setContentView(R.layout.activity_upload_manager);

        ButterKnife.bind(this);
        AndroidInjection.inject(this);

        setupToolbar();

        if (viewModel == null) {
            viewModel = ViewModelProviders.of(this).get(UploadManagerViewModel.class);
        }

        RecyclerUploadAdapter adapter = new RecyclerUploadAdapter(this);
        recyclerScans.setAdapter(adapter);
        recyclerScans.setLayoutManager(new LinearLayoutManager(this));

        MeasureRepository repository = MeasureRepository.getInstance(this);
        repository.getUploadMeasures().observe(this, measures -> {
            adapter.setData(measures);
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
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
        txtTotalSize.setText(String.format("%.2f MB", totalSize / 1024 / 1024));

        uploadedSize = status.getUploaded();
        txtUploadedSize.setText(String.format("%.2f MB", uploadedSize / 1024 / 1024));

        progress = (int) (uploadedSize / totalSize * 100);
        progressOverall.setProgress(progress);

        if (progress >= 100) {
            running = false;
            previousUploadSize = 0;
            txtUploadSpeed.setText("");
            txtExpectTime.setText(R.string.upload_completed);
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
                            txtUploadSpeed.setText(String.format(Locale.US, "%.2fMB/S", speed / 1024 / 1024));
                        else if (speed / 1024 > 1)
                            txtUploadSpeed.setText(String.format(Locale.US, "%dKB/S", (int) (speed / 1024)));
                        else
                            txtUploadSpeed.setText(String.format(Locale.US, "%dB/S", (int) (speed)));

                        if (speed / 1024 > 1) {
                            double time = (totalSize - uploadedSize) / speed;

                            if (time < 60)
                                txtExpectTime.setText(R.string.less_than_minute);
                            else if (time < 60 * 60)
                                txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.minutes), (int) (time / 60)));
                            else if (time < 60 * 60 * 24)
                                txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.hours), (int) (time / 60 / 60)));
                            else
                                txtExpectTime.setText(String.format(Locale.US, "%d " + getString(R.string.days), (int) (time / 60 / 60 / 24)));
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
