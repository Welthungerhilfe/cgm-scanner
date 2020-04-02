package de.welthungerhilfe.cgm.scanner.ui.activities;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.gson.Gson;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ShortBuffer;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactList;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.arcore.render.BackgroundRenderer;
import de.welthungerhilfe.cgm.scanner.helper.arcore.render.OcclusionRenderer;
import de.welthungerhilfe.cgm.scanner.helper.arcore.tool.CameraPermissionHelper;
import de.welthungerhilfe.cgm.scanner.helper.arcore.tool.DisplayRotationHelper;
import de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver;
import de.welthungerhilfe.cgm.scanner.helper.service.AddressService;
import de.welthungerhilfe.cgm.scanner.helper.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.helper.tango.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.helper.tango.OverlaySurface;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.MULTI_UPLOAD_BUNCH;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_SIDE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_PREVIEW;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_SIDE;

public class ScanModeActivity extends AppCompatActivity implements View.OnClickListener, GLSurfaceView.Renderer, ImageReader.OnImageAvailableListener, SurfaceTexture.OnFrameAvailableListener {
    private final int PERMISSION_LOCATION = 0x0001;
    private final int PERMISSION_CAMERA = 0x0002;
    private final int PERMISSION_STORAGE = 0x0002;

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

    @BindView(R.id.imgScanStep1)
    ImageView imgScanStep1;
    @BindView(R.id.imgScanStep2)
    ImageView imgScanStep2;
    @BindView(R.id.imgScanStep3)
    ImageView imgScanStep3;

    @BindView(R.id.btnScanStep1)
    Button btnScanStep1;
    @BindView(R.id.btnScanStep2)
    Button btnScanStep2;
    @BindView(R.id.btnScanStep3)
    Button btnScanStep3;
    @BindView(R.id.btnScanComplete)
    Button btnScanComplete;

    @BindView(R.id.lytScanStep1)
    LinearLayout lytScanStep1;
    @BindView(R.id.lytScanStep2)
    LinearLayout lytScanStep2;
    @BindView(R.id.lytScanStep3)
    LinearLayout lytScanStep3;

    @BindView(R.id.lytSelectMode)
    LinearLayout lytSelectMode;

    @BindView(R.id.lytScanSteps)
    LinearLayout lytScanSteps;
    @BindView(R.id.lytScanner)
    LinearLayout lytScanner;

    @BindView(R.id.imgScanSuccess1)
    ImageView imgScanSuccess1;
    @BindView(R.id.imgScanSuccess2)
    ImageView imgScanSuccess2;
    @BindView(R.id.imgScanSuccess3)
    ImageView imgScanSuccess3;

    @BindView(R.id.txtScanStep1)
    TextView txtScanStep1;
    @BindView(R.id.txtScanStep2)
    TextView txtScanStep2;
    @BindView(R.id.txtScanStep3)
    TextView txtScanStep3;

    @BindView(R.id.lytScanAgain1)
    LinearLayout lytScanAgain1;
    @BindView(R.id.lytScanAgain2)
    LinearLayout lytScanAgain2;
    @BindView(R.id.lytScanAgain3)
    LinearLayout lytScanAgain3;

    @BindView(R.id.btnRetake1)
    Button btnRetake1;
    @BindView(R.id.btnRetake2)
    Button btnRetake2;
    @BindView(R.id.btnRetake3)
    Button btnRetake3;

    @BindView(R.id.btnTutorial1)
    Button btnTutorial1;
    @BindView(R.id.btnTutorial2)
    Button btnTutorial2;
    @BindView(R.id.btnTutorial3)
    Button btnTutorial3;

    @OnClick(R.id.lytScanStanding)
    void scanStanding() {
        SCAN_MODE = SCAN_STANDING;

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
        SCAN_MODE = SCAN_LYING;

        imgScanLying.setImageResource(R.drawable.lying_active);
        imgScanLyingCheck.setImageResource(R.drawable.radio_active);
        txtScanLying.setTextColor(getResources().getColor(R.color.colorBlack, getTheme()));

        imgScanStanding.setImageResource(R.drawable.standing_inactive);
        imgScanStandingCheck.setImageResource(R.drawable.radio_inactive);
        txtScanStanding.setTextColor(getResources().getColor(R.color.colorGreyDark, getTheme()));

        changeMode();
    }
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btnScanStep1, R.id.btnRetake1})
    void scanStep1() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_CAMERA);
        } else {
            if (SCAN_MODE == SCAN_STANDING) {
                SCAN_STEP = SCAN_STANDING_FRONT;

                mTitleView.setText(getString(R.string.front_view_01) + " - " + getString(R.string.mode_standing));
            } else if (SCAN_MODE == SCAN_LYING) {
                SCAN_STEP = SCAN_LYING_FRONT;

                mTitleView.setText(getString(R.string.front_view_01) + " - " + getString(R.string.mode_lying));
            }

            fab.setImageResource(R.drawable.recorder);
            lytScanner.setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btnScanStep2, R.id.btnRetake2})
    void scanStep2() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_CAMERA);
        } else {
            if (SCAN_MODE == SCAN_STANDING) {
                SCAN_STEP = SCAN_STANDING_SIDE;

                mTitleView.setText(getString(R.string.lateral_view_02) + " - " + getString(R.string.mode_standing));
            } else if (SCAN_MODE == SCAN_LYING) {
                SCAN_STEP = SCAN_LYING_SIDE;

                mTitleView.setText(getString(R.string.lateral_view_02) + " - " + getString(R.string.mode_lying));
            }

            fab.setImageResource(R.drawable.recorder);
            lytScanner.setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btnScanStep3, R.id.btnRetake3})
    void scanStep3() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_CAMERA);
        } else {
            if (SCAN_MODE == SCAN_STANDING) {
                SCAN_STEP = SCAN_STANDING_BACK;

                mTitleView.setText(getString(R.string.back_view_03) + " - " + getString(R.string.mode_standing));
            } else if (SCAN_MODE == SCAN_LYING) {
                SCAN_STEP = SCAN_LYING_BACK;

                mTitleView.setText(getString(R.string.back_view_03) + " - " + getString(R.string.mode_lying));
            }

            fab.setImageResource(R.drawable.recorder);
            lytScanner.setVisibility(View.VISIBLE);
        }
    }

    @OnClick({R.id.btnTutorial1, R.id.btnTutorial2, R.id.btnTutorial3})
    void showTutorial() {
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
        measure.setTimestamp(Utils.getUniversalTimestamp());
        measure.setQrCode(person.getQrcode());
        measure.setSchema_version(CgmDatabase.version);

        progressDialog.show();

        new SaveMeasureTask(ScanModeActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private AddressReceiver receiver = new AddressReceiver(new Handler()) {
        @Override
        public void onAddressDetected(String result) {
            location.setAddress(result);
            measure.setLocation(location);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
        }
    };

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = SCAN_STANDING;
    public int SCAN_STEP = SCAN_PREVIEW;
    private boolean step1 = false, step2 = false, step3 = false;

    public Person person;
    public Measure measure;
    public Loc location;

    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;
    private ArtifactResultRepository artifactResultRepository;

    private SessionManager session;

    private boolean mIsConnected = false;

    private GLSurfaceView mCameraSurfaceView;
    //private OverlaySurface mOverlaySurfaceView;
    private CameraSurfaceRenderer mRenderer;

    private TextView mTitleView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;


    private File mExtFileDir;
    private File mScanArtefactsOutputFolder;
    private String mPointCloudSaveFolderPath;
    private File mPointCloudSaveFolder;
    private File mRgbSaveFolder;

    private int mDisplayRotation = Surface.ROTATION_0;

    private boolean mPointCloudAvailable;
    private boolean mIsRecording;
    private int mProgress;

    private long mNowTime;
    private String mNowTimeString;

    private Semaphore mutex_on_mIsRecording;

    private long age = 0;

    private int noOfPoints;
    private double averageLigtingPenality=0.00;

    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private static final int INVALID_TEXTURE_ID = 0;
    private static final int SECS_TO_MILLISECS = 1000;


    private AlertDialog progressDialog;


    // Google AR Core
    private DisplayRotationHelper displayRotationHelper;

    private boolean surfaceCreated;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final OcclusionRenderer occlusionRenderer = new OcclusionRenderer();

    private final AtomicBoolean shouldUpdateSurfaceTexture = new AtomicBoolean(false);

    private Session sharedSession;

    private CameraDevice cameraDevice;

    private SharedCamera sharedCamera;

    private String cameraId;

    boolean initialized = false;

    private ImageReader cpuImageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;

    private boolean captureSessionChangesPossible = true;

    private CaptureRequest.Builder previewCaptureRequestBuilder;

    private final ConditionVariable safeToExitApp = new ConditionVariable();

    boolean isGlAttached;

    private CameraCaptureSession captureSession;

    private final CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
            ScanModeActivity.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " closed.");
            ScanModeActivity.this.cameraDevice = null;
            safeToExitApp.open();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.w(TAG, "Camera device ID " + cameraDevice.getId() + " disconnected.");
            cameraDevice.close();
            ScanModeActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Camera device ID " + cameraDevice.getId() + " error " + error);
            cameraDevice.close();
            ScanModeActivity.this.cameraDevice = null;
            // Fatal error. Quit application.
            finish();
        }
    };

    CameraCaptureSession.StateCallback cameraCaptureCallback = new CameraCaptureSession.StateCallback() {
        // Called when the camera capture session is first configured after the app
        // is initialized, and again each time the activity is resumed.
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session configured.");
            captureSession = session;
            resumeCamera2();
        }

        @Override
        public void onSurfacePrepared(
                CameraCaptureSession session, Surface surface) {
            Log.d(TAG, "Camera capture surface prepared.");
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session ready.");
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session active.");
            synchronized (ScanModeActivity.this) {
                captureSessionChangesPossible = true;
                ScanModeActivity.this.notify();
            }
        }

        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession session) {
            Log.w(TAG, "Camera capture queue empty.");
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session closed.");
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure camera capture session.");
        }
    };

    private final CameraCaptureSession.CaptureCallback captureSessionCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            shouldUpdateSurfaceTexture.set(true);
        }

        @Override
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
            Log.e(TAG, "onCaptureBufferLost: " + frameNumber);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Log.e(TAG, "onCaptureFailed: " + failure.getFrameNumber() + " " + failure.getReason());
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            Log.e(TAG, "onCaptureSequenceAborted: " + sequenceId + " " + session);
        }
    };

    public void onStart() {
        super.onStart();

        mutex_on_mIsRecording = new Semaphore(1,true);
        mIsRecording = false;
        mPointCloudAvailable = false;
    }

    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        /*
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Crashes.trackError(throwable);
            finish();
        });

         */

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);

        if (person == null) {
            Toast.makeText(this, "Person was not defined", Toast.LENGTH_LONG).show();
            finish();
        }

        mNowTime = System.currentTimeMillis();
        mNowTimeString = String.valueOf(mNowTime);

        session = new SessionManager(this);

        age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;

        if (measure == null) {
            measure = new Measure();
            measure.setId(AppController.getInstance().getMeasureId());
            measure.setQrCode(person.getQrcode());
            measure.setCreatedBy(session.getUserEmail());
            measure.setAge(age);
            measure.setDate(System.currentTimeMillis());
            measure.setArtifact_synced(false);
        }

        setContentView(R.layout.activity_scan_mode);

        ButterKnife.bind(this);

        mTitleView = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);

        findViewById(R.id.btnRetake).setOnClickListener(this);
        findViewById(R.id.imgClose).setOnClickListener(this);

        // GL surface view that renders camera preview image.
        mCameraSurfaceView = findViewById(R.id.surfaceview);
        mCameraSurfaceView.setPreserveEGLContextOnPause(true);
        mCameraSurfaceView.setEGLContextClientVersion(2);
        mCameraSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mCameraSurfaceView.setRenderer(this);
        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Helpers, see hello_ar_java sample to learn more.
        displayRotationHelper = new DisplayRotationHelper(this);

        measureRepository = MeasureRepository.getInstance(this);
        fileLogRepository = FileLogRepository.getInstance(this);
        artifactResultRepository = ArtifactResultRepository.getInstance(this);

        setupToolbar();

        getCurrentLocation();

        setupScanArtifacts();

        progressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_loading)
                .create();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSION_STORAGE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        waitUntilCameraCaptureSessionIsActive();
        startBackgroundThread();

        mCameraSurfaceView.onResume();

        if (surfaceCreated) {
            if (initialized) System.exit(0);
            openCamera();
        }

        displayRotationHelper.onResume();
    }

    @Override
    protected void onPause() {
        mCameraSurfaceView.onPause();
        waitUntilCameraCaptureSessionIsActive();
        displayRotationHelper.onPause();
        closeCamera();
        stopBackgroundThread();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // Stop background handler thread.
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while trying to join background handler thread", e);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_add_measure);
    }

    private void setupScanArtifacts() {
        mExtFileDir = AppController.getInstance().getRootDirectory();

        Log.e("Root Directory", mExtFileDir.getParent());
        mScanArtefactsOutputFolder  = new File(mExtFileDir,person.getQrcode() + "/measurements/" + mNowTimeString + "/");
        mPointCloudSaveFolder = new File(mScanArtefactsOutputFolder,"pc");
        mRgbSaveFolder = new File(mScanArtefactsOutputFolder,"rgb");

        if(!mPointCloudSaveFolder.exists()) {
            boolean created = mPointCloudSaveFolder.mkdirs();
            if (created) {
                Log.i(TAG, "Folder: \"" + mPointCloudSaveFolder + "\" created\n");
            } else {
                Log.e(TAG,"Folder: \"" + mPointCloudSaveFolder + "\" could not be created!\n");
            }
        }

        if(!mRgbSaveFolder.exists()) {
            boolean created = mRgbSaveFolder.mkdirs();
            if (created) {
                Log.i(TAG, "Folder: \"" + mRgbSaveFolder + "\" created\n");
            } else {
                Log.e(TAG,"Folder: \"" + mRgbSaveFolder + "\" could not be created!\n");
            }
        }

        Log.v(TAG,"mPointCloudSaveFolder: "+mPointCloudSaveFolder);
        Log.v(TAG,"mRgbSaveFolder: "+mRgbSaveFolder);
    }

    private void updateScanningProgress(int numPoints, float distance, float confidence) {
        float minPointsToCompleteScan = 199500.0f;
        float progressToAddFloat = numPoints / minPointsToCompleteScan;
        progressToAddFloat = progressToAddFloat*100;
        int progressToAdd = (int) progressToAddFloat;
        Log.d(TAG, "numPoints: "+numPoints+" float: "+progressToAddFloat+" currentProgress: "+mProgress+" progressToAdd: "+progressToAdd);
        if (mProgress+progressToAdd > 100) {
            mProgress = 100;
            runOnUiThread(() -> fab.setImageResource(R.drawable.done));
        } else {
            mProgress = mProgress+progressToAdd;
        }

        Log.d("scan_progress", String.valueOf(mProgress));
        Log.d("scan_progress_step", String.valueOf(progressToAdd));
    }

    private void changeMode() {
        if (SCAN_MODE == SCAN_STANDING) {
            imgScanStep1.setImageResource(R.drawable.stand_front_active);
            imgScanStep2.setImageResource(R.drawable.stand_side_active);
            imgScanStep3.setImageResource(R.drawable.stand_back_active);
        } else if (SCAN_MODE == SCAN_LYING) {
            imgScanStep1.setImageResource(R.drawable.lying_front_active);
            imgScanStep2.setImageResource(R.drawable.lying_side_active);
            imgScanStep3.setImageResource(R.drawable.lying_back_active);
        }
    }

    public void goToNextStep() {
        closeScan();
        getScanQuality(measure.getId(),SCAN_STEP);
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

    private void hideCompleteButton() {
        int cx = (btnScanComplete.getLeft() + btnScanComplete.getRight()) / 2;
        int cy = (btnScanComplete.getTop() + btnScanComplete.getBottom()) / 2;

        int dx = Math.max(cx, btnScanComplete.getWidth() - cx);
        int dy = Math.max(cy, btnScanComplete.getHeight() - cy);
        float finalRadius = (float) Math.hypot(dx, dy);

        Animator animator = ViewAnimationUtils.createCircularReveal(btnScanComplete, cx, cy, finalRadius, 0);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                btnScanComplete.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void startScan() {
        mProgress = 0;

        resumeScan();
    }

    private void resumeScan() {
        if (SCAN_STEP == SCAN_PREVIEW)
            return;

        mIsRecording = true;
        fab.setImageResource(R.drawable.stop);
    }

    private void pauseScan() {
        mIsRecording = false;
        fab.setImageResource(R.drawable.recorder);
    }

    public void closeScan() {
        mIsRecording = false;
        progressBar.setProgress(0);
        mProgress = 0;

        lytScanner.setVisibility(View.GONE);
    }

    @SuppressLint("StaticFieldLeak")
    private void getScanQuality(String measureId, int scanStep) {
        new AsyncTask<Void, Void, Boolean>() {
            private double averagePointCount = 0;
            private int pointCloudCount = 0;

            @Override
            protected Boolean doInBackground(Void... voids) {
                averagePointCount = artifactResultRepository.getAveragePointCount(measureId, scanStep);
                pointCloudCount = artifactResultRepository.getPointCloudCount(measureId, scanStep);

                return true;
            }

            @SuppressLint("DefaultLocale")
            public void onPostExecute(Boolean results) {
                double lightScore = (Math.abs(averagePointCount / 38000 - 1.0) * 3);

                double durationScore;
                if (scanStep % 100 == 1)
                    durationScore = Math.abs(1- Math.abs((double) pointCloudCount / 24 - 1));
                else
                    durationScore = Math.abs(1- Math.abs((double) pointCloudCount / 8 - 1));

                if (lightScore > 1) lightScore -= 1;
                if (durationScore > 1) durationScore -= 1;

                Log.e("ScanQuality", String.valueOf(lightScore));
                Log.e("DurationQuality", String.valueOf(durationScore));

                String issues = "Scan Quality :";
                issues = String.format("%s\n - Light Score : %d%%", issues, Math.round(lightScore * 100));
                issues = String.format("%s\n - Duration Score : %d%%", issues, Math.round(durationScore * 100));

                if (scanStep == SCAN_STANDING_FRONT || scanStep == SCAN_LYING_FRONT) {
                    btnScanStep1.setVisibility(View.GONE);

                    if (pointCloudCount < 8) {
                        issues = String.format("%s\n - Duration was too short", issues);
                    } else if (pointCloudCount > 9) {
                        issues = String.format("%s\n - Duration was too long", issues);
                    }

                    txtScanStep1.setText(issues);
                    imgScanStep1.setVisibility(View.GONE);
                    lytScanAgain1.setVisibility(View.VISIBLE);

                    step1 = true;
                } else if (scanStep == SCAN_STANDING_SIDE || scanStep == SCAN_LYING_SIDE) {
                    btnScanStep2.setVisibility(View.GONE);

                    if (pointCloudCount < 12) {
                        issues = String.format("%s\n - Duration was too short", issues);
                    } else if (pointCloudCount > 27) {
                        issues = String.format("%s\n - Duration was too long", issues);
                    }

                    txtScanStep2.setText(issues);
                    imgScanStep2.setVisibility(View.GONE);
                    lytScanAgain2.setVisibility(View.VISIBLE);

                    step2 = true;
                } else if (scanStep == SCAN_STANDING_BACK || scanStep == SCAN_LYING_BACK) {
                    btnScanStep3.setVisibility(View.GONE);

                    if (pointCloudCount < 8) {
                        issues = String.format("%s\n - Duration was too short", issues);
                    } else if (pointCloudCount > 9) {
                        issues = String.format("%s\n - Duration was too long", issues);
                    }

                    txtScanStep3.setText(issues);
                    imgScanStep3.setVisibility(View.GONE);
                    lytScanAgain3.setVisibility(View.VISIBLE);

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
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

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

                    Intent intent = new Intent(this, AddressService.class);
                    intent.putExtra("add_receiver", receiver);
                    intent.putExtra("add_location", loc);
                    startService(intent);
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
            Toast.makeText(ScanModeActivity.this, "Storage permission needed!", Toast.LENGTH_SHORT).show();
            finish();
        }
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
                    if (mProgress > 0) {
                        resumeScan();
                    } else {
                        startScan();
                    }
                }
                break;
            case R.id.imgClose:
                closeScan();
                break;
            case R.id.btnRetake:
                mProgress = 0;
                break;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        surfaceCreated = true;

        // Set GL clear color to black.
        GLES20.glClearColor(0f, 0f, 0.5f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the camera preview image texture. Used in non-AR and AR mode.
            backgroundRenderer.createOnGlThread(this);
            occlusionRenderer.createOnGlThread(this);

            openCamera();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // Use the cGL clear color specified in onSurfaceCreated() to erase the GL surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (!shouldUpdateSurfaceTexture.get()) {
            // Not ready to draw.
            return;
        }

        // Handle display rotations.
        displayRotationHelper.updateSessionIfNeeded(sharedSession);

        try {
            onDrawFrameCamera2();
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    public void onDrawFrameCamera2() {
        SurfaceTexture texture = sharedCamera.getSurfaceTexture();

        // Ensure the surface is attached to the GL context.
        if (!isGlAttached) {
            texture.attachToGLContext(backgroundRenderer.getTextureId());
            isGlAttached = true;
        }

        // Update the surface.
        texture.updateTexImage();

        // Account for any difference between camera sensor orientation and display orientation.
        int rotationDegrees = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId);

        // Determine size of the camera preview image.
        Size size = sharedSession.getCameraConfig().getTextureSize();

        // Determine aspect ratio of the output GL surface, accounting for the current display rotation
        // relative to the camera sensor orientation of the device.
        float displayAspectRatio =
                displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId);

        // Render camera preview image to the GL surface.
        //backgroundRenderer.draw(size.getWidth(), size.getHeight(), displayAspectRatio, rotationDegrees);
        occlusionRenderer.draw(true);
    }

    private void openCamera() {
        // Don't open camera if already opened.
        if (cameraDevice != null) {
            return;
        }

        // Verify CAMERA_PERMISSION has been granted.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }

        // Make sure that ARCore is installed, up to date, and supported on this device.
        if (!isARCoreSupportedAndUpToDate()) {
            return;
        }

        if (sharedSession == null) {
            try {
                // Create ARCore session that supports camera sharing.
                sharedSession = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
            } catch (UnavailableException e) {
                Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
                return;
            }

            // Enable auto focus mode while ARCore is running.
            Config config = sharedSession.getConfig();
            config.setFocusMode(Config.FocusMode.AUTO);
            sharedSession.configure(config);
        }

        // Store the ARCore shared camera reference.
        sharedCamera = sharedSession.getSharedCamera();

        // Store the ID of the camera used by ARCore.
        cameraId = sharedSession.getCameraConfig().getCameraId();


        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanModeActivity.this);
                    String[] res = occlusionRenderer.getResolutions(ScanModeActivity.this, cameraId).toArray(new String[0]);

                    if (res.length > 0)
                    {
                        builder.setTitle("Choose ToF resolution");
                        builder.setItems(res, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openCamera(which);
                                initialized = true;
                            }
                        });
                    } else {
                        builder.setTitle("Camera2 API: ToF not found");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        });
                    }

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }).start();
    }

    private void openCamera(int index) {

        occlusionRenderer.initCamera(this, cameraId, index);

        // Use the currently configured CPU image size.
        cpuImageReader = ImageReader.newInstance(occlusionRenderer.getDepthWidth(), occlusionRenderer.getDepthHeight(), ImageFormat.DEPTH16, 2);
        cpuImageReader.setOnImageAvailableListener(this, backgroundHandler);

        // When ARCore is running, make sure it also updates our CPU image surface.
        sharedCamera.setAppSurfaces(this.cameraId, Arrays.asList(cpuImageReader.getSurface()));

        try {

            // Wrap our callback in a shared camera callback.
            CameraDevice.StateCallback wrappedCallback =
                    sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler);

            // Store a reference to the camera system service.
            // Reference to the camera system service.
            CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

            // Get the characteristics for the ARCore camera.
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.cameraId);

            // On Android P and later, get list of keys that are difficult to apply per-frame and can
            // result in unexpected delays when modified during the capture session lifetime.
            if (Build.VERSION.SDK_INT >= 28) {
                keysThatCanCauseCaptureDelaysWhenModified = characteristics.getAvailableSessionKeys();
                if (keysThatCanCauseCaptureDelaysWhenModified == null) {
                    // Initialize the list to an empty list if getAvailableSessionKeys() returns null.
                    keysThatCanCauseCaptureDelaysWhenModified = new ArrayList<>();
                }
            }

            // Prevent app crashes due to quick operations on camera open / close by waiting for the
            // capture session's onActive() callback to be triggered.
            captureSessionChangesPossible = false;

            // Open the camera device using the ARCore wrapped callback.
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            waitUntilCameraCaptureSessionIsActive();
            safeToExitApp.close();
            cameraDevice.close();
            safeToExitApp.block();
        }
        if (cpuImageReader != null) {
            cpuImageReader.close();
            cpuImageReader = null;
        }
    }

    private void resumeCamera2() {
        setRepeatingCaptureRequest();
        sharedCamera.getSurfaceTexture().setOnFrameAvailableListener(this);
    }

    private void setRepeatingCaptureRequest() {
        try {
            captureSession.setRepeatingRequest(
                    previewCaptureRequestBuilder.build(), captureSessionCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set repeating request", e);
        }
    }

    private synchronized void waitUntilCameraCaptureSessionIsActive() {
        while (!captureSessionChangesPossible) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
            }
        }
    }

    private boolean isARCoreSupportedAndUpToDate() {
        // Make sure ARCore is installed and supported on this device.
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        switch (availability) {
            case SUPPORTED_INSTALLED:
                break;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try {
                    // Request ARCore installation or update if needed.
                    ArCoreApk.InstallStatus installStatus =
                            ArCoreApk.getInstance().requestInstall(this, /*userRequestedInstall=*/ true);
                    switch (installStatus) {
                        case INSTALL_REQUESTED:
                            Log.e(TAG, "ARCore installation requested.");
                            return false;
                        case INSTALLED:
                            break;
                    }
                } catch (UnavailableException e) {
                    Log.e(TAG, "ARCore not installed", e);
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "ARCore not installed\n" + e, Toast.LENGTH_LONG).show());
                    finish();
                    return false;
                }
                break;
            case UNKNOWN_ERROR:
            case UNKNOWN_CHECKING:
            case UNKNOWN_TIMED_OUT:
            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                Log.e(
                        TAG,
                        "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned "
                                + availability);
                runOnUiThread(
                        () ->
                                Toast.makeText(
                                        getApplicationContext(),
                                        "ARCore is not supported on this device, "
                                                + "ArCoreApk.checkAvailability() returned "
                                                + availability,
                                        Toast.LENGTH_LONG)
                                        .show());
                return false;
        }
        return true;
    }

    private void createCameraPreviewSession() {
        try {
            // Note that isGlAttached will be set to true in AR mode in onDrawFrame().
            sharedSession.setCameraTextureName(backgroundRenderer.getTextureId());
            sharedCamera.getSurfaceTexture().setOnFrameAvailableListener(this);

            // Create an ARCore compatible capture request using `TEMPLATE_RECORD`.
            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Build surfaces list, starting with ARCore provided surfaces.
            List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();

            // Add a CPU image reader surface. On devices that don't support CPU image access, the image
            // may arrive significantly later, or not arrive at all.
            surfaceList.add(cpuImageReader.getSurface());

            // Surface list should now contain three surfaces:
            // 0. sharedCamera.getSurfaceTexture()
            // 1. …
            // 2. cpuImageReader.getSurface()

            // Add ARCore surfaces and CPU image surface targets.
            for (Surface surface : surfaceList) {
                previewCaptureRequestBuilder.addTarget(surface);
            }

            // Wrap our callback in a shared camera callback.
            CameraCaptureSession.StateCallback wrappedCallback = sharedCamera.createARSessionStateCallback(cameraCaptureCallback, backgroundHandler);

            // Create camera capture session for camera preview using ARCore wrapped callback.
            cameraDevice.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.w(TAG, "onImageAvailable: Skipping null image.");
            return;
        }

        Image.Plane plane = image.getPlanes()[0];
        ShortBuffer shortDepthBuffer = plane.getBuffer().asShortBuffer();
        ArrayList<Short> pixel = new ArrayList<Short>();
        while (shortDepthBuffer.hasRemaining()) {
            pixel.add(shortDepthBuffer.get());
        }
        int stride = plane.getRowStride();

        int offset = 0;
        float[] output = new float[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int depthSample = pixel.get((int)(y / 2) * stride + x);
                int depthRange = (depthSample & 0x1FFF);
                int depthConfidence = ((depthSample >> 13) & 0x7);
                float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;
                output[offset + x] = 0.0001f * depthRange;
            }
            offset += image.getWidth();
        }
        image.close();

        occlusionRenderer.update(output);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    @SuppressLint("StaticFieldLeak")
    class SaveMeasureTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;

        SaveMeasureTask(Activity act) {
            activity = act;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Gson gson = new Gson();

            measureRepository.insertMeasure(measure);

            try {
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(AppController.getInstance().getAzureConnection());
                CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

                try {
                    if (!measure.isArtifact_synced()) {
                        CloudQueue measureArtifactsQueue = queueClient.getQueueReference("artifact-list");
                        measureArtifactsQueue.createIfNotExists();

                        long totalNumbers  = fileLogRepository.getTotalArtifactCountForMeasure(measure.getId());
                        final int size = 50;
                        int offset = 0;

                        while (offset + 1 < totalNumbers) {
                            List<FileLog> measureArtifacts = fileLogRepository.getArtifactsForMeasure(measure.getId(), offset, size);

                            ArtifactList artifactList = new ArtifactList();
                            artifactList.setMeasure_id(measure.getId());
                            artifactList.setStart(offset + 1);
                            artifactList.setEnd(offset + measureArtifacts.size());
                            artifactList.setArtifacts(measureArtifacts);
                            artifactList.setTotal(totalNumbers);

                            offset += measureArtifacts.size();

                            CloudQueueMessage measureArtifactsMessage = new CloudQueueMessage(measure.getId());
                            measureArtifactsMessage.setMessageContent(gson.toJson(artifactList));
                            measureArtifactsQueue.addMessage(measureArtifactsMessage);
                        }

                        measure.setArtifact_synced(true);
                    }

                    CloudQueue measureQueue = queueClient.getQueueReference("measure");
                    measureQueue.createIfNotExists();

                    CloudQueueMessage message = new CloudQueueMessage(measure.getId());
                    message.setMessageContent(gson.toJson(message));
                    measureQueue.addMessage(message);

                    measure.setTimestamp(session.getSyncTimestamp());
                    measureRepository.updateMeasure(measure);
                } catch (StorageException e) {
                    e.printStackTrace();
                }
            } catch (URISyntaxException | InvalidKeyException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void onPostExecute(Void result) {
            activity.finish();
        }
    }
}
