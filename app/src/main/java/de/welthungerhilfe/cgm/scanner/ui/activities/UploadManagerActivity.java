package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
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

    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        setContentView(R.layout.activity_upload_manager);

        ButterKnife.bind(this);

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
        uploadedSize = status.getUploaded();
        progress = (int) (uploadedSize / totalSize * 100);

        txtTotalSize.setText(String.format("%.2f MB", totalSize / 1024 / 1024));
        txtUploadedSize.setText(String.format("%.2f MB", uploadedSize / 1024 / 1024));
        progressOverall.setProgress(progress);
    };

    @Override
    public void run() {
        while (running) {
            //update UI
            runOnUiThread(() -> {
                if (running) {
                    if (previousUploadSize > 0) {
                        double speed = uploadedSize - previousUploadSize;
                        if (speed / 1024 / 1024 > 1)
                            txtUploadSpeed.setText(String.format("%.2fMB/S", speed / 1024 / 1024));
                        else if (speed / 1024 > 1)
                            txtUploadSpeed.setText(String.format("%dKB/S", (int) (speed / 1024)));
                        else
                            txtUploadSpeed.setText(String.format("%dB/S", (int) (speed)));

                        if (speed / 1024 > 1) {
                            double time = (totalSize - uploadedSize) / speed;

                            if (time < 60)
                                txtExpectTime.setText("less than 1 minute");
                            else if (time < 60 * 60)
                                txtExpectTime.setText(String.format("%d minutes", (int) (time / 60)));
                            else if (time < 60 * 60 * 24)
                                txtExpectTime.setText(String.format("%d hours", (int) (time / 60 / 60)));
                            else
                                txtExpectTime.setText(String.format("%d days", (int) (time / 60 / 60 / 24)));
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
