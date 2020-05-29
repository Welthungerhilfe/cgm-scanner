package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
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
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.UploadManagerViewModel;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerUploadAdapter;

public class UploadManagerActivity extends AppCompatActivity {
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

    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        setContentView(R.layout.activity_upload_manager);

        ButterKnife.bind(this);

        setupToolbar();

        UploadManagerViewModel viewModel = ViewModelProviders.of(this).get(UploadManagerViewModel.class);
        viewModel.getUploadStatusLiveData().observe(this, status -> {
            txtTotalSize.setText(String.format("%.2f MB", status.getTotal() / 1024 / 1024));
            txtUploadedSize.setText(String.format("%.2f MB", status.getUploaded() / 1024 / 1024));

            double progress = status.getUploaded() / status.getTotal() * 100;
            progressOverall.setProgress((int) progress);
        });
        viewModel.getSpeedLiveData().observe(this, speed -> {
            if (speed.doubleValue() != 0) {
                if (speed.doubleValue() / 1024 / 1024 > 1)
                    txtUploadSpeed.setText(String.format("%.2fMB/S", speed.doubleValue() / 1024 / 1024));
                else if (speed.doubleValue() / 1024 > 1)
                    txtUploadSpeed.setText(String.format("%dKB/S", (int) (speed.doubleValue() / 1024)));
                else
                    txtUploadSpeed.setText(String.format("%dB/S", (int) (speed.doubleValue())));

                UploadStatus status = viewModel.getUploadStatusLiveData().getValue();
                double expected = (status.getTotal() - status.getUploaded()) / speed.doubleValue();
                if (expected < 60)
                    txtExpectTime.setText("Less than 1 minute");
                else if (expected < 3600)
                    txtExpectTime.setText(String.format("%d minutes", (int) (expected / 60)));
                else if (expected < 3600 * 24)
                    txtExpectTime.setText(String.format("%d hours", (int) (expected / 60 / 60)));
                else
                    txtExpectTime.setText(String.format("%d hours", (int) (expected / 60 / 60 / 24)));
            }
        });
        viewModel.getExpectTimeLiveData().observe(this, time -> {
            double expected = time.doubleValue();
            if (expected > 60)
                txtUploadSpeed.setText(String.format("%d minutes", time.intValue() / 60));
            else if (expected > 60 * 60)
                txtUploadSpeed.setText(String.format("%d hours", time.intValue() / 60 / 60));
            else if (expected > 60 * 60 * 24)
                txtUploadSpeed.setText(String.format("%d days", time.intValue() / 60 / 60 / 24));
            else
                txtUploadSpeed.setText(String.format("%d seconds", time.intValue()));
        });

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

    public void onBackPressed() {
        finish();
    }
}
