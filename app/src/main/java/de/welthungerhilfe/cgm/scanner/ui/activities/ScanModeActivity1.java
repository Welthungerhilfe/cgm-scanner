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
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Base64;
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
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.Audio;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARRealSenseCamera1;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARRealSenseCamera2;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARRealSenseCamera2_no_angle;
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

public class ScanModeActivity1 extends BaseActivity implements View.OnClickListener, AbstractIntelARCamera.Camera2DataListener, ScanTypeView.ScanTypeListener, SensorEventListener {


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

    private static final int PERMISSION_STORAGE = 101;

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
        Intent intent = new Intent(this, ScanModeActivity1.class);
        intent.putExtra(AppConstants.EXTRA_SCAN_MODE, isStanding);
        intent.putExtra(AppConstants.EXTRA_PERSON, person);
        intent.putExtra(AppConstants.EXTRA_MEASURE, measure);

        finish(); // End the current instance of ScanModeActivity
        startActivity(intent); // Start a new instance with the provided data
    }

    public void onScan(int buttonId, boolean isRetake) {
        this.isRetake = isRetake;
        if(isRetake){
            retakeFiles.clear();
            retakeFiles = new ArrayList<>();
        }
        if (!checkStoragePermissions()) {
            Toast.makeText(ScanModeActivity1.this,"No permission "+checkStoragePermissions(),Toast.LENGTH_LONG).show();
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
            /*if(isRetake){
                updateRetakeFileLog(SCAN_STEP);
            }*/
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
        Intent intent = new Intent(ScanModeActivity1.this, TutorialActivity.class);
        intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
        startActivity(intent);
    }

    public void completeScan(View view) {
        measure.setCreatedBy(session.getUserEmail());
        measure.setDate(AppController.getInstance().getUniversalTimestamp());
        measure.setAge(age);
        measure.setType("ir-3.0.0");
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

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = AppConstants.SCAN_STANDING;
    public static int SCAN_STEP = AppConstants.SCAN_PREVIEW;
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

        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE " + Build.VERSION.SDK_INT);


        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        isStanding = getIntent().getBooleanExtra(AppConstants.EXTRA_SCAN_MODE,true);
        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);
        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE 2" + Build.VERSION.SDK_INT);

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
        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE 3" + Build.VERSION.SDK_INT);

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

        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE 4" + Build.VERSION.SDK_INT);


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

        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE 5" + Build.VERSION.SDK_INT);

        if (!checkStoragePermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }

        LogFileUtils.logInfoOffline(TAG, "INITALIZE SCANMODE 6" + Build.VERSION.SDK_INT);

        activityScanModeBinding.lytScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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
            // scanLying();
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
                // Toast.makeText(ScanModeActivity1.this, "Lying scan is currently unavailable", Toast.LENGTH_SHORT).show();

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
                // if(sessionManager.getSensorMode()== AppConstants.SENSOR_SELECTED){
                //  finish();
                sessionManager.setIsSensorconnected(false);

                onSensorDisconnect();
                // }
            }
        });

    }

    public boolean checkStoragePermissions() {
        LogFileUtils.logInfoOffline(TAG, "Checking permissions for Android API " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            boolean hasVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            LogFileUtils.logInfoOffline(TAG, "Images: " + hasImages + ", Video: " + hasVideo + ", Audio: " + hasAudio);
            if (!hasImages || !hasVideo || !hasAudio) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_VIDEO) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_AUDIO)) {
                    Toast.makeText(this, "This app needs media permissions to save and process scan data.", Toast.LENGTH_LONG).show();
                }
                LogFileUtils.logInfoOffline(TAG, "Requesting media permissions");
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        },
                        PERMISSION_STORAGE
                );
                return false;
            }
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    LogFileUtils.logInfoOffline(TAG, "Error opening MANAGE_ALL_FILES_ACCESS_PERMISSION: " + e.getMessage());
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                return false;
            }
            return true;
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "This app needs storage access to save scan data.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_STORAGE
                );
                return false;
            }
            return true;
        }
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
        if(alertDialogdisconnect!=null) {
            alertDialogdisconnect.dismiss();
        }
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
        File extFileDir = AppController.getInstance().getRootDirectory(this);
        // File extFileDir = AppController.getInstance().getPublicAppDirectory(this);
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
        //   LogFileUtils.logInfo(TAG, "currentProgress=" + mProgress + ", progressToAdd=" + progressToAdd);
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


            // activityScanModeBinding.scanType2.setTitle(R.string.side_scan);
            if (files != null) {
                files.clear();

            }
            if(retakeFiles!=null){
                retakeFiles.clear();
            }
            files = new ArrayList<>();
            retakeFiles = new ArrayList<>();
            //  getCamera().setPlaneMode(AbstractARCamera.PlaneMode.LOWEST);
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
            // getCamera().setPlaneMode(AbstractARCamera.PlaneMode.VISIBLE);
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
            Toast.makeText(ScanModeActivity1.this, R.string.permission_camera, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (requestCode == PERMISSION_STORAGE && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity1.this, R.string.storage_permission_needed, Toast.LENGTH_SHORT).show();
            finish();
        }
        setupScanArtifacts();
    }

    public void onBackPressed() {
        //  super.onBackPressed();
        if (activityScanModeBinding.lytScanner.getVisibility() == View.VISIBLE) {
            activityScanModeBinding.lytScanner.setVisibility(View.GONE);
        } else {
            finish();
        }
    }

    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 700;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_scan_result:
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < CLICK_DEBOUNCE_MS) {
                    return; // Ignore rapid clicks
                }
                lastClickTime = currentTime;
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
            if (LocalPersistency.getBoolean(this, SettingsActivity.KEY_SHOW_NO_IMU)) {
                mCameraInstance = new ARRealSenseCamera2_no_angle(this,depthMode,previewSize);

            }else {
                mCameraInstance = new ARRealSenseCamera2(this,depthMode,previewSize);

            }

            // mCameraInstance = new ARRealSenseCamera1(this,depthMode,previewSize);
           /* if (AREngineCamera.shouldUseAREngine()) {
                mCameraInstance = new AREngineCamera(this, depthMode, previewSize);
                AbstractIntelARCamera.DepthPreviewMode depthModear;
            } else {
                mCameraInstance = new ARCoreCamera(this, depthMode, previewSize);
            }*/
        }

        return mCameraInstance;
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex, int SCAN_STEP) {
       LogFileUtils.logInfo("ScanmodeActivity","this is from scanmodeactivity 1 "+SCAN_STEP+ " "+ScanModeActivity1.SCAN_STEP+" "+frameIndex);

        createPose(bitmap, frameIndex, SCAN_STEP);
    }

    int count =0;
    public void onPostColorDataReceived(Bitmap bitmap, int frameIndex, float poseScore, String poseCoordinates, String boundingBox, int SCAN_STEP) {
        LogFileUtils.logInfo("ScanmodeActivity","this is from create pose "+SCAN_STEP+ " "+ScanModeActivity1.SCAN_STEP);

        long profile = System.currentTimeMillis();
        cameraCalibration = mCameraInstance.getCameraCalibration();
        // cameraCalibration = session.getArcoreCaliFile();
        boolean hasCameraCalibration;
        if (cameraCalibration != null && !cameraCalibration.contains("NaN")) {
            hasCameraCalibration = true;
        } else {
            hasCameraCalibration = false;
        }



        Runnable thread = () -> {
            try {
                // Write RGB data
                String currentImgFilename = "rgb_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".jpg";
                currentImgFilename = currentImgFilename.replace('/', '_');
                File artifactFile = new File(mRgbSaveFolder, currentImgFilename);

                BitmapHelper.writeBitmapToFile(bitmap, artifactFile);


                onProcessArtifact(artifactFile, ArtifactType.RGB, 0, poseScore, poseCoordinates, 0, 0, boundingBox, null, SCAN_STEP);

                // Save RGB metadata
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
                            onProcessArtifact(artifactFile, ArtifactType.CALIBRATION, 0, 0, null, 0, 0, null, null,0);

                        } catch (Exception e) {
                            LogFileUtils.logException(e, "scanemode runnablethread");
                        }
                    }
                }
            } catch (Exception e) {
                LogFileUtils.logException(e, "scanemode runnablethread1 ");
            }

            onThreadChange(-1);
        };
        onThreadChange(1);
        executor.execute(thread);

    }

    @Override
    public void onDepthDataReceived(Depthmap depthmap, int frameIndex, DepthFrame depthFrame, int height, int width, byte[] byteArray) {
        /*if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
            updateScanningProgress();
     //       onProcessArtifact(artifactFile, ArtifactType.DEPTH, 10, 0, null, 1.0, 1.0f, null, "80.0");


        }


            if(true) {
            return;
        }*/
        //   LogFileUtils.logInfo2("Scanmode","Depthdata received 1"+depthFrame.getHeight()+" "+depthFrame.getWidth());


       /* float height = 0;
        if (SCAN_MODE == AppConstants.SCAN_STANDING) {
            height = getCamera().getTargetHeight();

            if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
                if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT) {
                    heights.add(height);

                }
            }


        }*/
        //    onFeedbackUpdate();

        onFeedbackUpdate();
        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0)) {
            String orientation;
            if(SCAN_MODE==AppConstants.SCAN_STANDING){
                orientation ="orientation_angle:"+ String.format("%.0f", transformAngle(horizontalAngle))+",app_angle:"+String.format("%.0f", verticalAngle);

            }else {
                orientation ="orientation_angle:"+ String.format("%.0f", verticalAngle)+",app_angle:"+String.format("%.0f", horizontalAngle);
            }


            float light = mCameraInstance.getLightIntensity();
            Log.i("ScanModeActivity", "this is value of orientation " + orientation);
            double child_distance = mCameraInstance.getTargetDistance();
            if (light > 1) {
                light = 1.0f - (light - 1.0f);
            }
            //LogFileUtils.logInfo(TAG,"this is valur of distance & light "+child_distance+" "+light);
            float light_score = light;
            long profile = System.currentTimeMillis();
            String depthmapFilename = "depth_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP + "_" + frameIndex + ".depth";
            mNumberOfFilesWritten++;

            updateScanningProgress();

            onLightScore(getCamera().getLightIntensity());

            //     float finalHeight = height * 100.0f;


            Runnable thread = () -> {
                try {

                    //write depthmap
                    File artifactFile = new File(mDepthmapSaveFolder, depthmapFilename);
                    //depthmap.save(artifactFile);
                    save(artifactFile,depthFrame,frameIndex,height,width,byteArray);
                    //   LogFileUtils.logInfoOffline("SCANMODE","this is depth -1");

                    //onProcessArtifact(artifactFile,ArtifactType.DEPTH, 10.0f, 0, null, distance, light_score, null,orientation);
                    onProcessArtifact(artifactFile, ArtifactType.DEPTH, 10.0f, 0, null, child_distance, light_score, null, orientation,0);

                    //  onProcessArtifact(File artifactFile, ArtifactType type, float childHeight, float poseScore, String poseCordinates, double child_distance, float light_score, String boundinBox, String orientation) {

                    //       onProcessArtifact(artifactFile, ArtifactType.DEPTH, 10, 0, null, 1.0, 1.0f, null, "80.0");

                    // LogFileUtils.logInfoOffline("SCANMODE","this is depth 0");

                    //   LogFileUtils.logInfo1("Scanmode","this is inside on Depthdata received 6"+depthFrame);

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
                    //  LogFileUtils.logInfoOffline("ScanModeActivity1", "OnDepthDataReceived "+e.getMessage());

                }

                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }

    public static double transformAngle(double inputAngle) {
        if (inputAngle == 90) {
            return 0; // Condition 1: input angle is 90, return 0
        } else if (inputAngle < 90) {
            return 90 - inputAngle; // Condition 2: input angle less than 90, return the difference
        } else { // inputAngle > 90
            return 450 - inputAngle; // Condition 3: input angle greater than 90, return based on 360-degree wrap
        }
    }

    float[] position;
    float[] rotation;
    @Override
    public void onAngleReceived(double verticalAngle,double horizontalAngle, float[] position, float[]rotation) {
        // activityScanModeBinding.tvAngle.setText(" "+mCameraInstance.getLightIntensity());
        this.verticalAngle = 90-verticalAngle;

        //  activityScanModeBinding.tvAngle.setText(""+mCameraInstance.getPersonCount());
        if (System.currentTimeMillis() - lastUpdatedAngle > 300) {

            lastUpdatedAngle = System.currentTimeMillis();
            //    activityScanModeBinding.tvAngle.setText(""+childCount);
            if(SCAN_MODE==AppConstants.SCAN_STANDING){
                activityScanModeBinding.tvAngle.setText(String.format("%.0f", this.verticalAngle));

            }else {
                this.horizontalAngle = 90-horizontalAngle;
                activityScanModeBinding.tvAngle.setText(String.format("%.0f", this.horizontalAngle));
            }



        }
        this.position = position;
        this.rotation =rotation;
        //   activityScanModeBinding.tvAngle.setText(angle.substring(0,4)+"");
        // this.angle = angle;
        //  activityScanModeBinding.tvAngle.setText(""+mCameraInstance.getPersonCount());

    }

    Float distance;
    @Override
    public void onDistancereceived(Float distance) {
        // activityScanModeBinding.tvAngle.setText(String.format("%.2f", distance));
        this.distance =distance;
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


            // int dataSize = stride * height;

            // Toast.makeText(PreviewActivity.this,"this is value of frame set "+depth.getProfile().getHeight()+" ,"+depth.getProfile().getWidth(),Toast.LENGTH_LONG).show();



            /*byte[] byteArray = new byte[dataSize];
            depthFrame1.getData(byteArray);*/


            //  byte[] data = getData();
            FileOutputStream stream = new FileOutputStream(file);
            ZipOutputStream zip = new ZipOutputStream(stream);
            byte[] info = (width + "x" + height + "_0.001_7_" + getPose("_") + "\n").getBytes();
            //LogFileUtils.logInfoOffline(TAG, "pose: " +getPose("_"));
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

    /*    float[] position = new float[] {0, 0, 0};
        float[] rotation = new float[] {0, 0, 0, 1};*/
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

  /*  public void onDepthDataReceived(Depthmap depthmap, int frameIndex) {

        float height = 0;
        if (SCAN_MODE == AppConstants.SCAN_STANDING) {
            height = getCamera().getTargetHeight();

            if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {
                if (SCAN_STEP == AppConstants.SCAN_STANDING_FRONT) {
                    heights.add(height);

                }
            }

            //realtime value
            *//*String text = getString(R.string.label_height) + " : " + String.format(Locale.US,"~%.1fcm", height * 100.0f) +
            "\nNoise amount: " + String.format(Locale.US, "%.3f", getCamera().getDepthNoiseAmount());
            runOnUiThread(() -> mTitleView.setText(text));*//*
        }
        Log.i("ScanModeActivity", "this is before on feedback update ");
        onFeedbackUpdate();

        if (mIsRecording && (frameIndex % AppConstants.SCAN_FRAMESKIP == 0)) {

            float light = mCameraInstance.getLightIntensity();
            String orientation ="horizontal_angle:"+ mCameraInstance.getOrientation()+", vertical_angel:"+String.format("%.0f", angle - 90);
            Log.i("ScanModeActivity", "this is value of orientation " + orientation);
            double child_distance = mCameraInstance.getTargetDistance();
            if (light > 1) {
                light = 1.0f - (light - 1.0f);
            }
            //LogFileUtils.logInfo(TAG,"this is valur of distance & light "+child_distance+" "+light);
            float light_score = light;
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
                    onProcessArtifact(artifactFile, ArtifactType.DEPTH, finalHeight, 0, null, child_distance, light_score, null, orientation);

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
                    LogFileUtils.logException(e, "OnDepthDataReceived");
                }

                onThreadChange(-1);
            };
            onThreadChange(1);
            executor.execute(thread);
        }
    }*/

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
        //  boolean childDetected = getCamera().getPersonCount() == 1;
        float distance = mCameraInstance.getTargetDistance();
        float wallDistance = mCameraInstance.getWallDistance();
        runOnUiThread(() -> {
            String formattedDistance = String.format("%.2f", distance);
            String formattedwallDistance = String.format("%.2f", wallDistance);
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

            // if (mTxtFeedback.getVisibility() == View.GONE) {
            switch (light) {
                case NORMAL:

                       /* if(mCameraInstance.getTargetDistance() < 0.7) {

                        }else if(mCameraInstance.getTargetDistance() > 1.5){
                            break;
                        }*/
                    setFeedback(null);
                    break;
                case BRIGHT:
                    setFeedback(getString(R.string.score_light_bright));
                    break;
                case DARK:
                    setFeedback(getString(R.string.score_light_dark));
                    break;
            }
            // }

            // if ((mTxtFeedback.getVisibility() == View.GONE) && (distance != 0)) {
            // if ((mTxtFeedback.getVisibility() == View.GONE) && (distance != 0)) {
            activityScanModeBinding.tvChildDistance.setText(formattedDistance+" mts, wall:= "+formattedwallDistance);

            /*if (distance < 0.7) {
                //  setFeedback("Too Close");
                activityScanModeBinding.tvChildDistance.setText("Too Close");

            } else if (distance > 2.1f) {
                //  setFeedback("Too Far");
                activityScanModeBinding.tvChildDistance.setText("Too Far");

            } else {
                activityScanModeBinding.tvChildDistance.setText(formattedDistance+" mts ");
                setFeedback(null);
            }*/
            //   }
        });

    }

    private void setOutline(boolean visible) {
        float alpha = mOutlineAlpha * 0.9f + 0.1f;
        if (visible && (alpha > 0)) {
            mOutline.setAlpha(alpha);
            mOutline.setVisibility(View.GONE);
        } else {
            mOutline.setVisibility(View.GONE);
        }
        mOutlineAlpha = visible ? alpha : -2;
    }

    private void setFeedback(String feedback) {
        //check if the feedback changed

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

    private void onProcessArtifact(File artifactFile, ArtifactType type, float childHeight, float poseScore, String poseCordinates, double child_distance, float light_score, String boundinBox, String orientation, int SCAN_STEP) {
        if(type == ArtifactType.DEPTH){
            //   LogFileUtils.logInfoOffline("SCANMODE","this is depth 0");
        }
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
                    if(type == ArtifactType.DEPTH){
                        //      LogFileUtils.logInfoOffline("SCANMODE","this is depth 1");
                    }
                    log.setStep(ScanModeActivity1.SCAN_STEP);
                    log.setId(AppController.getInstance().getArtifactId("scan-depth", mNowTime));
                    log.setType("depth");
                    break;
                case RGB:
                    log.setStep(SCAN_STEP);
                    log.setId(AppController.getInstance().getArtifactId("scan-rgb", mNowTime));
                    log.setType("rgb");
                    break;
            }
            if(type == ArtifactType.DEPTH){
                //LogFileUtils.logInfoOffline("SCANMODE","this is depth 1");
            }
            //set information if child is detected (note: this is unsupported on ARCore devices and for lying children wrongly oriented)
            // boolean childDetected = getCamera().getPersonCount() == 1;
            log.setChildDetected(true);

            log.setChildHeight(childHeight);
            log.setPoseScore(poseScore);
            log.setPoseCoordinates(poseCordinates);
            log.setBoundingBox(boundinBox);

            if(type == ArtifactType.DEPTH){
                //    LogFileUtils.logInfoOffline("SCANMODE","this is depth 2");
            }
            //set metadata
            log.setPath(artifactFile.getPath());
            log.setHashValue(FileSystem.getMD5(artifactFile.getPath()));
            log.setFileSize(artifactFile.length());
            log.setUploadDate(0);
            log.setDeleted(false);

            if(type == ArtifactType.DEPTH){
                //   LogFileUtils.logInfoOffline("SCANMODE","this is depth 3");
            }
            log.setQrCode(person.getQrcode());
            log.setCreateDate(mNowTime);
            log.setCreatedBy(session.getUserEmail());
            log.setAge(age);
            log.setSchema_version(CgmDatabase.version);
            log.setMeasureId(measure.getId());
            log.setEnvironment(session.getEnvironment());
            log.setChild_distance(child_distance);
            if(type == ArtifactType.DEPTH){
                // LogFileUtils.logInfoOffline("SCANMODE","this is depth 4");
            }
            log.setLight_score(light_score);
            log.setOrientation(orientation);


            synchronized (lock) {

                if(isRetake){
                    retakeFiles.add(log);

                }else {
                    files.add(log);
                    LogFileUtils.logInfo("PRE_GO_TO_NEXT",SCAN_STEP+" "+ScanModeActivity1.SCAN_STEP+" "+log.getPath());
                }
            }
        }
    }

    private void onThreadChange(int diff) {
        synchronized (threadsLock) {
            threadsCount += diff;
            if (threadsCount == 0) {
                //   LogFileUtils.logInfo(TAG, "The last thread finished");
            } else {
                // LogFileUtils.logInfo(TAG, "Amount of threads : " + threadsCount);
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
                    lightScore /= (float) lightScores.get(SCAN_STEP).size();
                }

                //too bright values are not over 100%
                if (lightScore > 1) {
                    lightScore = 1.0f - (lightScore - 1.0f);
                }
             //   logArtifactSummary("GO_TO_NEXT",files,SCAN_STEP);
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
                }
                else if (scanStep == AppConstants.SCAN_STANDING_SIDE_RIGHT || scanStep == AppConstants.SCAN_LYING_SIDE_RIGHT) {
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
                // First, encrypt all files
                logArtifactSummary("Before encryption- aes",files);
               // logArtifactSummary("Before encryption", files, AppConstants.SCAN_STANDING_FRONT); // Example for type 100
               // logArtifactSummary("Before encryption", files, AppConstants.SCAN_STANDING_BACK); // Example for type 100
               // logArtifactSummary("Before encryption", files, AppConstants.SCAN_STANDING_SIDE_LEFT); // Example for type 100
               // logArtifactSummary("Before encryption", files, AppConstants.SCAN_STANDING_SIDE_RIGHT); // Example for type 100
                for (FileLog log : files) {
                   // encryptImage(log.getPath());
                    encryptFile(log.getPath(),"CGM_2025_ajsspsa");
                    // Verify file exists after
                    if (!new File(log.getPath()).exists()) {
                        LogFileUtils.logError("ScanModeActivity1", "Encrypted file does not exist: " + log.getPath());
                    }
                }

               // logArtifactSummary("After encryption",files);

                // Then, insert FileLog entries
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

    public void createPose(Bitmap bitmap, int frameIndex, int SCAN_STEP) {
        Log.i(TAG, "this is inside point 0");
        LogFileUtils.logInfo("ScanmodeActivity","this is from create pose "+SCAN_STEP+ " "+ScanModeActivity1.SCAN_STEP+" "+frameIndex);

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
                                    // Task completed successfully
                                    // ...
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
                                        ans[1] = poseCoordinates;

                                    }
                                    if (ans[0] == null) {
                                        ans[0] = "0.0";
                                    }
                                    Log.i(TAG, "this is inside point 4" + ans[0] + " " + ans[1]);
                                    cretaeBoundingbox(bitmap, frameIndex, Float.parseFloat(ans[0]), ans[1], SCAN_STEP);


                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                    Log.i(TAG, "this is inside point 5");
                                    cretaeBoundingbox(bitmap, frameIndex, 0.0f, null,SCAN_STEP);


                                }
                            });
        }
    }

    public void cretaeBoundingbox(Bitmap bitmap, int frameIndex, float poseScore, String poseCoordinates, int SCAN_STEP) {

        LogFileUtils.logInfo("ScanmodeActivity","this is from bounding box "+SCAN_STEP+ " "+ScanModeActivity1.SCAN_STEP);

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
                                        //    LogFileUtils.logInfo(TAG,"this is value of bounding box "+boundingBox);

                                    }

                                } else {
                                }
                                onPostColorDataReceived(bitmap, frameIndex, poseScore, poseCoordinates, boundingBox, SCAN_STEP);
                            }

                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                                onPostColorDataReceived(bitmap, frameIndex, poseScore, poseCoordinates, null,SCAN_STEP);

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
        //      calculateVerticalAngle(accelerometerReading);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static final byte[] KEY = new byte[] { 0x12, 0x34, 0x56, 0x78 };

    private static final Map<String, Map<String, String>> artifactTracker = new ConcurrentHashMap<>(); // Key: "diff_index", Value: {rgb: path, depth: path}
    private static final Object trackerLock = new Object();

    public void encryptImage(String filePath) {
        // Validate input
        if (filePath == null || filePath.isEmpty()) {
            LogFileUtils.logError("ScanModeActivity1", "Invalid file path");
            return;
        }

        File file = new File(filePath);
        File tempFile = new File(file.getParent(), UUID.randomUUID().toString() + ".tmp");

        // Extract frame number (diff), index, and file type
        String frameNumber = "unknown";
        String index = "unknown";
        String fileType = filePath.contains(".depth") ? "depth" : filePath.contains(".jpg") ? "rgb" : "other";
        String fileName = file.getName();
        int lastUnderscore = fileName.lastIndexOf('_');
        int lastDot = fileName.lastIndexOf('.');
        if (lastUnderscore > 0 && lastDot > lastUnderscore) {
            frameNumber = fileName.substring(lastUnderscore + 1, lastDot); // e.g., 72
            int secondLastUnderscore = fileName.lastIndexOf('_', lastUnderscore - 1);
            if (secondLastUnderscore > 0) {
                index = fileName.substring(secondLastUnderscore + 1, lastUnderscore); // e.g., 104
            }
        }

        // Log input for pre-encryption pairing check
        String trackerKey = frameNumber + "_" + index;
        synchronized (trackerLock) {
            artifactTracker.computeIfAbsent(trackerKey, k -> new HashMap<>()).put(fileType, filePath);
            LogFileUtils.logInfo("ScanModeActivity1", "Input registered: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
            Map<String, String> pair = artifactTracker.get(trackerKey);
            if (pair.containsKey("rgb") && pair.containsKey("depth")) {
                LogFileUtils.logInfo("ScanModeActivity1", "Paired pre-encryption: diff=" + frameNumber + ", index=" + index + ", rgb=" + pair.get("rgb") + ", depth=" + pair.get("depth"));
            }
        }

        // Validate file and directory
        if (!file.exists() || !file.canRead()) {
            LogFileUtils.logError("ScanModeActivity1", "Input file not readable: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
            synchronized (trackerLock) {
                artifactTracker.get(trackerKey).remove(fileType); // Clean up invalid entry
            }
            return;
        }
        if (!file.canWrite()) {
            LogFileUtils.logError("ScanModeActivity1", "Input file not writable: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
        }
        if (!tempFile.getParentFile().canWrite()) {
            LogFileUtils.logError("ScanModeActivity1", "Temp directory not writable: " + tempFile.getParent() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
            return;
        }
        if (KEY == null || KEY.length == 0) {
            LogFileUtils.logError("ScanModeActivity1", "Invalid encryption key, type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
            return;
        }

        long startTime = System.currentTimeMillis();
        LogFileUtils.logInfo("ScanModeActivity1", "Starting encryption: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", time: " + startTime);

        boolean encryptionSuccess = false;
        try (FileInputStream in = new FileInputStream(file);
             FileOutputStream out = new FileOutputStream(tempFile);
             FileChannel channel = out.getChannel();
             FileLock lock = channel.lock()) {
            byte[] buffer = new byte[16384];
            int bytesRead;
            int keyLen = KEY.length;

            // Encrypt
            while ((bytesRead = in.read(buffer)) > 0) {
                for (int i = 0; i < bytesRead; i += keyLen) {
                    for (int j = 0; j < keyLen && (i + j) < bytesRead; j++) {
                        buffer[i + j] ^= KEY[j];
                    }
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.getFD().sync();

            LogFileUtils.logInfo("ScanModeActivity1", "Encryption done: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", duration: " + (System.currentTimeMillis() - startTime) + "ms");

            // Verify temp file
            if (!tempFile.exists() || tempFile.length() == 0) {
                LogFileUtils.logError("ScanModeActivity1", "Invalid temp file: exists=" + tempFile.exists() + ", size=" + tempFile.length() + ", path=" + tempFile.getPath() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
                return;
            }

            // Retry rename
            int maxRetries = 3;
            int retryCount = 0;
            boolean renamed = false;
            long delay = 50;
            while (retryCount < maxRetries && !renamed) {
                try {
                    if (file.exists() && !file.delete()) {
                        LogFileUtils.logError("ScanModeActivity1", "Failed to delete original file (retry " + (retryCount + 1) + "/" + maxRetries + "): " + file.getPath() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
                        retryCount++;
                        Thread.sleep(delay);
                        delay *= 2;
                        continue;
                    }
                    if (tempFile.renameTo(file)) {
                        renamed = true;
                        LogFileUtils.logInfo("ScanModeActivity1", "Renamed temp file: " + file.getPath() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", retries=" + retryCount);
                    } else {
                        LogFileUtils.logError("ScanModeActivity1", "Failed to rename (retry " + (retryCount + 1) + "/" + maxRetries + "): temp=" + tempFile.getPath() + ", target=" + file.getPath() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
                        retryCount++;
                        Thread.sleep(delay);
                        delay *= 2;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogFileUtils.logError("ScanModeActivity1", "Retry interrupted: " + e.getMessage() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + "--" + e);
                    retryCount++;
                    Thread.sleep(delay);
                    delay *= 2;
                }
            }

            if (!renamed) {
                LogFileUtils.logError("ScanModeActivity1", "Failed to rename after retries: " + tempFile.getPath() + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
                return;
            }

            // Verify final file
            if (!file.exists() || !file.canRead() || file.length() == 0) {
                LogFileUtils.logError("ScanModeActivity1", "Invalid final file: exists=" + file.exists() + ", readable=" + file.canRead() + ", size=" + file.length() + ", path=" + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index);
                return;
            }

            encryptionSuccess = true;
            LogFileUtils.logInfo("ScanModeActivity1", "Encryption completed: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", total duration: " + (System.currentTimeMillis() - startTime) + "ms");
        } catch (IOException | InterruptedException e) {
            LogFileUtils.logError("ScanModeActivity1", "Encryption failed: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", error: " + (e.getMessage() != null ? e.getMessage() : "Unknown") + "--" + e);
        } finally {
            synchronized (trackerLock) {
                Map<String, String> pair = artifactTracker.get(trackerKey);
                if (encryptionSuccess) {
                    pair.put(fileType + "_encrypted", filePath);
                    if (pair.containsKey("rgb_encrypted") && pair.containsKey("depth_encrypted")) {
                        LogFileUtils.logInfo("ScanModeActivity1", "Paired post-encryption: diff=" + frameNumber + ", index=" + index + ", rgb=" + pair.get("rgb") + ", depth=" + pair.get("depth"));
                    }
                } else {
                    LogFileUtils.logInfo("ScanModeActivity1", "Encryption failed for: " + filePath + ", type: " + fileType + ", diff: " + frameNumber + ", index: " + index + ", may cause post-encryption misalignment");
                }

                // Log unpaired inputs (pre-encryption misalignment)
                if (pair != null) {
                    if (pair.containsKey("rgb") && !pair.containsKey("depth")) {
                        LogFileUtils.logInfo("ScanModeActivity1", "Unpaired RGB pre-encryption: diff=" + frameNumber + ", index=" + index + ", rgb=" + pair.get("rgb"));
                    } else if (pair.containsKey("depth") && !pair.containsKey("rgb")) {
                        LogFileUtils.logInfo("ScanModeActivity1", "Unpaired depth pre-encryption: diff=" + frameNumber + ", index=" + index + ", depth=" + pair.get("depth"));
                    }
                }

                // Clean up temp file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }

    // Clear tracker after scan (call in goToNextStep or scan stop)
    private void clearArtifactTracker() {
        synchronized (trackerLock) {
            artifactTracker.clear();
            LogFileUtils.logInfo("ScanModeActivity1", "Artifact tracker cleared");
        }
    }

    private void logArtifactSummary(String phase, List<FileLog> fileLogs) {
        // Group artifacts by scan type
        Map<Integer, List<FileLog>> byScanType = new HashMap<>();
        for (FileLog log : fileLogs) {
            String filename = new File(log.getPath()).getName();
            int scanType;

            // Parse scan type from filename (e.g., ..._100_222.jpg -> 100)
            try {
                String[] parts = filename.split("_");
                String stepPart = parts[parts.length - 2]; // Second-to-last part is step
                scanType = Integer.parseInt(stepPart);
            } catch (Exception e) {
                if (filename.equals("camera_calibration.txt")) {
                    scanType = 0; // Calibration files
                } else {
                    continue; // Skip invalid filenames
                }
            }
            byScanType.computeIfAbsent(scanType, k -> new ArrayList<>()).add(log);
        }

        // Fixed scan types
        int[] scanTypes = {100, 102, 103, 104};

        synchronized (lock) { // Synchronize to avoid concurrent modification
            for (int scanType : scanTypes) {
                List<FileLog> logs = byScanType.getOrDefault(scanType, new ArrayList<>());

                // Separate RGB, depth, and calibration
                List<FileLog> rgbLogs = new ArrayList<>();
                List<FileLog> depthLogs = new ArrayList<>();
                long calibrationCount = 0;

                for (FileLog log : logs) {
                    String filename = new File(log.getPath()).getName();
                    if (filename.equals("camera_calibration.txt")) {
                        calibrationCount++;
                    } else if (filename.endsWith(".jpg")) {
                        rgbLogs.add(log);
                    } else if (filename.endsWith(".depth")) {
                        depthLogs.add(log);
                    }
                }

                // Sort by diff (frame index) to ensure consistent deletion order
                rgbLogs.sort((a, b) -> {
                    String aName = new File(a.getPath()).getName();
                    String bName = new File(b.getPath()).getName();
                    int aDiff = Integer.parseInt(aName.split("_")[aName.split("_").length - 1].replace(".jpg", ""));
                    int bDiff = Integer.parseInt(bName.split("_")[bName.split("_").length - 1].replace(".jpg", ""));
                    return aDiff - bDiff;
                });
                depthLogs.sort((a, b) -> {
                    String aName = new File(a.getPath()).getName();
                    String bName = new File(b.getPath()).getName();
                    int aDiff = Integer.parseInt(aName.split("_")[aName.split("_").length - 1].replace(".depth", ""));
                    int bDiff = Integer.parseInt(bName.split("_")[bName.split("_").length - 1].replace(".depth", ""));
                    return aDiff - bDiff;
                });

                // Limit to 9 artifacts per type
                if (rgbLogs.size() > 9) {
                    for (FileLog log : rgbLogs.subList(9, rgbLogs.size())) {
                        File file = new File(log.getPath());
                        if (file.exists()) {
                            if (file.delete()) {
                                LogFileUtils.logInfo("ScanModeActivity1", "Deleted excess RGB artifact: " + log.getPath() + " for scan type " + scanType);
                            } else {
                                LogFileUtils.logError("ScanModeActivity1", "Failed to delete excess RGB artifact: " + log.getPath() + " for scan type " + scanType);
                            }
                        }
                        log.setDeleted(true);
                        fileLogRepository.updateFileLog(log); // Update in database
                        files.remove(log); // Remove from files list
                    }
                    rgbLogs.subList(9, rgbLogs.size()).clear();
                }

                if (depthLogs.size() > 9) {
                    for (FileLog log : depthLogs.subList(9, depthLogs.size())) {
                        File file = new File(log.getPath());
                        if (file.exists()) {
                            if (file.delete()) {
                                LogFileUtils.logInfo("ScanModeActivity1", "Deleted excess depth artifact: " + log.getPath() + " for scan type " + scanType);
                            } else {
                                LogFileUtils.logError("ScanModeActivity1", "Failed to delete excess depth artifact: " + log.getPath() + " for scan type " + scanType);
                            }
                        }
                        log.setDeleted(true);
                        fileLogRepository.updateFileLog(log); // Update in database
                        files.remove(log); // Remove from files list
                    }
                    depthLogs.subList(9, depthLogs.size()).clear();
                }

                // Extract diffs for logging
                List<Integer> rgbDiffs = rgbLogs.stream()
                        .map(log -> {
                            String filename = new File(log.getPath()).getName();
                            String[] parts = filename.split("_");
                            String diffStr = parts[parts.length - 1].replace(".jpg", "");
                            try {
                                return Integer.parseInt(diffStr);
                            } catch (NumberFormatException e) {
                                return -1;
                            }
                        })
                        .filter(diff -> diff != -1)
                        .sorted()
                        .collect(Collectors.toList());

                List<Integer> depthDiffs = depthLogs.stream()
                        .map(log -> {
                            String filename = new File(log.getPath()).getName();
                            String[] parts = filename.split("_");
                            String diffStr = parts[parts.length - 1].replace(".depth", "");
                            try {
                                return Integer.parseInt(diffStr);
                            } catch (NumberFormatException e) {
                                return -1;
                            }
                        })
                        .filter(diff -> diff != -1)
                        .sorted()
                        .collect(Collectors.toList());

                // Format diff lists
                String rgbDiffStr = rgbDiffs.isEmpty() ? "None" : rgbDiffs.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                String depthDiffStr = depthDiffs.isEmpty() ? "None" : depthDiffs.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                String calibrationStr = calibrationCount == 0 ? "None" : String.valueOf(calibrationCount);

                // Log in specified format
                LogFileUtils.logInfo("ScanModeActivity1", phase + " - Type " + scanType);
                LogFileUtils.logInfo("ScanModeActivity1", "RGB diff: " + rgbDiffStr + " (" + rgbDiffs.size() + " artifacts)");
                LogFileUtils.logInfo("ScanModeActivity1", "Depth diff: " + depthDiffStr + " (" + depthDiffs.size() + " artifacts)");
                LogFileUtils.logInfo("ScanModeActivity1", "Calibration: " + calibrationStr + " artifact" + (calibrationCount == 0 ? "s" : ""));
            }
        }
    }

   /* private void logArtifactSummary(String phase, List<FileLog> fileLogs, int scanType) {
        // Filter artifacts for the specified scan type (parsed from filename)
        List<FileLog> logs = new ArrayList<>();
        for (FileLog log : fileLogs) {
            String filename = new File(log.getPath()).getName();
            int fileScanType;

            // Parse scan type from filename (e.g., ..._100_222.jpg -> 100)
            try {
                String[] parts = filename.split("_");
                String stepPart = parts[parts.length - 2]; // Second-to-last part is step
                fileScanType = Integer.parseInt(stepPart);
            } catch (Exception e) {
                continue; // Skip invalid filenames
            }

            if (fileScanType == scanType) {
                logs.add(log);
            }
        }

        // Separate RGB, depth, and calibration
        List<Integer> rgbDiffs = new ArrayList<>();
        List<Integer> depthDiffs = new ArrayList<>();
        long calibrationCount = 0;

        for (FileLog log : logs) {
            String filename = new File(log.getPath()).getName();

            // Handle calibration
            if (filename.equals("camera_calibration.txt")) {
                calibrationCount++;
                continue;
            }

            // Determine type from extension
            String type = filename.endsWith(".jpg") ? "rgb" : filename.endsWith(".depth") ? "depth" : null;
            if (type == null) continue;

            // Extract diff from filename (e.g., ..._100_222.jpg -> 222)
            String[] parts = filename.split("_");
            String diffStr = parts[parts.length - 1].replace(".jpg", "").replace(".depth", "");
            int diff;
            try {
                diff = Integer.parseInt(diffStr);
            } catch (NumberFormatException e) {
                continue; // Skip invalid diff
            }

            // Add to appropriate list
            if ("rgb".equals(type)) {
                rgbDiffs.add(diff);
            } else if ("depth".equals(type)) {
                depthDiffs.add(diff);
            }
        }

        // Sort diffs
        Collections.sort(rgbDiffs);
        Collections.sort(depthDiffs);

        // Format diff lists
        String rgbDiffStr = rgbDiffs.isEmpty() ? "None" : rgbDiffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String depthDiffStr = depthDiffs.isEmpty() ? "None" : depthDiffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String calibrationStr = calibrationCount == 0 ? "None" : String.valueOf(calibrationCount);

        // Log in specified format
        LogFileUtils.logInfo("ScanModeActivity1", phase + " - Type " + scanType);
        LogFileUtils.logInfo("ScanModeActivity1", "RGB diff: " + rgbDiffStr + " (" + rgbDiffs.size() + " artifacts)");
        LogFileUtils.logInfo("ScanModeActivity1", "Depth diff: " + depthDiffStr + " (" + depthDiffs.size() + " artifacts)");
        LogFileUtils.logInfo("ScanModeActivity1", "Calibration: " + calibrationStr + " artifact" + (calibrationCount == 0 ? "s" : ""));
    }*/

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // GCM tag length
     private static final int IV_LENGTH_BYTE = 12; // GCM IV length
     private static final int KEY_LENGTH_BIT = 256;

    public static void encryptFile(String filePath, String passphrase) {
        // Step 1: Generate AES key from passphrase
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(passphrase.getBytes("UTF-8"));
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            // Step 2: Read the file
            File file = new File(filePath);
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileBytes);
            fis.close();

            // Step 3: Generate a random IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            // Step 4: Encrypt the file content
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(fileBytes);

            // Step 5: Combine IV and encrypted data
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

            // Step 6: Base64 encode for easier storage/transmission
            String encryptedBase64 = Base64.encodeToString(encryptedWithIv, Base64.DEFAULT);

            // Step 7: Write the encrypted data back to the file
            FileWriter writer = new FileWriter(file);
            writer.write(encryptedBase64);
            writer.close();
        }catch (Exception e){
            LogFileUtils.logError("ScanmodeActivity1","this is encryption error "+e.getMessage());
        }
    }
    private void logArtifactSummary(String phase, List<FileLog> fileLogs, int scanType) {
        // Map to store diffs by scan type and type (rgb/depth)
        Map<Integer, Map<String, List<Integer>>> diffsByType = new HashMap<>();
        Map<Integer, List<FileLog>> logsByScanType = new HashMap<>();

        // Parse all file logs to identify diffs and group by scan type
        for (FileLog log : fileLogs) {
            String filename = new File(log.getPath()).getName();
            int fileScanType;
            String[] parts = filename.split("_");

            // Parse scan type from filename (e.g., ..._100_222.jpg -> 100)
            try {
                String stepPart = parts[parts.length - 2]; // Second-to-last part is step
                fileScanType = Integer.parseInt(stepPart);
            } catch (Exception e) {
                if (filename.equals("camera_calibration.txt")) {
                    logsByScanType.computeIfAbsent(0, k -> new ArrayList<>()).add(log);
                }
                continue; // Skip invalid filenames
            }

            logsByScanType.computeIfAbsent(fileScanType, k -> new ArrayList<>()).add(log);

            // Skip calibration files
            if (filename.equals("camera_calibration.txt")) {
                continue;
            }

            // Determine file type
            String type = filename.endsWith(".jpg") ? "rgb" : filename.endsWith(".depth") ? "depth" : null;
            if (type == null) continue;

            // Extract diff (e.g., 222 from ..._100_222.jpg)
            String diffStr = parts[parts.length - 1].replace(".jpg", "").replace(".depth", "");
            int diff;
            try {
                diff = Integer.parseInt(diffStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // Store diff
            diffsByType.computeIfAbsent(fileScanType, k -> new HashMap<>())
                    .computeIfAbsent(type, k -> new ArrayList<>()).add(diff);
        }

        // Find depth artifacts without RGB pairs and update RGB file paths
        for (Map.Entry<Integer, Map<String, List<Integer>>> entry : diffsByType.entrySet()) {
            int fileScanType = entry.getKey();
            Map<String, List<Integer>> diffs = entry.getValue();
            List<Integer> depthDiffs = diffs.getOrDefault("depth", new ArrayList<>());
            List<Integer> rgbDiffs = diffs.getOrDefault("rgb", new ArrayList<>());

            // Check each depth diff
            for (int diff : depthDiffs) {
                if (!rgbDiffs.contains(diff)) {
                    // Find RGB file with this diff in other scan types
                    for (Map.Entry<Integer, Map<String, List<Integer>>> otherEntry : diffsByType.entrySet()) {
                        int otherScanType = otherEntry.getKey();
                        if (otherScanType == fileScanType) continue;

                        List<Integer> otherRgbDiffs = otherEntry.getValue().getOrDefault("rgb", new ArrayList<>());
                        if (otherRgbDiffs.contains(diff)) {
                            // Find the FileLog for this RGB file
                            for (FileLog log : fileLogs) {
                                String filename = new File(log.getPath()).getName();
                                if (!filename.endsWith(".jpg")) continue;

                                String[] parts = filename.split("_");
                                try {
                                    int logScanType = Integer.parseInt(parts[parts.length - 2]);
                                    String diffStr = parts[parts.length - 1].replace(".jpg", "");
                                    int logDiff = Integer.parseInt(diffStr);

                                    if (logScanType == otherScanType && logDiff == diff) {
                                        // Prepare new path
                                        String newPath = log.getPath().replace("_" + otherScanType + "_" + diff + ".jpg",
                                                "_" + fileScanType + "_" + diff + ".jpg");

                                        // Physically rename file
                                        File oldFile = new File(log.getPath());
                                        File newFile = new File(newPath);
                                        if (oldFile.exists()) {
                                            if (!oldFile.renameTo(newFile)) {
                                                LogFileUtils.logError("ScanModeActivity1", "Failed to rename " + log.getPath() + " to " + newPath);
                                                continue;
                                            }
                                        } else {
                                            LogFileUtils.logError("ScanModeActivity1", "File does not exist: " + log.getPath());
                                            continue;
                                        }

                                        // Update FileLog path and step
                                        log.setPath(newPath);
                                        log.setStep(fileScanType); // Assuming FileLog has setStep(int)

                                        // Update logsByScanType
                                        logsByScanType.get(otherScanType).remove(log);
                                        logsByScanType.computeIfAbsent(fileScanType, k -> new ArrayList<>()).add(log);
                                    }
                                } catch (Exception e) {
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Process logs for the specified scan type
        List<FileLog> logs = logsByScanType.getOrDefault(scanType, new ArrayList<>());

        // Separate RGB, depth, and calibration
        List<Integer> rgbDiffs = new ArrayList<>();
        List<Integer> depthDiffs = new ArrayList<>();
        long calibrationCount = 0;

        for (FileLog log : logs) {
            String filename = new File(log.getPath()).getName();

            // Handle calibration
            if (filename.equals("camera_calibration.txt")) {
                calibrationCount++;
                continue;
            }

            // Determine type from extension
            String type = filename.endsWith(".jpg") ? "rgb" : filename.endsWith(".depth") ? "depth" : null;
            if (type == null) continue;

            // Extract diff from filename
            String[] parts = filename.split("_");
            String diffStr = parts[parts.length - 1].replace(".jpg", "").replace(".depth", "");
            int diff;
            try {
                diff = Integer.parseInt(diffStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // Add to appropriate list
            if ("rgb".equals(type)) {
                rgbDiffs.add(diff);
            } else if ("depth".equals(type)) {
                depthDiffs.add(diff);
            }
        }

        // Sort diffs
        Collections.sort(rgbDiffs);
        Collections.sort(depthDiffs);

        // Format diff lists
        String rgbDiffStr = rgbDiffs.isEmpty() ? "None" : rgbDiffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String depthDiffStr = depthDiffs.isEmpty() ? "None" : depthDiffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String calibrationStr = calibrationCount == 0 ? "None" : String.valueOf(calibrationCount);

        // Log in specified format
        LogFileUtils.logInfo("ScanModeActivity1", phase + " - Type " + scanType);
        LogFileUtils.logInfo("ScanModeActivity1", "RGB diff: " + rgbDiffStr + " (" + rgbDiffs.size() + " artifacts)");
        LogFileUtils.logInfo("ScanModeActivity1", "Depth diff: " + depthDiffStr + " (" + depthDiffs.size() + " artifacts)");
        LogFileUtils.logInfo("ScanModeActivity1", "Calibration: " + calibrationStr + " artifact" + (calibrationCount == 0 ? "s" : ""));
    }
}