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
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.MediaActionSound;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.camera.Depthmap;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.ui.views.ScanTypeView;
import de.welthungerhilfe.cgm.scanner.utils.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.camera.ARCoreCamera;
import de.welthungerhilfe.cgm.scanner.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.camera.AbstractARCamera;
import de.welthungerhilfe.cgm.scanner.camera.TangoCamera;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.camera.TangoUtils;
import de.welthungerhilfe.cgm.scanner.utils.IO;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ScanModeActivity extends BaseActivity implements View.OnClickListener, AbstractARCamera.Camera2DataListener, TangoCamera.TangoCameraListener, ScanTypeView.ScanTypeListener {

    private enum ArtifactType { CALIBRATION, DEPTH, RGB };

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.imgScanStanding)
    ImageView imgScanStanding;
    @BindView(R.id.imgScanStandingCheck)
    ImageView imgScanStandingCheck;
    @BindView(R.id.txtScanStanding)
    TextView txtScanStanding;

    @BindView(R.id.imgScanLying)
    ImageView imgScanLying;
    @BindView(R.id.imgScanLyingCheck)
    ImageView imgScanLyingCheck;
    @BindView(R.id.txtScanLying)
    TextView txtScanLying;

    @BindView(R.id.btnScanComplete)
    Button btnScanComplete;

    @BindView(R.id.lytSelectMode)
    LinearLayout lytSelectMode;

    @BindView(R.id.lytScanSteps)
    LinearLayout lytScanSteps;
    @BindView(R.id.lytScanner)
    LinearLayout lytScanner;

    @BindView(R.id.scanType1)
    ScanTypeView scanType1;
    @BindView(R.id.scanType2)
    ScanTypeView scanType2;
    @BindView(R.id.scanType3)
    ScanTypeView scanType3;

    @OnClick(R.id.lytScanStanding)
    void scanStanding() {
        SCAN_MODE = AppConstants.SCAN_STANDING;

        imgScanStanding.setImageResource(R.drawable.standing_active);
        imgScanStandingCheck.setImageResource(R.drawable.radio_active);
        txtScanStanding.setTextColor(getResources().getColor(R.color.colorBlack, getTheme()));

        imgScanLying.setImageResource(R.drawable.lying_inactive);
        imgScanLyingCheck.setImageResource(R.drawable.radio_inactive);
        txtScanLying.setTextColor(getResources().getColor(R.color.colorGreyDark, getTheme()));

        changeMode();
    }

    @OnClick(R.id.lytScanLying)
    void scanLying() {
        SCAN_MODE = AppConstants.SCAN_LYING;

        imgScanLying.setImageResource(R.drawable.lying_active);
        imgScanLyingCheck.setImageResource(R.drawable.radio_active);
        txtScanLying.setTextColor(getResources().getColor(R.color.colorBlack, getTheme()));

        imgScanStanding.setImageResource(R.drawable.standing_inactive);
        imgScanStandingCheck.setImageResource(R.drawable.radio_inactive);
        txtScanStanding.setTextColor(getResources().getColor(R.color.colorGreyDark, getTheme()));

        changeMode();
    }

    @Override
    public void onScan(int buttonId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_CAMERA);
        } else {
            if (SCAN_MODE == AppConstants.SCAN_STANDING) {

                switch (buttonId) {
                    case 1:
                        SCAN_STEP = AppConstants.SCAN_STANDING_FRONT;
                        mTitleView.setText(getString(R.string.front_scan) + " - " + getString(R.string.mode_standing));
                        break;
                    case 2:
                        SCAN_STEP = AppConstants.SCAN_STANDING_SIDE;
                        mTitleView.setText(getString(R.string.side_scan) + " - " + getString(R.string.mode_standing));
                        break;
                    case 3:
                        SCAN_STEP = AppConstants.SCAN_STANDING_BACK;
                        mTitleView.setText(getString(R.string.back_scan) + " - " + getString(R.string.mode_standing));
                        break;
                }
            } else if (SCAN_MODE == AppConstants.SCAN_LYING) {
                switch (buttonId) {
                    case 1:
                        SCAN_STEP = AppConstants.SCAN_LYING_FRONT;
                        mTitleView.setText(getString(R.string.front_scan) + " - " + getString(R.string.mode_lying));
                        break;
                    case 2:
                        SCAN_STEP = AppConstants.SCAN_LYING_SIDE;
                        mTitleView.setText(getString(R.string.side_scan) + " - " + getString(R.string.mode_lying));
                        break;
                    case 3:
                        SCAN_STEP = AppConstants.SCAN_LYING_BACK;
                        mTitleView.setText(getString(R.string.back_scan) + " - " + getString(R.string.mode_lying));
                        break;
                }
            }
            openScan();
        }
    }

    @Override
    public void onTutorial() {
        Intent intent = new Intent(ScanModeActivity.this, TutorialActivity.class);
        intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
        startActivity(intent);
    }

    @OnClick(R.id.btnScanComplete)
    void completeScan() {
        measure.setCreatedBy(session.getUserEmail());
        measure.setDate(Utils.getUniversalTimestamp());
        measure.setAge(age);
        measure.setType(AppConstants.VAL_MEASURE_AUTO);
        measure.setWeight(0.0f);
        measure.setHeight(0.0f);
        measure.setHeadCircumference(0.0f);
        measure.setMuac(0.0f);
        measure.setOedema(false);
        measure.setPersonId(person.getId());
        measure.setTimestamp(mNowTime);
        measure.setQrCode(person.getQrcode());
        measure.setSchema_version(CgmDatabase.version);
        measure.setScannedBy(session.getDevice());

        if (!heights.isEmpty()) {
            Collections.sort(heights, (a, b) -> (int) (1000 * (a - b)));
            measure.setHeight(heights.get(heights.size() / 2) * 100.0f);
        }

        progressDialog.show();

        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
            LocalPersistency.setString(this, SettingsPerformanceActivity.KEY_TEST_RESULT_ID, measure.getId());
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_SCAN, System.currentTimeMillis());
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_START, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_END, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_RECEIVE, 0);
        }
        new SaveMeasureTask(ScanModeActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = AppConstants.SCAN_STANDING;
    public int SCAN_STEP = AppConstants.SCAN_PREVIEW;
    private boolean step1 = false, step2 = false, step3 = false;

    public Person person;
    public Measure measure;
    public Loc location;

    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;
    private HashMap<Integer, ArrayList<Float>> lightScores;
    private ArrayList<FileLog> files;
    private final Object lock = new Object();

    private SessionManager session;
    private ArrayList<Float> heights;

    private TextView mTxtFeedback;
    private TextView mTitleView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    // variables for Pose and point clouds
    private int mNumberOfFilesWritten;

    private File mScanArtefactsOutputFolder;
    private File mDepthmapSaveFolder;
    private File mRgbSaveFolder;

    private boolean mIsRecording;
    private int mProgress;

    private long mNowTime;
    private String mNowTimeString;

    private long mColorSize;
    private long mColorTime;
    private long mDepthSize;
    private long mDepthTime;

    private long age = 0;

    private AlertDialog progressDialog;

    private ExecutorService executor;
    private int threadsCount = 0;
    private final Object threadsLock = new Object();

    private AbstractARCamera mCameraInstance;

    public void onStart() {
        super.onStart();

        mNumberOfFilesWritten = 0;
        mIsRecording = false;

        mColorSize = 0;
        mColorTime = 0;
        mDepthSize = 0;
        mDepthTime = 0;
        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_SIZE, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_SIZE, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_TIME, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_TIME, 0);
        }
    }

    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LogFileUtils.logException(throwable);
            Crashes.trackError(throwable);
            finish();
        });

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);

        if (person == null) {
            Toast.makeText(this, R.string.person_not_defined, Toast.LENGTH_LONG).show();
            finish();
        }

        executor = Executors.newFixedThreadPool(20);

        mNowTime = Utils.getUniversalTimestamp();
        mNowTimeString = String.valueOf(mNowTime);

        session = new SessionManager(this);
        heights = new ArrayList<>();

        age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;

        if (measure == null) {
            measure = new Measure();
            measure.setId(AppController.getInstance().getMeasureId());
            measure.setQrCode(person.getQrcode());
            measure.setCreatedBy(session.getUserEmail());
            measure.setAge(age);
            measure.setDate(System.currentTimeMillis());
            measure.setArtifact_synced(false);
            measure.setEnvironment(session.getEnvironment());
        }

        setContentView(R.layout.activity_scan_mode);

        ButterKnife.bind(this);

        mTxtFeedback = findViewById(R.id.txtFeedback);
        mTitleView = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);

        findViewById(R.id.imgClose).setOnClickListener(this);

        ImageView colorCameraPreview = findViewById(R.id.colorCameraPreview);
        ImageView depthCameraPreview = findViewById(R.id.depthCameraPreview);
        GLSurfaceView glSurfaceView = findViewById(R.id.surfaceview);
        getCamera().onCreate(colorCameraPreview, depthCameraPreview, glSurfaceView);

        measureRepository = MeasureRepository.getInstance(this);
        fileLogRepository = FileLogRepository.getInstance(this);
        lightScores = new HashMap<>();
        files = new ArrayList<>();

        setupToolbar();

        getCurrentLocation();

        setupScanArtifacts();

        progressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_loading)
                .create();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }

        scanType1.setListener(1, this);
        scanType2.setListener(2, this);
        scanType3.setListener(3, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCamera().onResume();
        getCamera().addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getCamera().removeListener(this);
        getCamera().onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_add_measure);
    }

    private void setupScanArtifacts() {
        File extFileDir = AppController.getInstance().getRootDirectory(this);

        LogFileUtils.logInfo(TAG, "Using directory " + extFileDir.getParent());
        mScanArtefactsOutputFolder = new File(extFileDir, person.getQrcode() + "/measurements/" + mNowTimeString + "/");
        mDepthmapSaveFolder = new File(mScanArtefactsOutputFolder, "depth");
        mRgbSaveFolder = new File(mScanArtefactsOutputFolder, "rgb");

        if (!mDepthmapSaveFolder.exists()) {
            boolean created = mDepthmapSaveFolder.mkdirs();
            if (created) {
                LogFileUtils.logInfo(TAG, "Folder: \"" + mDepthmapSaveFolder + "\" created\n");
            } else {
                LogFileUtils.logError(TAG, "Folder: \"" + mDepthmapSaveFolder + "\" could not be created!\n");
            }
        }

        if (!mRgbSaveFolder.exists()) {
            boolean created = mRgbSaveFolder.mkdirs();
            if (created) {
                LogFileUtils.logInfo(TAG, "Folder: \"" + mRgbSaveFolder + "\" created\n");
            } else {
                LogFileUtils.logError(TAG, "Folder: \"" + mRgbSaveFolder + "\" could not be created!\n");
            }
        }

        LogFileUtils.logInfo(TAG, "mDepthmapSaveFolder: " + mDepthmapSaveFolder);
        LogFileUtils.logInfo(TAG, "mRgbSaveFolder: " + mRgbSaveFolder);
    }

    private void updateScanningProgress() {
        float cloudsToFinishScan = (SCAN_STEP % 100 == 1 ? 24 : 8);
        float progressToAddFloat = 100.0f / cloudsToFinishScan;
        int progressToAdd = (int) progressToAddFloat;
        LogFileUtils.logInfo(TAG, "currentProgress=" + mProgress + ", progressToAdd=" + progressToAdd);
        if (mProgress + progressToAdd > 100) {
            mProgress = 100;
            runOnUiThread(() -> {
                fab.setImageResource(R.drawable.done);
                goToNextStep();
            });
        } else {
            mProgress = mProgress + progressToAdd;
        }
        progressBar.setProgress(mProgress);
    }

    private void changeMode() {
        if (SCAN_MODE == AppConstants.SCAN_STANDING) {
            scanType1.setChildIcon(R.drawable.stand_front_active);
            scanType2.setChildIcon(R.drawable.stand_side_active);
            scanType3.setChildIcon(R.drawable.stand_back_active);
            getCamera().setPlaneMode(AbstractARCamera.PlaneMode.LOWEST);
        } else if (SCAN_MODE == AppConstants.SCAN_LYING) {
            scanType1.setChildIcon(R.drawable.lying_front_active);
            scanType2.setChildIcon(R.drawable.lying_side_active);
            scanType3.setChildIcon(R.drawable.lying_back_active);
            getCamera().setPlaneMode(AbstractARCamera.PlaneMode.VISIBLE);
        }
    }

    public void goToNextStep() {
        closeScan();

        if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT || SCAN_STEP == AppConstants.SCAN_LYING_FRONT) {
            scanType1.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_SIDE || SCAN_STEP == AppConstants.SCAN_LYING_SIDE) {
            scanType2.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_BACK || SCAN_STEP == AppConstants.SCAN_LYING_BACK) {
            scanType3.goToNextStep();
        }
        getScanQuality(SCAN_STEP);
    }

    private void showCompleteButton() {
        btnScanComplete.setVisibility(View.VISIBLE);
        btnScanComplete.requestFocus();

        int cx = (btnScanComplete.getLeft() + btnScanComplete.getRight()) / 2;
        int cy = (btnScanComplete.getTop() + btnScanComplete.getBottom()) / 2;

        int dx = Math.max(cx, btnScanComplete.getWidth() - cx);
        int dy = Math.max(cy, btnScanComplete.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        Animator animator = ViewAnimationUtils.createCircularReveal(btnScanComplete, cx, cy, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);
        animator.start();
    }

    private void resumeScan() {
        if (SCAN_STEP == AppConstants.SCAN_PREVIEW)
            return;

        mIsRecording = true;
        fab.setImageResource(R.drawable.stop);
        Utils.playShooterSound(this, MediaActionSound.START_VIDEO_RECORDING);
    }

    private void pauseScan() {
        mIsRecording = false;
        fab.setImageResource(R.drawable.recorder);
        Utils.playShooterSound(this, MediaActionSound.STOP_VIDEO_RECORDING);
    }

    private void openScan() {
        getCamera().resetTrackingState();
        fab.setImageResource(R.drawable.recorder);
        lytScanner.setVisibility(View.VISIBLE);
        mTxtFeedback.setVisibility(View.GONE);
        mProgress = 0;
        progressBar.setProgress(0);
    }

    public void closeScan() {
        if (mIsRecording) {
            Utils.playShooterSound(this, MediaActionSound.STOP_VIDEO_RECORDING);
        }
        mIsRecording = false;
        lytScanner.setVisibility(View.GONE);
    }

    @SuppressLint("StaticFieldLeak")
    private void getScanQuality(int scanStep) {
        new AsyncTask<Void, Void, Boolean>() {
            private double lightScore = 0;

            @Override
            protected Boolean doInBackground(Void... voids) {
                synchronized (lock) {
                    //get average light score
                    if (lightScores.containsKey(SCAN_STEP)) {
                        for (Float value : lightScores.get(SCAN_STEP)) {
                            lightScore += value;
                        }
                        lightScore /= (float)lightScores.get(SCAN_STEP).size();
                    }

                    //too bright values are not over 100%
                    if (lightScore > 1) {
                        lightScore = 1.0f - (lightScore - 1.0f);
                    }
                }
                return true;
            }

            @SuppressLint("DefaultLocale")
            public void onPostExecute(Boolean results) {

                LogFileUtils.logInfo(TAG, "LightScore=" + lightScore);

                String issues = getString(R.string.scan_quality);
                issues = String.format("%s\n - " + getString(R.string.score_light) + "%d%%", issues, Math.round(lightScore * 100));

                if (scanStep == AppConstants.SCAN_STANDING_FRONT || scanStep == AppConstants.SCAN_LYING_FRONT) {
                    scanType1.finishStep(issues);
                    step1 = true;
                } else if (scanStep == AppConstants.SCAN_STANDING_SIDE || scanStep == AppConstants.SCAN_LYING_SIDE) {
                    scanType2.finishStep(issues);
                    step2 = true;

                } else if (scanStep == AppConstants.SCAN_STANDING_BACK || scanStep == AppConstants.SCAN_LYING_BACK) {
                    scanType3.finishStep(issues);
                    step3 = true;
                }

                if (step1 && step2 && step3) {
                    showCompleteButton();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location loc = null;

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else {
                List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (loc == null || l.getAccuracy() < loc.getAccuracy()) {
                        loc = l;
                    }
                }
                if (loc != null) {
                    location = new Loc();

                    location.setLatitude(loc.getLatitude());
                    location.setLongitude(loc.getLongitude());
                    location.setAddress(Utils.getAddress(this, location));
                    measure.setLocation(location);
                }
            }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION && grantResults.length > 0 && grantResults[0] >= 0) {
            getCurrentLocation();
        }
        if (requestCode == PERMISSION_CAMERA && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity.this, R.string.permission_camera, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (requestCode == PERMISSION_STORAGE && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity.this, R.string.storage_permission_needed, Toast.LENGTH_SHORT).show();
            finish();
        }
        setupScanArtifacts();
    }

    public void onBackPressed() {
        if (lytScanner.getVisibility() == View.VISIBLE) {
            lytScanner.setVisibility(View.GONE);
        } else {
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_scan_result:
                if (mIsRecording) {
                    if (mProgress >= 100) {
                        goToNextStep();
                    } else {
                        pauseScan();
                    }
                } else {
                    resumeScan();
                }
                break;
            case R.id.imgClose:
                closeScan();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class SaveMeasureTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;

        SaveMeasureTask(Activity act) {
            activity = act;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            //stop AR
            getCamera().onPause();

            //wait until everything is saved
            waitUntilFinished();

            //save metadata into DB
            synchronized (lock) {
                for (FileLog log : files) {
                    fileLogRepository.insertFileLog(log);
                }
                measureRepository.insertMeasure(measure);
            }

            runOnUiThread(() -> {
                if (!UploadService.isInitialized()) {
                    startService(new Intent(getApplicationContext(), UploadService.class));
                } else {
                    UploadService.forceResume();
                }
            });
            return null;
        }

        public void onPostExecute(Void result) {
            try {
                UploadService.forceResume();
                activity.finish();
            } catch (Exception e) {
                LogFileUtils.logException(e);
            }
        }
    }

    private AbstractARCamera getCamera() {
        if (mCameraInstance == null) {
            AbstractARCamera.DepthPreviewMode depthMode = AbstractARCamera.DepthPreviewMode.FOCUS;
            AbstractARCamera.PreviewSize previewSize = AbstractARCamera.PreviewSize.CLIPPED;
            if (LocalPersistency.getBoolean(this, SettingsActivity.KEY_SHOW_DEPTH)) {
                depthMode = AbstractARCamera.DepthPreviewMode.CENTER;
            }

            if (TangoUtils.isTangoSupported()) {
                mCameraInstance = new TangoCamera(this);
            } else if (AREngineCamera.shouldUseAREngine()) {
                mCameraInstance = new AREngineCamera(this, depthMode, previewSize);
            } else {
                mCameraInstance = new ARCoreCamera(this, depthMode, previewSize);
            }
        }
        return mCameraInstance;
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex) {
        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {

            long profile = System.currentTimeMillis();
            boolean hasCameraCalibration = mCameraInstance.hasCameraCalibration();
            String cameraCalibration = mCameraInstance.getCameraCalibration();

            Runnable thread = () -> {
                try {

                    //write RGB data
                    String currentImgFilename = "rgb_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".jpg";
                    currentImgFilename = currentImgFilename.replace('/', '_');
                    File artifactFile = new File(mRgbSaveFolder, currentImgFilename);
                    BitmapUtils.writeBitmapToFile(bitmap, artifactFile);
                    onProcessArtifact(artifactFile, ArtifactType.RGB);

                    //save RGB metadata
                    if (artifactFile.exists()) {
                        mColorSize += artifactFile.length();
                        mColorTime += System.currentTimeMillis() - profile;
                        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_SIZE, mColorSize);
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_TIME, mColorTime);
                        }
                    }

                    //save calibration data
                    artifactFile = new File(mScanArtefactsOutputFolder, "camera_calibration.txt");
                    if (!artifactFile.exists()) {
                        if (hasCameraCalibration) {
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(artifactFile.getAbsolutePath());
                                fileOutputStream.write(cameraCalibration.getBytes());
                                fileOutputStream.flush();
                                fileOutputStream.close();
                                onProcessArtifact(artifactFile, ArtifactType.CALIBRATION);

                            } catch (Exception e) {
                                LogFileUtils.logException(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LogFileUtils.logException(e);
                }

                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }

    @Override
    public void onDepthDataReceived(Image image, float[] position, float[] rotation, int frameIndex) {

        if (SCAN_MODE == AppConstants.SCAN_STANDING) {
            float height = getCamera().getTargetHeight();
            if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
                if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT) {
                    heights.add(height);
                }
            }

            //realtime value
            /*runOnUiThread(() -> {
                String text = getString(R.string.label_height) + " : " + String.format("~%dcm", (int)(height * 100));
                mTitleView.setText(text);
            });*/
        }
        onFeedbackUpdate();

        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
            long profile = System.currentTimeMillis();
            Depthmap depthmap = getCamera().extractDepthmap(image, position, rotation, mCameraInstance instanceof AREngineCamera);
            String depthmapFilename = "depth_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".depth";
            mNumberOfFilesWritten++;

            updateScanningProgress();
            onLightScore(getCamera().getLightIntensity());

            Runnable thread = () -> {
                try {

                    //write depthmap
                    File artifactFile = new File(mDepthmapSaveFolder, depthmapFilename);
                    depthmap.save(artifactFile);
                    onProcessArtifact(artifactFile, ArtifactType.DEPTH);

                    //profile process
                    if (artifactFile.exists()) {
                        mDepthSize += artifactFile.length();
                        mDepthTime += System.currentTimeMillis() - profile;
                        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_SIZE, mDepthSize);
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_TIME, mDepthTime);
                        }
                    }
                } catch (Exception e) {
                    LogFileUtils.logException(e);
                }

                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }

    @Override
    public void onTangoColorData(TangoImageBuffer tangoImageBuffer) {
        if (!mIsRecording) {
            return;
        }

        String currentImgFilename = "rgb_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + String.format(Locale.US, "%f", tangoImageBuffer.timestamp) + ".jpg";
        File artifactFile = new File(mRgbSaveFolder.getPath(), currentImgFilename);

        Runnable thread = () -> {
            long profile = System.currentTimeMillis();
            TangoUtils.writeImageToFile(tangoImageBuffer, artifactFile);
            onProcessArtifact(artifactFile, ArtifactType.RGB);

            if (artifactFile.exists()) {
                mColorSize += artifactFile.length();
                mColorTime += System.currentTimeMillis() - profile;
                if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                    LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_SIZE, mColorSize);
                    LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_TIME, mColorTime);
                }
            }
            onThreadChange(-1);
        };
        onThreadChange(1);
        executor.execute(thread);
    }

    @Override
    public void onTangoDepthData(TangoPointCloudData pointCloudData, float[] position, float[] rotation, TangoCameraIntrinsics[] calibration) {

        onFeedbackUpdate();
        boolean hasCameraCalibration = mCameraInstance.hasCameraCalibration();
        String cameraCalibration = mCameraInstance.getCameraCalibration();

        // Saving the frame or not, depending on the current mode.
        if (mIsRecording) {
            long profile = System.currentTimeMillis();
            int numPoints = pointCloudData.numPoints;
            double timestamp = pointCloudData.timestamp;
            ByteBuffer buffer = ByteBuffer.allocate(pointCloudData.numPoints * 4 * 4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.asFloatBuffer().put(pointCloudData.points);

            updateScanningProgress();
            onLightScore(getCamera().getLightIntensity());

            String depthmapFilename = "depth_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP +
                    "_" + mNumberOfFilesWritten++ + "_" + String.format(Locale.US, "%f", pointCloudData.timestamp) + ".depth";

            Runnable thread = () -> {

                //write depthmap
                Depthmap depthmap = TangoUtils.extractDepthmap(buffer, numPoints, position, rotation, timestamp, calibration[1]);
                File artifactFile = new File(mDepthmapSaveFolder, depthmapFilename);
                depthmap.save(artifactFile);
                onProcessArtifact(artifactFile, ArtifactType.DEPTH);

                //profile process
                if (artifactFile.exists()) {
                    mDepthSize += artifactFile.length();
                    mDepthTime += System.currentTimeMillis() - profile;
                    if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                        LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_SIZE, mDepthSize);
                        LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_TIME, mDepthTime);
                    }
                }

                //save calibration data
                artifactFile = new File(mScanArtefactsOutputFolder, "camera_calibration.txt");
                if (!artifactFile.exists()) {
                    if (hasCameraCalibration) {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(artifactFile.getAbsolutePath());
                            fileOutputStream.write(cameraCalibration.getBytes());
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            onProcessArtifact(artifactFile, ArtifactType.CALIBRATION);

                        } catch (Exception e) {
                            LogFileUtils.logException(e);
                        }
                    }
                }
                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }

    private void onLightScore(float score) {
        synchronized (lock) {
            if (!lightScores.containsKey(SCAN_STEP)) {
                lightScores.put(SCAN_STEP, new ArrayList<>());
            }
            lightScores.get(SCAN_STEP).add(score);
        }
    }

    private void onFeedbackUpdate() {
        AbstractARCamera.LightConditions light = getCamera().getLightConditionState();
        AbstractARCamera.TrackingState tracking = getCamera().getTrackingState();
        float distance = getCamera().getTargetDistance();
        runOnUiThread(() -> {

            if (SCAN_MODE == AppConstants.SCAN_STANDING) {
                switch (tracking) {
                    case INIT:
                    case TRACKED:
                        mTxtFeedback.setVisibility(View.GONE);
                        break;
                    case LOST:
                        mTxtFeedback.setText("Child not fully visible");
                        mTxtFeedback.setVisibility(View.VISIBLE);
                        break;
                }
            } else {
                mTxtFeedback.setVisibility(View.GONE);
            }

            if (mTxtFeedback.getVisibility() == View.GONE) {
                switch (light) {
                    case NORMAL:
                        mTxtFeedback.setVisibility(View.GONE);
                        break;
                    case BRIGHT:
                        mTxtFeedback.setText(R.string.score_light_bright);
                        mTxtFeedback.setVisibility(View.VISIBLE);
                        break;
                    case DARK:
                        mTxtFeedback.setText(R.string.score_light_dark);
                        mTxtFeedback.setVisibility(View.VISIBLE);
                        break;
                }
            }

            if ((mTxtFeedback.getVisibility() == View.GONE) && (distance != 0)) {
                if (distance < 0.7) {
                    mTxtFeedback.setText(R.string.score_distance_close);
                    mTxtFeedback.setVisibility(View.VISIBLE);
                } else if (distance > 1.5f) {
                    mTxtFeedback.setText(R.string.score_distance_far);
                    mTxtFeedback.setVisibility(View.VISIBLE);
                } else {
                    mTxtFeedback.setVisibility(View.GONE);
                }
            }
        });
    }

    private void onProcessArtifact(File artifactFile, ArtifactType type) {
        if (artifactFile.exists()) {
            FileLog log = new FileLog();
            switch (type) {
                case CALIBRATION:
                    log.setStep(0);
                    log.setId(AppController.getInstance().getArtifactId("camera-calibration", mNowTime));
                    log.setType("calibration");
                    break;
                case DEPTH:
                    log.setStep(SCAN_STEP);
                    log.setId(AppController.getInstance().getArtifactId("scan-depth", mNowTime));
                    log.setType("depth");
                    break;
                case RGB:
                    log.setStep(SCAN_STEP);
                    log.setId(AppController.getInstance().getArtifactId("scan-rgb", mNowTime));
                    log.setType("rgb");
                    break;
            }
            log.setPath(artifactFile.getPath());
            log.setHashValue(IO.getMD5(artifactFile.getPath()));
            log.setFileSize(artifactFile.length());
            log.setUploadDate(0);
            log.setDeleted(false);
            log.setQrCode(person.getQrcode());
            log.setCreateDate(mNowTime);
            log.setCreatedBy(session.getUserEmail());
            log.setAge(age);
            log.setSchema_version(CgmDatabase.version);
            log.setMeasureId(measure.getId());
            log.setEnvironment(session.getEnvironment());
            synchronized (lock) {
                files.add(log);
            }
        }
    }

    private void onThreadChange(int diff) {
        synchronized (threadsLock) {
            threadsCount += diff;
            if (threadsCount == 0) {
                LogFileUtils.logInfo(TAG, "The last thread finished");
            } else {
                LogFileUtils.logInfo(TAG, "Amount of threads : " + threadsCount);
            }
        }
    }

    private void waitUntilFinished() {
        LogFileUtils.logInfo(TAG, "Start waiting on running threads");
        while (true) {
            synchronized (threadsLock) {
                if (threadsCount == 0) {
                    break;
                }
            }
            Utils.sleep(5);
        }
        LogFileUtils.logInfo(TAG, "Stop waiting on running threads");
    }
}
