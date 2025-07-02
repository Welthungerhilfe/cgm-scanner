package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaActionSound;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.DeviceListener;
//import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.Utils;
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
import de.welthungerhilfe.cgm.scanner.hardware.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARRealSenseCamera1;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARRealSenseCamera2;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AbstractARCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AbstractIntelARCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.Depthmap;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.BitmapHelper;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.ui.views.ScanTypeView;

public class ScanModeActivity2 extends BaseActivity implements View.OnClickListener, AbstractIntelARCamera.Camera2DataListener, ScanTypeView.ScanTypeListener, SensorEventListener {

    private enum ArtifactType {CALIBRATION, DEPTH, RGB};

    ActivityScanModeBinding activityScanModeBinding;

    FirebaseAnalytics firebaseAnalytics;
    boolean scanCompleted = false;
    boolean scanStarted = false;
    String cameraCalibration;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float[] accelerometerReading = new float[3];

    double lastUpdatedAngle = 0;

    double angle =0;

    private boolean isRetake = false;

    double verticalAngle=0, horizontalAngle=0;

    int childCount = 0;

    float mOutlineAlpha = 0;

    boolean finishCreateDataActivity = false;

    public void scanStanding() {
        SCAN_MODE = AppConstants.SCAN_STANDING;

        if(files!=null && files.size()>0) {
            showChangeModeConfirmation("This will discard standing data. Are you sure you want to continue?",true);
        }else {
            activityScanModeBinding.lytScanLying.setActive(false);
            activityScanModeBinding.lytScanStanding.setActive(true);
            changeMode();
        }
    }

    public void scanLying() {
        SCAN_MODE = AppConstants.SCAN_LYING;

        if(files!=null && files.size()>0) {
            showChangeModeConfirmation("This will discard standing data. Are you sure you want to continue?",false);
        }else {
            activityScanModeBinding.lytScanLying.setActive(true);
            activityScanModeBinding.lytScanStanding.setActive(false);
            changeMode();
        }
    }

    private void showChangeModeConfirmation(String message, boolean isStanding) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> restartActivity(isStanding))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void restartActivity(boolean isStanding){
        Intent intent = new Intent(this, ScanModeActivity2.class);
        intent.putExtra(AppConstants.EXTRA_SCAN_MODE, isStanding);
        intent.putExtra(AppConstants.EXTRA_PERSON, person);
        intent.putExtra(AppConstants.EXTRA_MEASURE, measure);

        finish();
        startActivity(intent);
    }

    public void onScan(int buttonId, boolean isRetake) {
        this.isRetake = isRetake;
        if(isRetake){
            retakeFiles.clear();
            retakeFiles = new ArrayList<>();
        }
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
                        SCAN_STEP = AppConstants.SCAN_STANDING_SIDE_LEFT;
                        mTitleView.setText(getString(R.string.side_scan) + " - " + getString(R.string.mode_standing));
                        break;
                    case 3:
                        SCAN_STEP = AppConstants.SCAN_STANDING_BACK;
                        mTitleView.setText(getString(R.string.back_scan) + " - " + getString(R.string.mode_standing));
                        break;
                    case 4:
                        SCAN_STEP = AppConstants.SCAN_STANDING_SIDE_RIGHT;
                        mTitleView.setText(getString(R.string.right_scan) + " - " + getString(R.string.mode_standing));

                }
            } else if (SCAN_MODE == AppConstants.SCAN_LYING) {
                switch (buttonId) {
                    case 1:
                        SCAN_STEP = AppConstants.SCAN_LYING_FRONT;
                        mTitleView.setText(getString(R.string.front_scan) + " - " + getString(R.string.mode_lying));
                        break;
                    case 2:
                        SCAN_STEP = AppConstants.SCAN_LYING_SIDE_LEFT;
                        mTitleView.setText(getString(R.string.side_scan) + " - " + getString(R.string.mode_lying));
                        break;
                    case 3:
                        SCAN_STEP = AppConstants.SCAN_LYING_BACK;
                        mTitleView.setText(getString(R.string.back_scan) + " - " + getString(R.string.mode_lying));
                        break;
                    case 4:
                        SCAN_STEP = AppConstants.SCAN_LYING_SIDE_RIGHT;
                        mTitleView.setText(getString(R.string.right_scan) + " - " + getString(R.string.mode_lying));
                        break;
                }
            }
            openScan();
        }
    }

    void updateRetakeFileLog(int scanStep){
        ArrayList<FileLog> toRemove = new ArrayList<>();
        for (FileLog file : files) {
            if (file.getStep() == scanStep) {
                toRemove.add(file);
            }
        }
        files.removeAll(toRemove);
        files.addAll(retakeFiles);

        retakeFiles.clear();

    }
    @Override
    public void onTutorial() {
        Intent intent = new Intent(ScanModeActivity2.this, TutorialActivity.class);
        intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
        startActivity(intent);
    }

    public void completeScan(View view) {
        measure.setCreatedBy(session.getUserEmail());
        measure.setDate(AppController.getInstance().getUniversalTimestamp());
        measure.setAge(age);
        measure.setType("ir-1.0.2");
        measure.setWeight(0.0f);
        measure.setHeight(0.0f);
        measure.setHeadCircumference(0.0f);
        measure.setMuac(0.0f);
        measure.setOedema(false);
        measure.setPersonId(person.getId());
        measure.setTimestamp(mNowTime);
        measure.setQrCode(person.getQrcode());
        measure.setSchema_version(CgmDatabase.version);
        measure.setScannedBy("IR-"+session.getIntelrealsenseSno());
        measure.setStd_test_qr_code(session.getStdTestQrCode());
        if (session.getSelectedMode() == AppConstants.RST_MODE) {
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

    private static final String TAG = ScanModeActivity2.class.getSimpleName();

    public int SCAN_MODE = AppConstants.SCAN_STANDING;
    public int SCAN_STEP = AppConstants.SCAN_PREVIEW;
    private boolean step1 = false, step2 = false, step3 = false, step4 = false;

    public Person person;
    public Measure measure;
    public Loc location;

    private MeasureRepository measureRepository;
    private PersonRepository personRepository;
    private FileLogRepository fileLogRepository;
    private HashMap<Integer, ArrayList<Float>> lightScores;
    private ArrayList<FileLog> files;

    private ArrayList<FileLog> retakeFiles;

    private final Object lock = new Object();

    private SessionManager session;
    private ArrayList<Float> heights;

    private long mLastFeedbackTime;
    private TextView mTxtFeedback;
    private TextView mTitleView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

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

    private AbstractIntelARCamera mCameraInstance;
    private ImageView mOutline;
    private float mOutlineAlpha_bb = 1;
    AccuratePoseDetectorOptions options;
    PoseDetector poseDetector;
    ObjectDetectorOptions objectDetectorOptions;
    ObjectDetector objectDetector;
    public boolean isStanding;

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
            LogFileUtils.logException(throwable, "Scanemode oncreate");
            //Crashes.trackError(throwable);
            finish();
        });

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        isStanding = getIntent().getBooleanExtra(AppConstants.EXTRA_SCAN_MODE,true);
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

        activityScanModeBinding = DataBindingUtil.setContentView(this, R.layout.activity_scan_mode);

        mTxtFeedback = findViewById(R.id.txtFeedback);
        mTitleView = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);

        activityScanModeBinding.llScanScreenToolbar.setOnClickListener(this);

        findViewById(R.id.imgClose).setOnClickListener(this);

        mOutline = findViewById(R.id.scanOutline);
        ImageView colorCameraPreview = findViewById(R.id.colorCameraPreview);
        ImageView depthCameraPreview = findViewById(R.id.depthCameraPreview);
        GLSurfaceView glSurfaceView = findViewById(R.id.surfaceview);

        getCamera().onCreate(colorCameraPreview, depthCameraPreview, glSurfaceView, mOutline);

        measureRepository = MeasureRepository.getInstance(this);
        personRepository = PersonRepository.getInstance(this);
        fileLogRepository = FileLogRepository.getInstance(this);
        lightScores = new HashMap<>();
        //files = new ArrayList<>();

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
        activityScanModeBinding.scanType4.setListener(4,this);

        options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);

        objectDetectorOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableClassification()  // Optional
                .build();

        objectDetector = ObjectDetection.getClient(objectDetectorOptions);
        if (isStanding) {
            scanStanding();
        } else {
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

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        AbstractIntelARCamera.getRsContext().setDevicesChangedCallback(new DeviceListener() {
            @Override
            public void onDeviceAttach() {
                showDisconnectionAlert("Intel RealSense Connected");
                sessionManager.setIsSensorconnected(true);
            }

            @Override
            public void onDeviceDetach() {
                sessionManager.setIsSensorconnected(false);

                onSensorDisconnect();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        getCamera().onResume();
        getCamera().addListener(this);

        if (session.getStdTestQrCode() != null) {
            activityScanModeBinding.toolbar.setBackgroundResource(R.color.colorPink);
        } else {
            activityScanModeBinding.toolbar.setBackgroundResource(R.color.colorPrimary);
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        getCamera().removeListener(this);
        getCamera().onPause();
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        if (scanStarted && !scanCompleted) {
            firebaseAnalytics.logEvent(FirebaseService.SCAN_CANCELED, null);
        }
        if(finishCreateDataActivity) {
            Intent intent = new Intent("com.example.REALSENSE_DISCONNECTED");
            sendBroadcast(intent);
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
        File extFileDir = AppController.getInstance().getPublicAppDirectory(this);
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
        float cloudsToFinishScan = (SCAN_STEP % 100 == 1 ? 8 : 8);
        float progressToAddFloat = 100.0f / cloudsToFinishScan;
        int progressToAdd = (int) progressToAddFloat;
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
            resetScanStepUi();

            activityScanModeBinding.scanType1.setChildIcon(R.drawable.stand_front);
            activityScanModeBinding.scanType2.setChildIcon(R.drawable.side_scan_left_svg);
            activityScanModeBinding.scanType3.setChildIcon(R.drawable.stand_back);
            activityScanModeBinding.scanType4.setChildIcon(R.drawable.side_scan_right_svg);

            if (files != null) {
                files.clear();

            }
            if(retakeFiles!=null){
                retakeFiles.clear();
            }
            files = new ArrayList<>();
            retakeFiles = new ArrayList<>();
        } else if (SCAN_MODE == AppConstants.SCAN_LYING) {

            resetScanStepUi();
            activityScanModeBinding.scanType1.setChildIcon(R.drawable.lying_front);
            activityScanModeBinding.scanType2.setChildIcon(R.drawable.lying_side);
            activityScanModeBinding.scanType3.setChildIcon(R.drawable.lying_back);
            activityScanModeBinding.scanType4.setChildIcon(R.drawable.lying_side);
            if (files != null) {
                files.clear();
            }
            if(retakeFiles!=null){
                retakeFiles.clear();
            }
            files = new ArrayList<>();
            retakeFiles = new ArrayList<>();
        }
    }

    public void goToNextStep() {
        closeScan();

        if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT || SCAN_STEP == AppConstants.SCAN_LYING_FRONT) {
            activityScanModeBinding.scanType1.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_SIDE_LEFT || SCAN_STEP == AppConstants.SCAN_LYING_SIDE_LEFT) {
            activityScanModeBinding.scanType2.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_BACK || SCAN_STEP == AppConstants.SCAN_LYING_BACK) {
            activityScanModeBinding.scanType3.goToNextStep();
        } else if (SCAN_STEP == AppConstants.SCAN_STANDING_SIDE_RIGHT || SCAN_STEP == AppConstants.SCAN_LYING_SIDE_RIGHT) {
            activityScanModeBinding.scanType4.goToNextStep();
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

    public void resetScanStepUi(){
        if(step1){
            activityScanModeBinding.scanType1.resetScanStep(R.string.help_front_view_2);
        }
        if(step2){
            activityScanModeBinding.scanType2.resetScanStep(R.string.help_lateral_view);
        }
        if(step3){
            activityScanModeBinding.scanType3.resetScanStep(R.string.help_back_view);
        }
        if(step4){
            activityScanModeBinding.scanType4.resetScanStep(R.string.help_right_view);
        }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_LOCATION && grantResults.length > 0 && grantResults[0] >= 0) {
            getCurrentLocation();
        }
        if (requestCode == PERMISSION_CAMERA && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity2.this, R.string.permission_camera, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (requestCode == PERMISSION_STORAGE && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity2.this, R.string.storage_permission_needed, Toast.LENGTH_SHORT).show();
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

    private AbstractIntelARCamera getCamera() {
        if (mCameraInstance == null) {
            AbstractIntelARCamera.DepthPreviewMode depthMode;
            AbstractIntelARCamera.PreviewSize previewSize = AbstractIntelARCamera.PreviewSize.CLIPPED;
            if (LocalPersistency.getBoolean(this, SettingsActivity.KEY_SHOW_DEPTH)) {
                if (AREngineCamera.shouldUseAREngine()) {
                    depthMode = AbstractIntelARCamera.DepthPreviewMode.CENTER;
                } else {
                    depthMode = AbstractIntelARCamera.DepthPreviewMode.CENTER_LOW_POWER;
                }
            } else {
                if (AREngineCamera.shouldUseAREngine()) {
                    depthMode = AbstractIntelARCamera.DepthPreviewMode.FOCUS;
                } else {
                    depthMode = AbstractIntelARCamera.DepthPreviewMode.FOCUS_LOW_POWER;
                }
            }

            mCameraInstance = new ARRealSenseCamera2(this,depthMode,previewSize);
        }

        return mCameraInstance;
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex, int SCAN_STEP) {
        Log.i(TAG, "this is value of bitmap & frameindex " + bitmap + " " + frameIndex);

        createPose(bitmap, frameIndex);
    }

    int count = 0;

    public void onPostColorDataReceived(Bitmap bitmap, int frameIndex, float poseScore, String poseCoordinates, String boundingBox) {

        long profile = System.currentTimeMillis();
        cameraCalibration = mCameraInstance.getCameraCalibration();
        boolean hasCameraCalibration;
        if (cameraCalibration != null && !cameraCalibration.contains("NaN")) {
            hasCameraCalibration = true;
        } else {
            hasCameraCalibration = false;
        }

        Runnable thread = () -> {
            try {
                String currentImgFilename = "rgb_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".jpg";
                currentImgFilename = currentImgFilename.replace('/', '_');
                File artifactFile = new File(mRgbSaveFolder, currentImgFilename);
                BitmapHelper.writeBitmapToFile(bitmap, artifactFile);
                onProcessArtifact(artifactFile, ArtifactType.RGB, 0, poseScore, poseCoordinates, 0, 0, boundingBox, null);

                if (artifactFile.exists()) {
                    mColorSize += artifactFile.length();
                    mColorTime += System.currentTimeMillis() - profile;
                    if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                        LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_SIZE, mColorSize);
                        LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_COLOR_TIME, mColorTime);
                    }
                }

                artifactFile = new File(mScanArtefactsOutputFolder, "camera_calibration.txt");
                if (!artifactFile.exists()) {
                    if (hasCameraCalibration) {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(artifactFile.getAbsolutePath());
                            fileOutputStream.write(cameraCalibration.getBytes());
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            onProcessArtifact(artifactFile, ArtifactType.CALIBRATION, 0, 0, null, 0, 0, null, null);

                        } catch (Exception e) {
                            LogFileUtils.logException(e, "scanmode runnablethread");
                        }
                    }
                }
            } catch (Exception e) {
                LogFileUtils.logException(e, "scanmode runnablethread1 ");
            }

            onThreadChange(-1);
        };
        onThreadChange(1);
        executor.execute(thread);
    }

    @Override
    public void onDepthDataReceived(Depthmap depthmap, int frameIndex, DepthFrame depthFrame, int height, int width, byte[] byteArray) {
        onFeedbackUpdate();
        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0)) {
            String orientation;
            if(SCAN_MODE==AppConstants.SCAN_STANDING){
                orientation ="orientation_angle:"+ String.format("%.0f", transformAngle(horizontalAngle))+",app_angle:"+String.format("%.0f", verticalAngle);

            }else {
                orientation ="orientation_angle:"+ String.format("%.0f", verticalAngle)+",app_angle:"+String.format("%.0f", horizontalAngle);
            }

            float light = mCameraInstance.getLightIntensity();
            Log.i("Orientation", "this is value of orientation " + orientation);
            double child_distance = mCameraInstance.getTargetDistance();
            if (light > 1) {
                light = 1.0f - (light - 1.0f);
            }
            float light_score = light;
            long profile = System.currentTimeMillis();
            String depthmapFilename = "depth_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".depth";
            mNumberOfFilesWritten++;

            updateScanningProgress();

            onLightScore(getCamera().getLightIntensity());

            Runnable thread = () -> {
                try {
                    File artifactFile = new File(mDepthmapSaveFolder, depthmapFilename);
                    save(artifactFile,depthFrame,frameIndex,height,width,byteArray);
                    onProcessArtifact(artifactFile, ArtifactType.DEPTH, 10.0f, 0, null, child_distance, light_score, null, orientation);

                    if (artifactFile.exists()) {
                        mDepthSize += artifactFile.length();
                        mDepthTime += System.currentTimeMillis() - profile;
                        if (LocalPersistency.getBoolean(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE)) {
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_SIZE, mDepthSize);
                            LocalPersistency.setLong(this, SettingsPerformanceActivity.KEY_TEST_PERFORMANCE_DEPTH_TIME, mDepthTime);
                        }
                    }
                } catch (Exception e) {
                }

                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }

    public static double transformAngle(double inputAngle) {
        if (inputAngle == 90) {
            return 0;
        } else if (inputAngle < 90) {
            return 90 - inputAngle;
        } else {
            return 450 - inputAngle;
        }
    }

    float[] position;
    float[] rotation;

    @Override
    public void onAngleReceived(double verticalAngle, double horizontalAngle, float[] position, float[] rotation) {
        this.activityScanModeBinding.tvAngle.setText(" "+mCameraInstance.getLightIntensity());
        this.verticalAngle = 90-verticalAngle;

        if (System.currentTimeMillis() - lastUpdatedAngle > 300) {
            lastUpdatedAngle = System.currentTimeMillis();
            if(SCAN_MODE==AppConstants.SCAN_STANDING){
                activityScanModeBinding.tvAngle.setText(String.format("%.0f", this.verticalAngle));

            }else {
                this.horizontalAngle = 90-horizontalAngle;
                activityScanModeBinding.tvAngle.setText(String.format("%.0f", this.horizontalAngle));
            }

        }
        this.position = position;
        this.rotation = rotation;
    }

    Float distance;

    @Override
    public void onDistancereceived(Float distance) {
        this.distance = distance;
    }

    int color;

    @Override
    public void onChildVisible(boolean data) {
        mOutlineAlpha_bb= mOutlineAlpha_bb * 0.9f + 0.1f;
        int alpha = Math.max(0, (int)(mOutlineAlpha_bb * 128));
        color = Color.GREEN;
        if ((mCameraInstance.getTargetDistance() < AppConstants.TOO_NEAR) || (mCameraInstance.getTargetDistance() > AppConstants.TOO_FAR)) {
            color = Color.RED;
        } else if (!data) {
            color = Color.RED;
        }
        color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        ImageViewCompat.setImageTintList(mOutline, ColorStateList.valueOf(color));

    }

    @Override
    public void onSensorDisconnect() {
        showDisconnectionAlert("Intel RealSense Disconnected");
        session.setIsSensorconnected(false);
    }

    AlertDialog alertDialogdisconnect;


    private void showDisconnectionAlert(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_realsense_disconnect, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        tvTitle.setText(title);
        tvMessage.setText("Intel RealSense got disconnected");

        alertDialogdisconnect = builder.create();
        alertDialogdisconnect.show();
        finishCreateDataActivity = true;
        // Auto close after short delay
        new android.os.Handler().postDelayed(() -> {
            finish(); // Destroy the activity
        }, 500); // 1
    }


    public void save(File file, DepthFrame depthFrame1,int frameIndex,int height, int width, byte[] byteArray) {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            ZipOutputStream zip = new ZipOutputStream(stream);
            byte[] info = (width + "x" + height + "_0.001_7_" + getPose("_") + "\n").getBytes();
            try {
            }catch (Exception e){
            }
            zip.putNextEntry(new ZipEntry("data"));
            zip.write(info, 0, info.length);
            zip.write(byteArray, 0, byteArray.length);
            zip.flush();
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPose(String separator) {
        String output = "";
        output += rotation[0] + separator;
        output += rotation[1] + separator;
        output += rotation[2] + separator;
        output += rotation[3] + separator;
        output += position[0] + separator;
        output += position[1] + separator;
        output += position[2];
        return output;
    }

    private void onLightScore(float score) {
        Log.i(TAG, "this is valur light score " + score);
        synchronized (lock) {
            if (!lightScores.containsKey(SCAN_STEP)) {
                lightScores.put(SCAN_STEP, new ArrayList<>());
            }
            lightScores.get(SCAN_STEP).add(score);
        }
    }

    private void onFeedbackUpdate() {
        AbstractIntelARCamera.LightConditions light = getCamera().getLightConditionState();
        float distance = mCameraInstance.getTargetDistance();
        runOnUiThread(() -> {
            String formattedDistance = String.format("%.1f", distance);

            if ((SCAN_MODE == AppConstants.SCAN_LYING) && (SCAN_STEP != AppConstants.SCAN_LYING_FRONT)) {
                getCamera().setSkeletonMode(AbstractIntelARCamera.SkeletonMode.OFF);
                setOutline(true);
            } else if (false) {
                getCamera().setSkeletonMode(AbstractIntelARCamera.SkeletonMode.OUTLINE);
                setFeedback(null);
                setOutline(true);
            } else {
                getCamera().setSkeletonMode(AbstractIntelARCamera.SkeletonMode.OFF);
                setOutline(true);
            }

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

            if (distance < 0.7) {
                activityScanModeBinding.tvChildDistance.setText("Too Close");

            } else if (distance > 2.1f) {
                activityScanModeBinding.tvChildDistance.setText("Too Far");

            } else {
                activityScanModeBinding.tvChildDistance.setText(formattedDistance+" mts ");
                setFeedback(null);
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
        Log.i(TAG, "this is valur feedback " + feedback);

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

    private void onProcessArtifact(File artifactFile, ArtifactType type, float childHeight, float poseScore, String poseCordinates, double child_distance, float light_score, String boundinBox, String orientation) {
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

            log.setChildDetected(true);

            log.setChildHeight(childHeight);
            log.setPoseScore(poseScore);
            log.setPoseCoordinates(poseCordinates);
            log.setBoundingBox(boundinBox);

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
            log.setChild_distance(child_distance);
            log.setLight_score(light_score);
            log.setOrientation(orientation);

            synchronized (lock) {
                if(isRetake){
                    retakeFiles.add(log);
                }else {
                    files.add(log);
                }
            }
        }
    }

    private void onThreadChange(int diff) {
        synchronized (threadsLock) {
            threadsCount += diff;
            if (threadsCount == 0) {
            } else {
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

                if (lightScores.containsKey(SCAN_STEP)) {
                    for (Float value : lightScores.get(SCAN_STEP)) {
                        lightScore += value;
                    }
                    lightScore /= (float) lightScores.get(SCAN_STEP).size();
                }

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
                } else if (scanStep == AppConstants.SCAN_STANDING_SIDE_LEFT || scanStep == AppConstants.SCAN_LYING_SIDE_LEFT) {
                    activityScanModeBinding.scanType2.finishStep(issues);
                    step2 = true;

                } else if (scanStep == AppConstants.SCAN_STANDING_BACK || scanStep == AppConstants.SCAN_LYING_BACK) {
                    activityScanModeBinding.scanType3.finishStep(issues);
                    step3 = true;
                } else if (scanStep == AppConstants.SCAN_STANDING_SIDE_RIGHT || scanStep == AppConstants.SCAN_LYING_SIDE_RIGHT) {
                    activityScanModeBinding.scanType4.finishStep(issues);
                    step4 = true;
                }

                Thread saveRetakeThread = new Thread(saveRetakeScan);
                saveRetakeThread.start();

                if (step1 && step2 && step3 && step4) {
                    showCompleteButton();
                    firebaseAnalytics.logEvent(FirebaseService.SCAN_START, null);
                    scanStarted = true;
                }
            });
        }
    };

    private final Runnable saveRetakeScan = new Runnable() {
        @Override
        public void run() {
            if(isRetake){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                updateRetakeFileLog(SCAN_STEP);
            }
        }
    };

    private final Runnable saveMeasure = new Runnable() {
        @Override
        public void run() {
            getCamera().removeListener(this);

            waitUntilFinished();

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
                firebaseAnalytics.logEvent(FirebaseService.SCAN_SUCCESSFUL, null);
                finish();
            });
        }
    };

    public void createPose(Bitmap bitmap, int frameIndex) {
        Log.i(TAG, "this is inside point 0");

        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0)) {

            Log.i(TAG, "this is inside point 1");
            if (bitmap == null) {
                return;
            }
            String[] ans = new String[2];
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            poseDetector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<Pose>() {
                                @Override
                                public void onSuccess(Pose pose) {
                                    Log.i(TAG, "this is inside point 3");

                                    String poseCoordinates = null;
                                    float poseTotal = 0.0f;
                                    float poseScore = 0.0f;

                                    List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();
                                    if (allPoseLandmarks != null && allPoseLandmarks.size() > 0) {
                                        poseCoordinates = "";
                                        for (int i = 0; i < allPoseLandmarks.size(); i++) {
                                            poseCoordinates = poseCoordinates + " " + allPoseLandmarks.get(i).getInFrameLikelihood() + "," + allPoseLandmarks.get(i).getPosition().x + "," + allPoseLandmarks.get(i).getPosition().y;
                                            poseTotal = poseTotal + allPoseLandmarks.get(i).getInFrameLikelihood();
                                        }
                                        poseScore = (float) (poseTotal / 33.0);
                                        Log.i("ScaneModeActivity", "this is value pf pose " + poseScore + " " + poseCoordinates);

                                        ans[0] = String.valueOf(poseScore);
                                        ans[1] += poseCoordinates;

                                    }
                                    if (ans[0] == null) {
                                        ans[0] = "0.0";
                                    }
                                    Log.i(TAG, "this is inside point 4" + ans[0] + " " + ans[1]);
                                    cretaeBoundingbox(bitmap, frameIndex, Float.parseFloat(ans[0]), ans[1]);

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "this is inside point 5");
                                    cretaeBoundingbox(bitmap, frameIndex, 0.0f, null);

                                }
                            });
        }
    }

    public void cretaeBoundingbox(Bitmap bitmap, int frameIndex, float poseScore, String poseCoordinates) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        String boundingBox = null;
        childCount = 0;
        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                Log.i("ObjectDetector ", "this is value of bounding box " + detectedObjects);
                                String boundingBox = null;

                                if (detectedObjects.size() != 0 && detectedObjects.get(0) != null) {
                                    Rect rect = detectedObjects.get(0).getBoundingBox();

                                    if (rect != null) {
                                        boundingBox = "{\"left\":\"" + rect.left + "\", \"right\":\"" + rect.right + "\", \"top\":\"" + rect.top + "\", \"bottom\":\"" + rect.bottom + "\"}";
                                    }
                                } else {
                                }
                                onPostColorDataReceived(bitmap, frameIndex, poseScore, poseCoordinates, boundingBox);
                            }

                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onPostColorDataReceived(bitmap, frameIndex, poseScore, poseCoordinates, null);

                            }
                        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerReading = event.values.clone();
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}