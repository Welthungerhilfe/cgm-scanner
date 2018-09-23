
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

package de.welthungerhilfe.cgm.scanner.activities;

import android.Manifest;
import android.app.Activity;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.display.DisplayManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.AddTrace;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;
import com.projecttango.tangosupport.TangoSupport;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack0Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack1Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyFront0Fragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.helper.service.FileLogMonitorService;
import de.welthungerhilfe.cgm.scanner.helper.service.FirebaseUploadService;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.tango.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.tango.OverlaySurface;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.MD5;
import de.welthungerhilfe.cgm.scanner.utils.TangoUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static com.projecttango.tangosupport.TangoSupport.initialize;

public class RecorderActivity extends Activity {
    private final int PERMISSION_LOCATION = 0x0001;

    private static GLSurfaceView mCameraSurfaceView;
    private static OverlaySurface mOverlaySurfaceView;

    private TextView mDisplayTextView;
    private CameraSurfaceRenderer mRenderer;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private static final int SECS_TO_MILLISECS = 1000;


    private static final String TAG = RecorderActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final String sTimestampFormat = "Timestamp: %f";

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private int mDisplayRotation = Surface.ROTATION_0;

    private Semaphore mutex_on_mIsRecording;

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

    private boolean mPointCloudAvailable;
    private boolean mIsRecording;

    private Person person;
    private Measure measure;
    private LinearLayout container;
    private FloatingActionButton fab;
    private ProgressBar progressBar;

    private File mExtFileDir;
    private File mScanArtefactsOutputFolder;
    private String mPointCloudSaveFolderPath;
    private File mPointCloudSaveFolder;
    private File mRgbSaveFolder;
    private long mNowTime;
    private String mNowTimeString;
    private String mQrCode;

    // removed the infant scanning workflow, so we start directly with the standard workflow 100
    // no need anymore for choosing the workflow with step 0
    private int mScanningWorkflowStep = 101;

    private final String BABY_FRONT_0 = "baby_front_0";
    private final String BABY_FRONT_1 = "baby_front_1";
    private final String BABY_BACK_0 = "baby_back_0";
    private final String BABY_BACK_1 = "baby_back_1";
    private BabyFront0Fragment babyFront0Fragment;
    private BabyBack0Fragment babyBack0Fragment;
    private BabyBack1Fragment babyBack1Fragment;


    // TODO: make available in Settings
    private boolean onboarding = true;

    private boolean Verbose = true;
    private FirebaseAnalytics mFirebaseAnalytics;

    private int mProgress;

    private Loc location;

    // Workflow
    public void gotoNextStep(int babyInfantChoice) {
        mScanningWorkflowStep = babyInfantChoice+1;

        gotoNextStep();
    }
    public void gotoNextStep() {
        // mScanningWorkflowStep 0 = choose between infant standing up and baby lying down
        // mScanningWorkflowStep 100+ = baby
        // mScanningWorkflowStep 200+ = infant
        // onBoarding steps are odd, scanning process steps are even 0,2,4,6

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (Verbose) Log.v("ScanningWorkflow","starting mScanningWorkflowStep: "+ mScanningWorkflowStep);

        if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_FRONT_ONBOARDING)
        {
            if (measure == null)
                measure = new Measure();
            if (location != null)
                measure.setLocation(location);
            measure.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
            measure.setDate(mNowTime);

            babyFront0Fragment = new BabyFront0Fragment();
            ft.add(R.id.container, babyFront0Fragment);
            ft.commit();
            measure.setType("v1.1.1");
        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_FRONT_SCAN)
        {
            mOverlaySurfaceView.setMode(OverlaySurface.BABY_OVERLAY);
            mDisplayTextView.setText(R.string.baby_full_body_front_scan_text);
            resumeScan();

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_FRONT_RECORDING)
        {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_LEFT_RIGHT_ONBOARDING)
        {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            babyBack0Fragment = new BabyBack0Fragment();
            ft.replace(R.id.container, babyBack0Fragment, BABY_BACK_0);
            ft.commit();
            pauseScan();

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_LEFT_RIGHT_SCAN)
        {
            mDisplayTextView.setText(R.string.baby_left_right_scan_text);
            resumeScan();

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_LEFT_RIGHT_RECORDING)
        {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_BACK_ONBOARDING)
        {
            mDisplayTextView.setText(R.string.empty_string);
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
            babyBack1Fragment = new BabyBack1Fragment();
            ft.replace(R.id.container, babyBack1Fragment, BABY_BACK_1);
            ft.commit();
            pauseScan();

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_BACK_SCAN)
        {
            mDisplayTextView.setText(R.string.baby_full_body_back_scan_text);
            resumeScan();

        }
        else if (mScanningWorkflowStep ==     AppConstants.BABY_FULL_BODY_BACK_RECORDING)
        {
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPink)));
            mIsRecording = true;

        } else {
            Log.v(TAG,"ScanningWorkflow finished for person "+person.getSurname());

            //TODO: set data and location async
            // location: https://developer.android.com/training/location/retrieve-current.html
            measure.setDate(System.currentTimeMillis());
            long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
            measure.setAge(age);
            measure.setType(AppConstants.VAL_MEASURE_AUTO);
            measure.setWeight(0.0f);
            measure.setHeight(0.0f);
            measure.setHeadCircumference(0.0f);
            measure.setMuac(0.0f);
            measure.setOedema(false);
            if (measure.getId() == null)
                EventBus.getDefault().post(new MeasureResult(measure));
            else
                new OfflineRepository().updateMeasure(measure);
            finish();
        }
        mScanningWorkflowStep++;
        if (!onboarding) mScanningWorkflowStep++;
        if (Verbose) Log.v("ScanningWorkflow","next mScanningWorkflowStep: "+ mScanningWorkflowStep);
    }

    private ViewHolder scanDialogViewHolder;
    private DialogPlus scanResultDialog;

    // TODO: show repeat/next dialog after each scan - especially when onboarding=false
    // TODO: make the user see the gathered point clouds
    private void showScanResultDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txtHeight = scanResultDialog.getHolderView().findViewById(R.id.txtHeight);

                txtHeight.setText(Integer.toString(24));

                scanResultDialog.show();
            }
        });
    }

    private void pauseScan() {
        container.setVisibility(View.VISIBLE);
        mCameraSurfaceView.setVisibility(View.INVISIBLE);
        mOverlaySurfaceView.setVisibility(View.INVISIBLE);
        mDisplayTextView.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void resumeScan() {
        container.setVisibility(View.INVISIBLE);
        mCameraSurfaceView.setVisibility(View.VISIBLE);
        mOverlaySurfaceView.setVisibility(View.VISIBLE);
        mDisplayTextView.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);

        mProgress = 0;
        progressBar.setProgress(mProgress);
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Overridable  method to get layout id.  Any provided layout needs to include
     * the same views (or compatible) as active_play_movie_surface
     *
     */
    protected int getContentViewId() {
        return R.layout.activity_recorder;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);
        if (person == null) Log.e(TAG,"person was null!");
        setContentView(getContentViewId());

        // start Workflow
        gotoNextStep();

        mCameraSurfaceView = findViewById(R.id.surfaceview);
        mOverlaySurfaceView = findViewById(R.id.overlaySurfaceView);
        mDisplayTextView = findViewById(R.id.display_textview);
        container = findViewById(R.id.container);



        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        fab = findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsRecording) {
                    mIsRecording = false;
                    record_SwitchChanged();
                    fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorGreen)));
                }
                gotoNextStep();
            }
        });

        progressBar = findViewById(R.id.progressBar);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }

        mPointCloudFilename = "";
        mNumberOfFilesWritten = 0;
        mPosePositionBuffer = new ArrayList<float[]>();
        mPoseOrientationBuffer = new ArrayList<float[]>();
        mPoseTimestampBuffer = new ArrayList<Float>();
        mPointCloudFilenameBuffer = new ArrayList<String>();
        mNumPoseInSequence = 0;
        mutex_on_mIsRecording = new Semaphore(1,true);
        mIsRecording = false;
        mPointCloudAvailable = false;

        mNowTime = System.currentTimeMillis();
        mNowTimeString = String.valueOf(mNowTime);
        mQrCode = person.getQrcode();

        setupScanArtefacts();
        // setupRenderer must be called after
        // setupScanArtefacts for setting mVideoOutputFile and sVideoEncoder was done!
        setupRenderer();

        getCurrentLocation();

        stopService(new Intent(this, FileLogMonitorService.class));
    }

    public void onDestroy() {
        startService(new Intent(this, FileLogMonitorService.class));
        super.onDestroy();
    }

    private void setupScanArtefacts() {
        // TODO make part of AppController?
        mExtFileDir = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());

        // TODO make part of AppConstants
        mScanArtefactsOutputFolder  = new File(mExtFileDir,mQrCode+"/measurements/"+mNowTimeString+"/");
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

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // TODO: setup own renderer for scanning process (or attribute Apache License 2.0 from Google)
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
                    synchronized (RecorderActivity.this) {
                        // Connect the Tango SDK to the OpenGL texture ID where we are going to
                        // render the camera.
                        // NOTE: This must be done after the texture is generated and the Tango
                        // service is connected.
                        if (mConnectedTextureIdGlThread == INVALID_TEXTURE_ID) {
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            //sVideoEncoder.setTextureId(mConnectedTextureIdGlThread);
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mConnectedTextureIdGlThread);

                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture and
                        // scene camera pose.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {

                            double rgbTimestamp =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

                            // {@code rgbTimestamp} contains the exact timestamp at which the
                            // rendered RGB frame was acquired.

                            // In order to see more details on how to use this timestamp to modify
                            // the scene camera and achieve an augmented reality effect,
                            // refer to java_augmented_reality_example and/or
                            // java_augmented_reality_opengl_example projects.

                        }
                    }
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL thread", e);
                    Crashlytics.log(Log.ERROR, TAG, "Tango API call error within the OpenGL thread");
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                    Crashlytics.log(Log.ERROR, TAG, "Exception on the OpenGL thread");
                }
            }
        });
        mCameraSurfaceView.setRenderer(mRenderer);
    }


    // TODO: implement own code&documentation or attribute Apache License 2.0 Copyright Google
    @Override
    protected void onResume() {
        super.onResume();
        mCameraSurfaceView.onResume();

        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango Service is properly set up and we start getting onFrameAvailable callbacks.
        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.

        mTango = new Tango(RecorderActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in
                // the OpenGL thread or in the UI thread.
                synchronized (RecorderActivity.this) {
                    try {
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        initialize(mTango);
                        mIsConnected = true;

                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date);
                        Crashlytics.log(Log.ERROR, TAG, "TangoOutOfDateException");
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                        Crashlytics.log(Log.ERROR, TAG, "TangoErrorException");
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                        Crashlytics.log(Log.ERROR, TAG, "TangoInvalidException");
                    }
                    setUpExtrinsics();
                }
            }
        });

        mCameraSurfaceView.onResume();
        Log.d(TAG, "onResume complete: " + this);
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
                Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in synchronized disconnect onPause");
            }
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Create a new Tango configuration and enable the Camera API.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    private void updateScanningProgress(int numPoints, float distance, float confidence) {
        float minPointsToCompleteScan = 199500.0f;
        float progressToAddFloat = numPoints / minPointsToCompleteScan;
        progressToAddFloat = progressToAddFloat*100;
        int progressToAdd = (int) progressToAddFloat;
        Log.d(TAG, "numPoints: "+numPoints+" float: "+progressToAddFloat+" currentProgress: "+mProgress+" progressToAdd: "+progressToAdd);
        if (mProgress+progressToAdd > 100) {
            mProgress = 100;
        } else {
            mProgress = mProgress+progressToAdd;
        }
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private void startupTango() {
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs =
                new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            String[] poseStatusCode = {"POSE_INITIALIZING","POSE_VALID","POSE_INVALID","POSE_UNKNOWN"};

            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                mDeltaTime = (float) (pose.timestamp - mPosePreviousTimeStamp)
                        * SECS_TO_MILLISECS;
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
            @AddTrace(name = "onPointCloudAvailable", enabled = true)
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {

                Log.d(TAG, "recording:"+mIsRecording);
                // set to true for next RGB image to be written
                // TODO remove when not necessary anymore (performance/video capture)
                mPointCloudAvailable = true;

                float[] average = TangoUtils.calculateAveragedDepth(pointCloudData.points, pointCloudData.numPoints);

                mOverlaySurfaceView.setDistance(average[0]);
                mOverlaySurfaceView.setConfidence(average[1]);

                // Get pose transforms for openGL to depth/color cameras.
                TangoPoseData oglTdepthPose = TangoSupport.getPoseAtTime(
                        pointCloudData.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                        TangoSupport.ROTATION_IGNORED);
                if (oglTdepthPose.statusCode != TangoPoseData.POSE_VALID) {
                    //Log.w(TAG, "Could not get depth camera transform at time "
                    //        + pointCloudData.timestamp);
                }

                mCurrentTimeStamp = (float) pointCloudData.timestamp;
                final float frameDelta = (mCurrentTimeStamp - mPointCloudPreviousTimeStamp)
                        * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                mPointCloudCallbackCount++;

                // My writing to file function


                // Background task for writing to file
                // TODO refactor to top-level class or make static?
                Runnable thread = new Runnable() {
                    @Override
                    @AddTrace(name = "pcRunnable", enabled = true)
                    public void run() {
                        try {
                            mutex_on_mIsRecording.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Crashlytics.log(Log.WARN, TAG, "InterruptedException aquiring recording mutext");
                        }
                        // Saving the frame or not, depending on the current mode.
                        if ( mIsRecording ) {
                            updateScanningProgress(pointCloudData.numPoints, average[0], average[1]);
                            progressBar.setProgress(mProgress);
                            mPointCloudFilename = "pc_" +mQrCode+"_" + mNowTimeString + "_" + mScanningWorkflowStep +
                                    "_" + String.format(Locale.getDefault(), "%03d", mNumberOfFilesWritten);
                            Uri pcUri = TangoUtils.writePointCloudToPcdFile(pointCloudData, mPointCloudSaveFolder, mPointCloudFilename);
                            File artefactFile = new File(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename +".pcd");
                            FileLog log = new FileLog();
                            log.setId(AppController.getInstance().getArtefactId("scan-pcd"));
                            log.setType("pcd");
                            log.setPath(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename + ".pcd");
                            log.setHashValue(MD5.getMD5(mPointCloudSaveFolder.getPath() + File.separator + mPointCloudFilename +".pcd"));
                            log.setFileSize(artefactFile.length());
                            log.setUploadDate(0);
                            log.setDeleted(false);
                            log.setQrCode(mQrCode);
                            log.setCreateDate(mNowTime);
                            log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                            new OfflineTask().saveFileLog(log);
                            // Direct Upload to Firebase Storage
                            mNumberOfFilesWritten++;
                            //mTimeToTakeSnap = false;

                        }
                        mutex_on_mIsRecording.release();
                    }
                };
                thread.run();
                /*
                if (mProgress == 100) {
                    Log.d(TAG, "enough data, stopping scan, mProgress:"+mProgress);
                    try {
                        mutex_on_mIsRecording.acquire();
                        mIsRecording = false;
                        record_SwitchChanged();
                        // Saving the frame or not, depending on the current mode.
                        mutex_on_mIsRecording.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //gotoNextStep();
                }
                */
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

        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                new Tango.OnFrameAvailableListener() {
                    @Override
                    public  void onFrameAvailable(TangoImageBuffer tangoImageBuffer, int i) {
                        if ( ! mIsRecording || ! mPointCloudAvailable) {
                            return;
                        }

                        Runnable thread = new Runnable() {
                            @Override
                            @AddTrace(name = "onFrameAvailableRunnable", enabled = true)
                            public void run() {
                                TangoImageBuffer currentTangoImageBuffer = TangoUtils.copyImageBuffer(tangoImageBuffer);

                                String currentImgFilename = "rgb_" +mQrCode+"_" + mNowTimeString + "_" +
                                        mScanningWorkflowStep + "_" + currentTangoImageBuffer.timestamp + ".jpg";
                                Uri uri = BitmapUtils.writeImageToFile(currentTangoImageBuffer, mRgbSaveFolder, currentImgFilename);
                                File artefactFile = new File(mRgbSaveFolder.getPath() + File.separator + currentImgFilename);
                                FileLog log = new FileLog();
                                log.setId(AppController.getInstance().getArtefactId("scan-rgb"));
                                log.setType("rgb");
                                log.setPath(mRgbSaveFolder.getPath() + File.separator + currentImgFilename);
                                log.setHashValue(MD5.getMD5(mRgbSaveFolder.getPath() + File.separator + currentImgFilename));
                                log.setFileSize(artefactFile.length());
                                log.setUploadDate(0);
                                log.setDeleted(false);
                                log.setQrCode(mQrCode);
                                log.setCreateDate(mNowTime);
                                log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                                new OfflineTask().saveFileLog(log);
                                // Direct Upload to Firebase Storage
                                // Start MyUploadService to upload the file, so that the file is uploaded
                                // even if this Activity is killed or put in the background
                            }
                        };
                        thread.run();
                    }
                });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */

    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mCameraSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUv(mDisplayRotation);
                }
            }
        });
    }


    // TODO: attribute Apache License 2.0 from Google or remove
    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RecorderActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    // TODO: proper attribution
    // From ParaView Tango Recorder Copyright Paraview
    // Apache License 2.0
    // https://github.com/Kitware/ParaViewTangoRecorder


    private void setUpExtrinsics() {
        // Set device to imu matrix in Model Matrix Calculator.
        TangoPoseData device2IMUPose = new TangoPoseData();
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        try {
            device2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.exception_tango_error,
                    Toast.LENGTH_SHORT).show();
            Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in device setup");
        }
        /*mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(
                device2IMUPose.getTranslationAsFloats(),
                device2IMUPose.getRotationAsFloats());
*/
        // Set color camera to imu matrix in Model Matrix Calculator.
        TangoPoseData color2IMUPose = new TangoPoseData();

        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        try {
            color2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), R.string.exception_tango_error,
                    Toast.LENGTH_SHORT).show();
            Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in camera setup");
        }
        /*
        mRenderer.getModelMatCalculator().SetColorCamera2IMUMatrix(
                color2IMUPose.getTranslationAsFloats(),
                color2IMUPose.getRotationAsFloats());
*/
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

    // This function is called when the Record Switch is changed
    private void record_SwitchChanged() {
        try {
            mutex_on_mIsRecording.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, "InterruptedException starting recording");
        }
        // Start Recording
        Log.v(TAG,"record_SwitchChanged to "+mIsRecording);
        if (mIsRecording) {
            mNowTime = System.currentTimeMillis();
            mNowTimeString = String.valueOf(mNowTime);
            Log.v(TAG,"now: "+mNowTimeString);
            mNumberOfFilesWritten = 0;
        }
        // Finish Recording
        else {
            // Background task for writing poses to file
            // TODO refactor to top-level class or make static?
            class SendCommandTask extends AsyncTask<Context, Void, Void> {
                /** The system calls this to perform work in a worker thread and
                 * delivers it the parameters given to AsyncTask.execute() */
                @Override
                protected Void doInBackground(Context... contexts) {

                    // Stop the Pose Recording, and write them to a file.
                    writePoseToFile(mNumPoseInSequence);
                    mNumPoseInSequence = 0;
                    mPoseOrientationBuffer.clear();
                    mPoseOrientationBuffer.clear();
                    mPoseTimestampBuffer.clear();
                    return null;
                }
            }
            new SendCommandTask().execute(this);

        }
        mutex_on_mIsRecording.release();

    }

    // This function writes the pose data and timestamps to .vtk files in binary
    @AddTrace(name = "writePoseToFile", enabled = true)
    private void writePoseToFile(int numPoints) {

        String poseFileName = "pc_" +mQrCode+"_"+ mNowTimeString + "_poses.vtk";
        mPointCloudFilenameBuffer.add(mPointCloudSaveFolder + poseFileName);
        File file = new File(mPointCloudSaveFolder, poseFileName);

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));

            // TODO: why pose method for writing VTK Polydata? (Line 1050 vs. 1192)
            out.write(("# vtk DataFile Version 3.0\n" +
                    "vtk output\n" +
                    "BINARY\n" +
                    "DATASET POLYDATA\n" +
                    "POINTS " + numPoints + " float\n").getBytes());

            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPosePositionBuffer.get(i)[0]);
                out.writeFloat(mPosePositionBuffer.get(i)[1]);
                out.writeFloat(mPosePositionBuffer.get(i)[2]);
            }

            out.write(("\nLINES 1 " + String.valueOf(numPoints + 1) + "\n").getBytes());
            out.writeInt(numPoints);
            for (int i = 0; i < numPoints; i++) {
                out.writeInt(i);
            }

            out.write(("\nFIELD FieldData 1\n" +
                    "Cam2Dev_transform 16 1 float\n").getBytes());
            for (int i = 0; i < cam2dev_Transform.length; i++) {
                out.writeFloat(cam2dev_Transform[i]);
            }

            out.write(("\nPOINT_DATA " + String.valueOf(numPoints) + "\n" +
                    "FIELD FieldData 2\n" +
                    "orientation 4 " + String.valueOf(numPoints) + " float\n").getBytes());

            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPoseOrientationBuffer.get(i)[0]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[1]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[2]);
                out.writeFloat(mPoseOrientationBuffer.get(i)[3]);
            }

            out.write(("\ntimestamp 1 " + String.valueOf(numPoints) + " float\n").getBytes());
            for (int i = 0; i < numPoints; i++) {
                out.writeFloat(mPoseTimestampBuffer.get(i));
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            Crashlytics.log(Log.ERROR, TAG, "IOException writing pose to file");
        }
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
            } else if (isNetworkEnabled || isGPSEnabled) {
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
                    getAddressFromLocation(loc.getLatitude(), loc.getLongitude());
                }
            }
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(RecorderActivity.this, Locale.getDefault());
                String result = null;
                try {
                    List <Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addressList != null && addressList.size() > 0) {
                        Address address = addressList.get(0);
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                            sb.append(address.getAddressLine(i));

                        result = sb.toString();
                    }
                } catch (IOException e) {
                    Log.e("Location Address Loader", "Unable connect to Geocoder", e);
                    Crashlytics.log(Log.ERROR, TAG, "IOException Unable connect to Geocoder");
                } finally {
                    location = new Loc();

                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    location.setAddress(result);

                    if (measure != null)
                        measure.setLocation(location);
                }
            }
        });
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION && grantResults[0] >= 0) {
            getCurrentLocation();
        }
    }
}
