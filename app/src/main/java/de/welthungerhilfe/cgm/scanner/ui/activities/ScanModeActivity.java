package de.welthungerhilfe.cgm.scanner.ui.activities;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.microsoft.appcenter.crashes.Crashes;
import com.projecttango.tangosupport.TangoSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver;
import de.welthungerhilfe.cgm.scanner.helper.service.AddressService;
import de.welthungerhilfe.cgm.scanner.helper.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.helper.tango.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.helper.tango.OverlaySurface;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.MD5;
import de.welthungerhilfe.cgm.scanner.utils.TangoUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static com.projecttango.tangosupport.TangoSupport.initialize;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_SIDE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_PREVIEW;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_SIDE;

public class ScanModeActivity extends BaseActivity implements View.OnClickListener {
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
        measure.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
        measure.setDate(Utils.getUniversalTimestamp());
        measure.setType("v1.1.2");
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

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private GLSurfaceView mCameraSurfaceView;
    //private OverlaySurface mOverlaySurfaceView;
    private CameraSurfaceRenderer mRenderer;

    private TextView mTitleView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    // variables for Pose and point clouds
    private float mDeltaTime;
    private int mValidPoseCallbackCount;
    private int mPointCloudCallbackCount;
    private boolean mTimeToTakeSnap;
    private String mPointCloudFilename;
    private int mNumberOfFilesWritten;
    private ArrayList<float[]> mPosePositionBuffer;
    private ArrayList<float[]> mPoseOrientationBuffer;
    private ArrayList<Float> mPoseTimestampBuffer;
    private ArrayList<String> mPointCloudFilenameBuffer;
    private float[] cam2dev_Transform;
    private int mNumPoseInSequence;
    private int mPreviousPoseStatus;
    private float mPosePreviousTimeStamp;
    private float mPointCloudPreviousTimeStamp;
    private float mCurrentTimeStamp;

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

    public void onStart() {
        super.onStart();

        mPointCloudFilename = "";
        mNumberOfFilesWritten = 0;
        mPosePositionBuffer = new ArrayList<>();
        mPoseOrientationBuffer = new ArrayList<>();
        mPoseTimestampBuffer = new ArrayList<>();
        mPointCloudFilenameBuffer = new ArrayList<>();
        mNumPoseInSequence = 0;
        mutex_on_mIsRecording = new Semaphore(1,true);
        mIsRecording = false;
        mPointCloudAvailable = false;

        mNowTime = System.currentTimeMillis();
        mNowTimeString = String.valueOf(mNowTime);

        mTango = new Tango(this, () -> {
            // Synchronize against disconnecting while the service is being used in
            // the OpenGL thread or in the UI thread.
            synchronized (this) {
                try {
                    mConfig = setupTangoConfig(mTango);
                    mTango.connect(mConfig);
                    startupTango();
                    initialize(mTango);
                    mIsConnected = true;

                    setDisplayRotation();
                } catch (TangoOutOfDateException e) {
                    Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    Crashes.trackError(e);
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.exception_tango_error), e);
                    Crashes.trackError(e);
                } catch (TangoInvalidException e) {
                    Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                    Crashes.trackError(e);
                }
                setUpExtrinsics();
            }
        });
    }

    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);

        if (person == null) {
            Toast.makeText(this, "Person was not defined", Toast.LENGTH_LONG).show();
            finish();
        }

        age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;

        if (measure == null) {
            measure = new Measure();
            measure.setId(AppController.getInstance().getMeasureId());
            measure.setQrCode(person.getQrcode());
            measure.setCreatedBy(AppController.getInstance().firebaseUser.getEmail());
            measure.setAge(age);
            measure.setDate(System.currentTimeMillis());
        }

        setContentView(R.layout.activity_scan_mode);

        ButterKnife.bind(this);

        mTitleView = findViewById(R.id.txtTitle);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);

        findViewById(R.id.btnRetake).setOnClickListener(this);
        findViewById(R.id.imgClose).setOnClickListener(this);

        mCameraSurfaceView = findViewById(R.id.surfaceview);
        //mOverlaySurfaceView = findViewById(R.id.overlaySurfaceView);

        measureRepository = MeasureRepository.getInstance(this);
        fileLogRepository = FileLogRepository.getInstance(this);
        artifactResultRepository = ArtifactResultRepository.getInstance(this);

        setupToolbar();

        getCurrentLocation();

        setupScanArtifacts();
        setupRenderer();

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

        mCameraSurfaceView.onResume();
        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        /*
        if (SCAN_STEP == SCAN_STANDING_FRONT || SCAN_STEP == SCAN_STANDING_SIDE || SCAN_STEP == SCAN_STANDING_BACK)
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_CLOSE_DOWN_UP_OVERLAY);
        else if (SCAN_STEP == SCAN_LYING_FRONT || SCAN_STEP == SCAN_LYING_SIDE || SCAN_STEP == SCAN_LYING_BACK)
            mOverlaySurfaceView.setMode(OverlaySurface.BABY_OVERLAY);
         */
    }

    @Override
    protected void onPause() {
        super.onPause();

        mCameraSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume.
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
                Crashes.trackError(e);
            }
        }
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

    private void setupRenderer() {
        mCameraSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new CameraSurfaceRenderer(new CameraSurfaceRenderer.RenderCallback() {

            @Override
            public void preRender() {

                // This is the work that you would do on your main OpenGL render thread.

                // We need to be careful to not run any Tango-dependent code in the OpenGL
                // thread unless we know the Tango Service to be properly set up and connected.
                if (!mIsConnected) {
                    return;
                }

                try {
                    // Synchronize against concurrently disconnecting the service triggered from the
                    // UI thread.
                    synchronized (this) {
                        // Connect the Tango SDK to the OpenGL texture ID where we are going to
                        // render the camera.
                        // NOTE: This must be done after the texture is generated and the Tango
                        // service is connected.
                        if (mConnectedTextureIdGlThread == INVALID_TEXTURE_ID) {
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            //sVideoEncoder.setTextureId(mConnectedTextureIdGlThread);
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, mConnectedTextureIdGlThread);

                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture and
                        // scene camera pose.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {

                            double rgbTimestamp = mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

                            // {@code rgbTimestamp} contains the exact timestamp at which the
                            // rendered RGB frame was acquired.

                            // In order to see more details on how to use this timestamp to modify
                            // the scene camera and achieve an augmented reality effect,
                            // refer to java_augmented_reality_example and/or
                            // java_augmented_reality_opengl_example projects.

                        }
                    }
                } catch (TangoErrorException e) {
                    Crashes.trackError(e);
                }
            }
        });

        mCameraSurfaceView.setRenderer(mRenderer);
    }

    private TangoConfig setupTangoConfig(Tango tango) {
        // Create a new Tango configuration and enable the Camera API.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    private void startupTango() {
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            String[] poseStatusCode = {"POSE_INITIALIZING","POSE_VALID","POSE_INVALID","POSE_UNKNOWN"};

            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                mDeltaTime = (float) (pose.timestamp - mPosePreviousTimeStamp) * SECS_TO_MILLISECS;
                mPosePreviousTimeStamp = (float) pose.timestamp;
                if (mPreviousPoseStatus != pose.statusCode) {
                    mValidPoseCallbackCount = 0;
                }
                mValidPoseCallbackCount++;
                mPreviousPoseStatus = pose.statusCode;

                // My pose buffering
                if (mIsRecording && pose.statusCode == TangoPoseData.POSE_VALID) {
                    mPosePositionBuffer.add(mNumPoseInSequence, pose.getTranslationAsFloats());
                    mPoseOrientationBuffer.add(mNumPoseInSequence, pose.getRotationAsFloats());
                    mPoseTimestampBuffer.add((float)pose.timestamp);
                    mNumPoseInSequence++;
                }
                //End of My pose buffering
            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) throws TangoErrorException {

                Log.d(TAG, "recording:"+mIsRecording);
                // set to true for next RGB image to be written
                // TODO remove when not necessary anymore (performance/video capture)
                mPointCloudAvailable = true;

                float[] average = TangoUtils.calculateAveragedDepth(pointCloudData.points, pointCloudData.numPoints);

                /*
                mOverlaySurfaceView.setNumPoints(pointCloudData.numPoints);
                mOverlaySurfaceView.setDistance(average[0]);
                mOverlaySurfaceView.setConfidence(average[1]);
                 */

                // Get pose transforms for openGL to depth/color cameras.
                TangoPoseData oglTdepthPose = TangoSupport.getPoseAtTime(
                        pointCloudData.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
                if (oglTdepthPose.statusCode != TangoPoseData.POSE_VALID) {
                    //Log.w(TAG, "Could not get depth camera transform at time " + pointCloudData.timestamp);
                }

                mCurrentTimeStamp = (float) pointCloudData.timestamp;
                final float frameDelta = (mCurrentTimeStamp - mPointCloudPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                mPointCloudCallbackCount++;

                // My writing to file function


                // Background task for writing to file
                // TODO refactor to top-level class or make static?
                Runnable thread = () -> {
                    try {
                        mutex_on_mIsRecording.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Crashes.trackError(e);
                    }
                    // Saving the frame or not, depending on the current mode.
                    if ( mIsRecording ) {
                        // TODO save files to local storage
                        updateScanningProgress(pointCloudData.numPoints, average[0], average[1]);
                        progressBar.setProgress(mProgress);

                        mPointCloudFilename = "pc_" + person.getQrcode() + "_" + mNowTimeString + "_" + SCAN_STEP +
                                "_" + String.format(Locale.getDefault(), "%03d", mNumberOfFilesWritten);
                        TangoUtils.writePointCloudToPcdFile(pointCloudData, mPointCloudSaveFolder, mPointCloudFilename);

                        File artefactFile = new File(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename +".pcd");
                        FileLog log = new FileLog();
                        log.setId(AppController.getInstance().getArtifactId("scan-pcd", mNowTime));
                        log.setType("pcd");
                        log.setPath(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename + ".pcd");
                        log.setHashValue(MD5.getMD5(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename +".pcd"));
                        log.setFileSize(artefactFile.length());
                        log.setUploadDate(0);
                        log.setDeleted(false);
                        log.setQrCode(person.getQrcode());
                        log.setCreateDate(mNowTime);
                        log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                        log.setAge(age);
                        log.setSchema_version(CgmDatabase.version);
                        log.setMeasureId(measure.getId());

                        fileLogRepository.insertFileLog(log);

                        ArtifactResult ar=new ArtifactResult();
                        double Artifact_Lighting_penalty=Math.abs((double) noOfPoints/38000-1.0)*100*3;
                        ar.setConfidence_value(String.valueOf(100-Artifact_Lighting_penalty));
                        ar.setArtifact_id(AppController.getInstance().getPersonId());
                        ar.setKey(SCAN_STEP);
                        ar.setMeasure_id(measure.getId());
                        ar.setMisc("");
                        ar.setType("PCD_POINTS_v0.2");
                        noOfPoints = pointCloudData.numPoints;
                        ar.setReal(noOfPoints);
                        artifactResultRepository.insertArtifactResult(ar);
                        // Todo;
                        //new OfflineTask().saveFileLog(log);
                        // Direct Upload to Firebase Storage
                        mNumberOfFilesWritten++;
                        double Scan_Duration_Penalty=Math.abs((double)mNumberOfFilesWritten/8-1)*100;

                        Log.d("Prajwal",String.valueOf(mNumberOfFilesWritten));
                    }
                    mutex_on_mIsRecording.release();
                };
                thread.run();
            }


            @Override
            public void onFrameAvailable(int cameraId) {
                // This will get called every time a new RGB camera frame is available to be
                // rendered.
                //Log.d(TAG, "onFrameAvailable");

                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Now that we are receiving onFrameAvailable callbacks, we can switch
                    // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
                    // This will result in a frame rate of approximately 30FPS, in synchrony with
                    // the RGB camera driver.
                    // If you need to render at a higher rate (i.e., if you want to render complex
                    // animations smoothly) you  can use RENDERMODE_CONTINUOUSLY throughout the
                    // application lifecycle.
                    if (mCameraSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }

                    // Note that the RGB data is not passed as a parameter here.
                    // Instead, this callback indicates that you can call
                    // the {@code updateTexture()} method to have the
                    // RGB data copied directly to the OpenGL texture at the native layer.
                    // Since that call needs to be done from the OpenGL thread, what we do here is
                    // set up a flag to tell the OpenGL thread to do that in the next run.
                    // NOTE: Even if we are using a render-by-request method, this flag is still
                    // necessary since the OpenGL thread run requested below is not guaranteed
                    // to run in synchrony with this requesting call.
                    mIsFrameAvailableTangoThread.set(true);
                    // Trigger an OpenGL render to update the OpenGL scene with the new RGB data.
                    mCameraSurfaceView.requestRender();
                }
            }
        });

        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, (tangoImageBuffer, i) -> {
            if ( ! mIsRecording || ! mPointCloudAvailable) {
                return;
            }

            Runnable thread = () -> {
                TangoImageBuffer currentTangoImageBuffer = TangoUtils.copyImageBuffer(tangoImageBuffer);

                // TODO save files to local storage
                String currentImgFilename = "rgb_" + person.getQrcode() +"_" + mNowTimeString + "_" + SCAN_STEP + "_" + currentTangoImageBuffer.timestamp + ".jpg";

                BitmapUtils.writeImageToFile(currentTangoImageBuffer, mRgbSaveFolder, currentImgFilename);

                File artefactFile = new File(mRgbSaveFolder.getPath() + File.separator + currentImgFilename);
                FileLog log = new FileLog();
                log.setId(AppController.getInstance().getArtifactId("scan-rgb", mNowTime));
                log.setType("rgb");
                log.setPath(mRgbSaveFolder.getPath() + File.separator + currentImgFilename);
                log.setHashValue(MD5.getMD5(mRgbSaveFolder.getPath() + File.separator + currentImgFilename));
                log.setFileSize(artefactFile.length());
                log.setUploadDate(0);
                log.setDeleted(false);
                log.setQrCode(person.getQrcode());
                log.setCreateDate(mNowTime);
                log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                log.setAge(age);
                log.setSchema_version(CgmDatabase.version);
                log.setMeasureId(measure.getId());

                fileLogRepository.insertFileLog(log);
            };
            thread.run();
        });
    }

    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mCameraSurfaceView.queueEvent(() -> {
            if (mIsConnected) {
                mRenderer.updateColorCameraTextureUv(mDisplayRotation);
            }
        });
    }

    private void setUpExtrinsics() {
        // Set device to imu matrix in Model Matrix Calculator.
        TangoPoseData device2IMUPose = new TangoPoseData();
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        try {
            device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            e.printStackTrace();
            Crashes.trackError(e);
        }
        /*mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(device2IMUPose.getTranslationAsFloats(),device2IMUPose.getRotationAsFloats());*/
        // Set color camera to imu matrix in Model Matrix Calculator.
        TangoPoseData color2IMUPose = new TangoPoseData();

        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        try {
            color2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            e.printStackTrace();
            Crashes.trackError(e);
        }

        //mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(color2IMUPose.getTranslationAsFloats(), color2IMUPose.getRotationAsFloats());

        // Get the Camera2Device transform
        float[] rot_Dev2IMU = device2IMUPose.getRotationAsFloats();
        float[] trans_Dev2IMU = device2IMUPose.getTranslationAsFloats();
        float[] rot_Cam2IMU = color2IMUPose.getRotationAsFloats();
        float[] trans_Cam2IMU = color2IMUPose.getTranslationAsFloats();

        float[] dev2IMU = new float[16];
        Matrix.setIdentityM(dev2IMU, 0);
        dev2IMU = ModelMatCalculator.quaternionMatrixOpenGL(rot_Dev2IMU);
        dev2IMU[12] += trans_Dev2IMU[0];
        dev2IMU[13] += trans_Dev2IMU[1];
        dev2IMU[14] += trans_Dev2IMU[2];

        float[] IMU2dev = new float[16];
        Matrix.setIdentityM(IMU2dev, 0);
        Matrix.invertM(IMU2dev, 0, dev2IMU, 0);

        float[] cam2IMU = new float[16];
        Matrix.setIdentityM(cam2IMU, 0);
        cam2IMU = ModelMatCalculator.quaternionMatrixOpenGL(rot_Cam2IMU);
        cam2IMU[12] += trans_Cam2IMU[0];
        cam2IMU[13] += trans_Cam2IMU[1];
        cam2IMU[14] += trans_Cam2IMU[2];

        cam2dev_Transform = new float[16];
        Matrix.setIdentityM(cam2dev_Transform, 0);
        Matrix.multiplyMM(cam2dev_Transform, 0, IMU2dev, 0, cam2IMU, 0);
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

        /*
        switch (SCAN_STEP) {
            case SCAN_STANDING_FRONT:
            case SCAN_LYING_FRONT:
                lytScanStep1.setVisibility(View.GONE);
                btnScanStep1.setText(R.string.retake_scan);
                btnScanStep1.setTextColor(getResources().getColor(R.color.colorWhite, getTheme()));
                btnScanStep1.setBackground(getResources().getDrawable(R.drawable.button_green_circular, getTheme()));

                step1 = true;
                break;
            case SCAN_STANDING_SIDE:
            case SCAN_LYING_SIDE:
                lytScanStep2.setVisibility(View.GONE);
                btnScanStep2.setText(R.string.retake_scan);
                btnScanStep2.setTextColor(getResources().getColor(R.color.colorWhite, getTheme()));
                btnScanStep2.setBackground(getResources().getDrawable(R.drawable.button_green_circular, getTheme()));

                step2 = true;
                break;
            case SCAN_STANDING_BACK:
            case SCAN_LYING_BACK:
                lytScanStep3.setVisibility(View.GONE);
                btnScanStep3.setText(R.string.retake_scan);
                btnScanStep3.setTextColor(getResources().getColor(R.color.colorWhite, getTheme()));
                btnScanStep3.setBackground(getResources().getDrawable(R.drawable.button_green_circular, getTheme()));

                step3 = true;
                break;
        }
        */

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
                if (scanStep % 100 == 1) durationScore = Math.abs(1 - Math.abs((double) pointCloudCount / 24 - 1));
                else durationScore = Math.abs(1- Math.abs((double) pointCloudCount / 8 - 1));

                if (lightScore > 1) lightScore -= 1;
                if (durationScore > 1) durationScore -= 1;

                Log.e("LightScore", String.valueOf(lightScore));
                Log.e("DurationScore", String.valueOf(durationScore));

                if (scanStep == SCAN_STANDING_FRONT || scanStep == SCAN_LYING_FRONT) {
                    btnScanStep1.setVisibility(View.GONE);

                    String issues = String.format(" - Light Score : %d%%", Math.round(lightScore * 100));
                    issues = String.format("%s\n - Duration score : %d%%", issues, Math.round(durationScore * 100));
                    if (pointCloudCount < 8) issues = String.format("%s\n - Duration was too short", issues);
                    else if (pointCloudCount > 9) issues = String.format("%s\n - Duration was too long", issues);

                    if (lightScore < 0.5 || durationScore < 0.5) {
                        txtScanStep1.setText(issues);
                        imgScanStep1.setVisibility(View.GONE);
                        lytScanAgain1.setVisibility(View.VISIBLE);
                    } else {
                        lytScanStep1.setVisibility(View.GONE);
                        btnScanStep1.setVisibility(View.GONE);
                        imgScanSuccess1.setVisibility(View.VISIBLE);
                    }

                    step1 = true;
                } else if (scanStep == SCAN_STANDING_SIDE || scanStep == SCAN_LYING_SIDE) {
                    btnScanStep2.setVisibility(View.GONE);

                    String issues = String.format(" - Light Score : %d%%", Math.round(lightScore * 100));
                    issues = String.format("%s\n - Duration score : %d%%", issues, Math.round(durationScore * 100));
                    if (pointCloudCount < 12) issues = String.format("%s\n - Duration was too short", issues);
                    else if (pointCloudCount > 27) issues = String.format("%s\n - Duration was too long", issues);

                    if (lightScore < 0.5 || durationScore < 0.5) {
                        txtScanStep2.setText(issues);
                        imgScanStep2.setVisibility(View.GONE);
                        lytScanAgain2.setVisibility(View.VISIBLE);
                    } else {
                        lytScanStep2.setVisibility(View.GONE);
                        btnScanStep2.setVisibility(View.GONE);
                        imgScanSuccess2.setVisibility(View.VISIBLE);
                    }

                    step2 = true;
                } else if (scanStep == SCAN_STANDING_BACK || scanStep == SCAN_LYING_BACK) {
                    btnScanStep3.setVisibility(View.GONE);

                    String issues = String.format(" - Light Score : %d%%", Math.round(lightScore * 100));
                    issues = String.format("%s\n - Duration score : %d%%", issues, Math.round(durationScore * 100));
                    if (pointCloudCount < 8) issues = String.format("%s\n - Duration was too short", issues);
                    else if (pointCloudCount > 9) issues = String.format("%s\n - Duration was too long", issues);

                    if (lightScore < 0.5 || durationScore < 0.5) {
                        txtScanStep3.setText(issues);
                        imgScanStep3.setVisibility(View.GONE);
                        lytScanAgain3.setVisibility(View.VISIBLE);
                    } else {
                        lytScanStep3.setVisibility(View.GONE);
                        btnScanStep3.setVisibility(View.GONE);
                        imgScanSuccess3.setVisibility(View.VISIBLE);
                    }

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

    @SuppressLint("StaticFieldLeak")
    class SaveMeasureTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;

        SaveMeasureTask(Activity act) {
            activity = act;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            measureRepository.insertMeasure(measure);
            return null;
        }

        public void onPostExecute(Void result) {
            activity.finish();
        }
    }
}
