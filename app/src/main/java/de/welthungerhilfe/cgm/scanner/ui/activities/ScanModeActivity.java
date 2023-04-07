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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaActionSound;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityScanModeBinding;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.Audio;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARCoreCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AbstractARCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.Depthmap;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.BitmapHelper;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.ui.views.ScanModeView;
import de.welthungerhilfe.cgm.scanner.ui.views.ScanTypeView;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class ScanModeActivity extends BaseActivity implements View.OnClickListener, AbstractARCamera.Camera2DataListener, ScanTypeView.ScanTypeListener {

    private enum ArtifactType { CALIBRATION, DEPTH, RGB };

    ActivityScanModeBinding activityScanModeBinding;

    FirebaseAnalytics firebaseAnalytics;
    boolean scanCompleted = false;
    boolean scanStarted = false;


    public void scanStanding() {
        SCAN_MODE = AppConstants.SCAN_STANDING;

        activityScanModeBinding.lytScanLying.setActive(false);
        activityScanModeBinding.lytScanStanding.setActive(true);

        changeMode();
    }

    public void scanLying() {
        SCAN_MODE = AppConstants.SCAN_LYING;

        activityScanModeBinding.lytScanLying.setActive(true);
        activityScanModeBinding.lytScanStanding.setActive(false);

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

    public void completeScan(View view) {
        measure.setCreatedBy(session.getUserEmail());
        measure.setDate(AppController.getInstance().getUniversalTimestamp());
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
        measure.setStd_test_qr_code(session.getStdTestQrCode());
        if(session.getSelectedMode()==AppConstants.RST_MODE) {
            measure.setReceived_at(System.currentTimeMillis());
        }


        if (!heights.isEmpty()) {
            Collections.sort(heights, (a, b) -> (int) (1000 * (a - b)));
            measure.setHeight(heights.get(heights.size() / 2) * 100.0f);
        }

        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_RESULT)) {
            LocalPersistency.setString(this, SettingsPerformanceActivity.KEY_TEST_RESULT_ID, measure.getId());
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_SCAN, System.currentTimeMillis());
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_START, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_END, 0);
            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_RESULT_RECEIVE, 0);
        }
        progressDialog.show();

        new Thread(saveMeasure).start();
    }

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = AppConstants.SCAN_STANDING;
    public int SCAN_STEP = AppConstants.SCAN_PREVIEW;
    private boolean step1 = false, step2 = false, step3 = false;

    public Person person;
    public Measure measure;
    public Loc location;

    private MeasureRepository measureRepository;
    private PersonRepository personRepository;
    private FileLogRepository fileLogRepository;
    private HashMap<Integer, ArrayList<Float>> lightScores;
    private ArrayList<FileLog> files;
    private final Object lock = new Object();

    private SessionManager session;
    private ArrayList<Float> heights;

    private long mLastFeedbackTime;
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
    private ImageView mOutline;
    private float mOutlineAlpha = 1;
    AccuratePoseDetectorOptions options;
    PoseDetector poseDetector;

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
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(this);
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

        mNowTime = AppController.getInstance().getUniversalTimestamp();
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

        activityScanModeBinding = DataBindingUtil.setContentView(this,R.layout.activity_scan_mode);

        mTxtFeedback = findViewById(R.id.txtFeedback);
        mTitleView = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);

        findViewById(R.id.imgClose).setOnClickListener(this);

        mOutline = findViewById(R.id.scanOutline);
        ImageView colorCameraPreview = findViewById(R.id.colorCameraPreview);
        ImageView depthCameraPreview = findViewById(R.id.depthCameraPreview);
        GLSurfaceView glSurfaceView = findViewById(R.id.surfaceview);
        getCamera().onCreate(colorCameraPreview, depthCameraPreview, glSurfaceView);

        measureRepository = MeasureRepository.getInstance(this);
        personRepository = PersonRepository.getInstance(this);
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }

        activityScanModeBinding.scanType1.setListener(1, this);
        activityScanModeBinding.scanType2.setListener(2, this);
        activityScanModeBinding.scanType3.setListener(3, this);

         options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);

        if(age >= 730){
            scanStanding();
        }else{
            scanLying();
        }

        activityScanModeBinding.lytScanStanding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanStanding();
            }
        });

        activityScanModeBinding.lytScanLying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanLying();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        getCamera().onResume();
        getCamera().addListener(this);
        if(session.getStdTestQrCode()!=null){
            activityScanModeBinding.toolbar.setBackgroundResource(R.color.colorPink);
        } else {
            activityScanModeBinding.toolbar.setBackgroundResource(R.color.colorPrimary);
        }
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
        if(scanStarted && !scanCompleted){
            firebaseAnalytics.logEvent(FirebaseService.SCAN_CANCELED,null);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(activityScanModeBinding.toolbar);
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
            activityScanModeBinding.scanType1.setChildIcon(R.drawable.stand_front);
            activityScanModeBinding.scanType2.setChildIcon(R.drawable.stand_side);
            activityScanModeBinding.scanType3.setChildIcon(R.drawable.stand_back);
            getCamera().setPlaneMode(AbstractARCamera.PlaneMode.LOWEST);
        } else if (SCAN_MODE == AppConstants.SCAN_LYING) {
            activityScanModeBinding.scanType1.setChildIcon(R.drawable.lying_front);
            activityScanModeBinding.scanType2.setChildIcon(R.drawable.lying_side);
            activityScanModeBinding.scanType3.setChildIcon(R.drawable.lying_back);
            getCamera().setPlaneMode(AbstractARCamera.PlaneMode.VISIBLE);
        }
    }

    public void goToNextStep() {
        closeScan();

        if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT || SCAN_STEP == AppConstants.SCAN_LYING_FRONT) {
            activityScanModeBinding.scanType1.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_SIDE || SCAN_STEP == AppConstants.SCAN_LYING_SIDE) {
            activityScanModeBinding.scanType2.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_BACK || SCAN_STEP == AppConstants.SCAN_LYING_BACK) {
            activityScanModeBinding.scanType3.goToNextStep();
        }
        new Thread(getScanQuality).start();
    }

    private void showCompleteButton() {
        activityScanModeBinding.btnScanComplete.setVisibility(View.VISIBLE);
        activityScanModeBinding.btnScanComplete.requestFocus();

        int cx = (activityScanModeBinding.btnScanComplete.getLeft() + activityScanModeBinding.btnScanComplete.getRight()) / 2;
        int cy = (activityScanModeBinding.btnScanComplete.getTop() + activityScanModeBinding.btnScanComplete.getBottom()) / 2;

        int dx = Math.max(cx, activityScanModeBinding.btnScanComplete.getWidth() - cx);
        int dy = Math.max(cy, activityScanModeBinding.btnScanComplete.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        Animator animator = ViewAnimationUtils.createCircularReveal(activityScanModeBinding.btnScanComplete, cx, cy, 0, finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);
        animator.start();
    }

    private void resumeScan() {
        if (SCAN_STEP == AppConstants.SCAN_PREVIEW)
            return;

        mIsRecording = true;
        fab.setImageResource(R.drawable.stop);
        Audio.playShooterSound(this, MediaActionSound.START_VIDEO_RECORDING);
    }

    private void pauseScan() {
        mIsRecording = false;
        fab.setImageResource(R.drawable.recorder);
        Audio.playShooterSound(this, MediaActionSound.STOP_VIDEO_RECORDING);
    }

    private void openScan() {
        fab.setImageResource(R.drawable.recorder);
        activityScanModeBinding.lytScanner.setVisibility(View.VISIBLE);
        mTxtFeedback.setVisibility(View.GONE);
        mProgress = 0;
        progressBar.setProgress(0);
    }

    public void closeScan() {
        if (mIsRecording) {
            Audio.playShooterSound(this, MediaActionSound.STOP_VIDEO_RECORDING);
        }
        mIsRecording = false;
        activityScanModeBinding.lytScanner.setVisibility(View.GONE);
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
                    location.setAddress(GPS.getAddress(this, location));
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
        if (activityScanModeBinding.lytScanner.getVisibility() == View.VISIBLE) {
            activityScanModeBinding.lytScanner.setVisibility(View.GONE);
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

    private AbstractARCamera getCamera() {
        if (mCameraInstance == null) {
            AbstractARCamera.DepthPreviewMode depthMode;
            AbstractARCamera.PreviewSize previewSize = AbstractARCamera.PreviewSize.CLIPPED;
            if (LocalPersistency.getBoolean(this, SettingsActivity.KEY_SHOW_DEPTH)) {
               /* if (AREngineCamera.shouldUseAREngine()) {
                    depthMode = AbstractARCamera.DepthPreviewMode.CENTER;
                } else {*/
                    depthMode = AbstractARCamera.DepthPreviewMode.CENTER_LOW_POWER;
              //  }
            } else {
               /* if (AREngineCamera.shouldUseAREngine()) {
                    depthMode = AbstractARCamera.DepthPreviewMode.FOCUS;
                } else {*/
                    depthMode = AbstractARCamera.DepthPreviewMode.FOCUS_LOW_POWER;
               // }
            }

           /* if (AREngineCamera.shouldUseAREngine()) {
                mCameraInstance = new AREngineCamera(this, depthMode, previewSize);
            } else {*/
                mCameraInstance = new ARCoreCamera(this, depthMode, previewSize);
          //  }
        }
        return mCameraInstance;
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex) {
        Log.i(TAG,"this is value of bitmap & frameindex "+bitmap+" "+frameIndex);
        createPose(bitmap, frameIndex);
    }

    public void onPostColorDataReceived(Bitmap bitmap, int frameIndex, float poseScore, String poseCoordinates){

            long profile = System.currentTimeMillis();
            boolean hasCameraCalibration = mCameraInstance.hasCameraCalibration();
            String cameraCalibration = mCameraInstance.getCameraCalibration();
            LogFileUtils.logInfo("ARENGINE","arcore step3 calibration value "+hasCameraCalibration+" "+cameraCalibration);


        Runnable thread = () -> {
                try {

                    //write RGB data
                    String currentImgFilename = "rgb_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".jpg";
                    currentImgFilename = currentImgFilename.replace('/', '_');
                    File artifactFile = new File(mRgbSaveFolder, currentImgFilename);
                    BitmapHelper.writeBitmapToFile(bitmap, artifactFile);
                    onProcessArtifact(artifactFile, ArtifactType.RGB, 0, poseScore, poseCoordinates);

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
                                onProcessArtifact(artifactFile, ArtifactType.CALIBRATION, 0,0,null);

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

    @Override
    public void onDepthDataReceived(Depthmap depthmap, int frameIndex) {

        float height = 0;
        if (SCAN_MODE == AppConstants.SCAN_STANDING) {
            height = getCamera().getTargetHeight();
            if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
                if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT) {
                    heights.add(height);
                }
            }

            //realtime value
            /*String text = getString(R.string.label_height) + " : " + String.format(Locale.US,"~%.1fcm", height * 100.0f) +
            "\nNoise amount: " + String.format(Locale.US, "%.3f", getCamera().getDepthNoiseAmount());
            runOnUiThread(() -> mTitleView.setText(text));*/
        }
        onFeedbackUpdate();

        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
            long profile = System.currentTimeMillis();
            String depthmapFilename = "depth_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".depth";
            mNumberOfFilesWritten++;

            updateScanningProgress();
            onLightScore(getCamera().getLightIntensity());

            float finalHeight = height * 100.0f;
            Runnable thread = () -> {
                try {

                    //write depthmap
                    File artifactFile = new File(mDepthmapSaveFolder, depthmapFilename);
                    depthmap.save(artifactFile);
                    onProcessArtifact(artifactFile, ArtifactType.DEPTH, finalHeight,0,null);

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
        boolean childDetected = getCamera().getPersonCount() == 1;
        runOnUiThread(() -> {

            if ((SCAN_MODE == AppConstants.SCAN_LYING) && (SCAN_STEP != AppConstants.SCAN_LYING_FRONT)) {
                getCamera().setSkeletonMode(AbstractARCamera.SkeletonMode.OFF);
                setOutline(false);
            } else if (childDetected) {
                getCamera().setSkeletonMode(AbstractARCamera.SkeletonMode.OUTLINE);
                setFeedback(null);
                setOutline(false);
            } else {
                getCamera().setSkeletonMode(AbstractARCamera.SkeletonMode.OFF);
                setOutline(true);
            }

            if (mTxtFeedback.getVisibility() == View.GONE) {
                switch (light) {
                    case NORMAL:
                        setFeedback(null);
                        break;
                    case BRIGHT:
                        setFeedback(getString(R.string.score_light_bright));
                        break;
                    case DARK:
                        setFeedback(getString(R.string.score_light_dark));
                        break;
                }
            }
        });
    }

    private void setOutline(boolean visible) {
        float alpha = mOutlineAlpha * 0.9f + 0.1f;
        if (visible && (alpha > 0)) {
            mOutline.setAlpha(alpha);
            mOutline.setVisibility(View.VISIBLE);
        } else {
            mOutline.setVisibility(View.GONE);
        }
        mOutlineAlpha = visible ? alpha : -2;
    }

    private void setFeedback(String feedback) {
        //check if the feedback changed
        String lastFeedback = null;
        if (mTxtFeedback.getVisibility() == View.VISIBLE) {
            lastFeedback = mTxtFeedback.getText().toString();
        }
        boolean updated;
        if ((feedback != null) && (lastFeedback != null)) {
            updated = feedback.compareTo(lastFeedback) != 0;
        } else {
            updated = (feedback == null) != (lastFeedback == null);
        }

        //update feedback only if the previous feedback was visible at least for 1s
        if (updated) {
            if (System.currentTimeMillis() - mLastFeedbackTime > 1000) {
                if (feedback == null) {
                    mTxtFeedback.setVisibility(View.GONE);
                } else {
                    mTxtFeedback.setText(feedback);
                    mTxtFeedback.setVisibility(View.VISIBLE);
                }
                mLastFeedbackTime = System.currentTimeMillis();
            }
        }
    }

    private void onProcessArtifact(File artifactFile, ArtifactType type, float childHeight, float poseScore, String poseCordinates) {
        if (artifactFile.exists()) {
            FileLog log = new FileLog();

            //set type specific information
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

            //set information if child is detected (note: this is unsupported on ARCore devices and for lying children wrongly oriented)
            boolean childDetected = getCamera().getPersonCount() == 1;
            log.setChildDetected(childDetected);
            log.setChildHeight(childHeight);
            log.setPoseScore(poseScore);
            log.setPoseCoordinates(poseCordinates);

            //set metadata
            log.setPath(artifactFile.getPath());
            log.setHashValue(FileSystem.getMD5(artifactFile.getPath()));
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
            AppController.sleep(5);
        }
        LogFileUtils.logInfo(TAG, "Stop waiting on running threads");
    }

    private final Runnable getScanQuality = new Runnable() {
        private double lightScore = 0;
        private int scanStep = 0;

        @Override
        public void run() {
            synchronized (lock) {
                scanStep = SCAN_STEP;

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

            runOnUiThread(() -> {
                LogFileUtils.logInfo(TAG, "LightScore=" + lightScore);

                String issues = getString(R.string.scan_quality);
                issues = String.format("%s\n - " + getString(R.string.score_light) + "%d%%", issues, Math.round(lightScore * 100));

                if (scanStep == AppConstants.SCAN_STANDING_FRONT || scanStep == AppConstants.SCAN_LYING_FRONT) {
                    activityScanModeBinding.scanType1.finishStep(issues);
                    step1 = true;
                } else if (scanStep == AppConstants.SCAN_STANDING_SIDE || scanStep == AppConstants.SCAN_LYING_SIDE) {
                    activityScanModeBinding.scanType2.finishStep(issues);
                    step2 = true;

                } else if (scanStep == AppConstants.SCAN_STANDING_BACK || scanStep == AppConstants.SCAN_LYING_BACK) {
                    activityScanModeBinding.scanType3.finishStep(issues);
                    step3 = true;
                }

                if (step1 && step2 && step3) {
                    showCompleteButton();
                    firebaseAnalytics.logEvent(FirebaseService.SCAN_START,null);
                    scanStarted = true;
                }
            });
        }
    };

    private final Runnable saveMeasure = new Runnable() {
        @Override
        public void run() {
            //stop receiving new data
            getCamera().removeListener(this);

            //wait until everything is saved
            waitUntilFinished();

            //save metadata into DB
            synchronized (lock) {
                for (FileLog log : files) {
                    fileLogRepository.insertFileLog(log);
                }
                person.setLast_updated(System.currentTimeMillis());
                personRepository.updatePerson(person);
                measureRepository.insertMeasure(measure);
            }

            runOnUiThread(() -> {
                if (!UploadService.isInitialized()) {
                    startService(new Intent(getApplicationContext(), UploadService.class));
                } else {
                    UploadService.forceResume();
                }
                scanCompleted = true;
                firebaseAnalytics.logEvent(FirebaseService.SCAN_SUCCESSFUL,null);
                finish();
            });
        }
    };

    public void createPose(Bitmap bitmap, int frameIndex) {
        Log.i(TAG,"this is inside point 0");

        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
            Log.i(TAG,"this is inside point 1");
            if(bitmap==null){
                return;
            }
            String[] ans = new String[2];
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            poseDetector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<Pose>() {
                                @Override
                                public void onSuccess(Pose pose) {
                                    // Task completed successfully
                                    // ...
                                    Log.i(TAG,"this is inside point 3");

                                    String poseCoordinates = null;
                                    float poseTotal = 0.0f;
                                    float poseScore = 0.0f;

                                    List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();
                                    if (allPoseLandmarks != null && allPoseLandmarks.size() > 0) {
                                        poseCoordinates = "";
                                        for (int i = 0; i < allPoseLandmarks.size(); i++) {
                                            poseCoordinates = poseCoordinates+" "+ allPoseLandmarks.get(i).getInFrameLikelihood() + "," +allPoseLandmarks.get(i).getPosition().x+","+allPoseLandmarks.get(i).getPosition().y;
                                            poseTotal = poseTotal + allPoseLandmarks.get(i).getInFrameLikelihood();
                                        }
                                        poseScore = (float) (poseTotal / 33.0);
                                        Log.i("ScaneModeActivity", "this is value pf pose " + poseScore + " " + poseCoordinates);


                                        ans[0] = String.valueOf(poseScore);
                                        ans[1] = poseCoordinates;

                                    }
                                        if(ans[0]==null){
                                            ans[0]="0.0";
                                        }
                                    Log.i(TAG,"this is inside point 4" + ans[0] + " " + ans[1]);

                                    onPostColorDataReceived(bitmap, frameIndex, Float.parseFloat(ans[0]), ans[1]);

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                    Log.i(TAG,"this is inside point 5");

                                    onPostColorDataReceived(bitmap, frameIndex, 0.0f, null);

                                }
                            });
        }
    }

}
