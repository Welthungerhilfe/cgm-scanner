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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactList;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.camera2.Camera2Manager;
import de.welthungerhilfe.cgm.scanner.helper.receiver.AddressReceiver;
import de.welthungerhilfe.cgm.scanner.helper.service.AddressService;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_SIDE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_PREVIEW;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_SIDE;

public class ScanModeActivity extends AppCompatActivity implements View.OnClickListener, Camera2Manager.CameraDataListener {
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

    private ImageView mColorCameraPreview;
    private ImageView mDepthCameraPreview;

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

    private final Camera2Manager mCamera2Manager = new Camera2Manager();

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

        mColorCameraPreview = findViewById(R.id.colorCameraPreview);
        mColorCameraPreview.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));
        mDepthCameraPreview = findViewById(R.id.depthCameraPreview);
        mDepthCameraPreview.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));

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
        mCamera2Manager.openCamera(this, PERMISSION_CAMERA);
        mCamera2Manager.addListener(this);
    }

    @Override
    protected void onPause() {
        mCamera2Manager.removeListener(this);
        mCamera2Manager.closeCamera();
        super.onPause();
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
    public void OnColorDataReceived(Bitmap bitmap) {
        if (mIsRecording) {
            File dir = new File(AppController.getInstance().getRootDirectory(), "rgb/");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File out = new File(dir, String.format("out%d.jpg", System.currentTimeMillis()));
            try {
                FileOutputStream fOutputStream = new FileOutputStream(out);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream);

                fOutputStream.flush();
                fOutputStream.close();

                MediaStore.Images.Media.insertImage(getContentResolver(), out.getAbsolutePath(), out.getName(), out.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        float scale = bitmap.getWidth() / (float)bitmap.getHeight();
        mColorCameraPreview.setImageBitmap(bitmap);
        mColorCameraPreview.setRotation(90);
        mColorCameraPreview.setScaleX(scale);
        mColorCameraPreview.setScaleY(scale);
        mDepthCameraPreview.setRotation(90);
        mDepthCameraPreview.setScaleX(scale);
        mDepthCameraPreview.setScaleY(scale);
    }

    @Override
    public void OnDepthDataReceived(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();

        if (mIsRecording) {
            File dir = new File(AppController.getInstance().getRootDirectory(), "depth16/");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File out = new File(dir, String.format("out%d.depth16", System.currentTimeMillis()));
            try {
                FileOutputStream stream = new FileOutputStream(out);
                FileChannel channel = stream.getChannel();
                channel.write(buffer);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Short> pixel = new ArrayList<>();
        while (shortDepthBuffer.hasRemaining()) {
            pixel.add(shortDepthBuffer.get());
        }
        int stride = plane.getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();

        int[] output = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int depthSample = pixel.get((y / 2) * stride + x);
                float depth = 0.001f * (depthSample & 0x1FFF);
                float r = Math.min(depth * 1.0f, 1);
                float g = Math.min(depth * 2.0f, 1);
                float b = Math.min(depth * 3.0f, 1);
                output[y * width + x] = Color.argb(0.5f, r, g, b);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(output, 0, width, 0, 0, width, height);
        mDepthCameraPreview.setImageBitmap(bitmap);
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
