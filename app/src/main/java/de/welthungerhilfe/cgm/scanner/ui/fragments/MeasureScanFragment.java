package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.projecttango.tangosupport.TangoSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.ui.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.helper.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.helper.tango.ModelMatCalculator;
import de.welthungerhilfe.cgm.scanner.helper.tango.OverlaySurface;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.MD5;
import de.welthungerhilfe.cgm.scanner.utils.TangoUtils;

import static com.projecttango.tangosupport.TangoSupport.initialize;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_SIDE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_PREVIEW;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_SIDE;

public class MeasureScanFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "ScanningProcess";

    private static GLSurfaceView mCameraSurfaceView;
    private static OverlaySurface mOverlaySurfaceView;
    private CameraSurfaceRenderer mRenderer;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;

    private int mDisplayRotation = Surface.ROTATION_0;

    private static final int INVALID_TEXTURE_ID = 0;
    private static final int SECS_TO_MILLISECS = 1000;

    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);

    private TextView mTitleView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;
    private Button btnRetake;

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

    private boolean mPointCloudAvailable;
    private boolean mIsRecording;
    private int mProgress;

    private long mNowTime;
    private String mNowTimeString;
    private String mQrCode;

    private Semaphore mutex_on_mIsRecording;

    private int mode = SCAN_PREVIEW;

    private FileLogRepository repository;
    private long age = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        repository = FileLogRepository.getInstance(context);

        age = (System.currentTimeMillis() - ((ScanModeActivity) getActivity()).person.getBirthday()) / 1000 / 60 / 60 / 24;

        mTango = new Tango(context, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in
                // the OpenGL thread or in the UI thread.
                synchronized (getActivity()) {
                    try {
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        initialize(mTango);
                        mIsConnected = true;

                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        // todo: Crashlytics.log(Log.ERROR, TAG, "TangoOutOfDateException");
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        // todo: Crashlytics.log(Log.ERROR, TAG, "TangoErrorException");
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        // todo: Crashlytics.log(Log.ERROR, TAG, "TangoInvalidException");
                    }
                    setUpExtrinsics();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

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
        mQrCode = ((ScanModeActivity)getActivity()).person.getQrcode();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_measure_scan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTitleView = view.findViewById(R.id.txtTitle);
        progressBar = view.findViewById(R.id.progressBar);
        fab = view.findViewById(R.id.fab_scan_result);
        fab.setOnClickListener(this);
        btnRetake = view.findViewById(R.id.btnRetake);
        view.findViewById(R.id.btnRetake).setOnClickListener(this);
        view.findViewById(R.id.imgClose).setOnClickListener(this);

        mCameraSurfaceView = view.findViewById(R.id.surfaceview);
        mOverlaySurfaceView = view.findViewById(R.id.overlaySurfaceView);

        switch (mode) {
            case SCAN_STANDING_FRONT:
                mTitleView.setText(getString(R.string.front_view_01) + " - " + getString(R.string.mode_standing));
                break;
            case SCAN_STANDING_SIDE:
                mTitleView.setText(getString(R.string.lateral_view_02) + " - " + getString(R.string.mode_standing));
                break;
            case SCAN_STANDING_BACK:
                mTitleView.setText(getString(R.string.back_view_03) + " - " + getString(R.string.mode_standing));
                break;
            case SCAN_LYING_FRONT:
                mTitleView.setText(getString(R.string.front_view_01) + " - " + getString(R.string.mode_lying));
                break;
            case SCAN_LYING_SIDE:
                mTitleView.setText(getString(R.string.lateral_view_02) + " - " + getString(R.string.mode_lying));
                break;
            case SCAN_LYING_BACK:
                mTitleView.setText(getString(R.string.back_view_03) + " - " + getString(R.string.mode_lying));
                break;
        }

        setupScanArtefacts();
        setupRenderer();
    }

    @Override
    public void onActivityCreated(Bundle savedBundle) {
        super.onActivityCreated(savedBundle);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        mCameraSurfaceView.onResume();
        mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        if (mode == SCAN_STANDING_FRONT || mode == SCAN_STANDING_SIDE || mode == SCAN_STANDING_BACK)
            mOverlaySurfaceView.setMode(OverlaySurface.INFANT_CLOSE_DOWN_UP_OVERLAY);
        else if (mode == SCAN_LYING_FRONT || mode == SCAN_LYING_SIDE || mode == SCAN_LYING_BACK)
            mOverlaySurfaceView.setMode(OverlaySurface.BABY_OVERLAY);
    }

    @Override
    public void onPause() {
        super.onPause();

        mCameraSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (getActivity()) {
            try {
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume.
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
                // todo: Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in synchronized disconnect onPause");
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_scan_result:
                if (mIsRecording) {
                    if (mProgress >= 100) {
                        completeScan();
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
                ((ScanModeActivity)getActivity()).closeScan();
                break;
            case R.id.btnRetake:
                mProgress = 0;
                break;
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    private void setupScanArtefacts() {
        mExtFileDir = AppController.getInstance().getRootDirectory();

        // TODO make part of AppConstants
        Log.e("Root Directory", mExtFileDir.getParent());
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
                    synchronized (getActivity()) {
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
                    Log.e(TAG, "Tango API call error within the OpenGL thread", e);
                    // todo: Crashlytics.log(Log.ERROR, TAG, "Tango API call error within the OpenGL thread");
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                    // todo: Crashlytics.log(Log.ERROR, TAG, "Exception on the OpenGL thread");
                }
            }
        });

        mCameraSurfaceView.setRenderer(mRenderer);
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
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData) {

                Log.d(TAG, "recording:"+mIsRecording);
                // set to true for next RGB image to be written
                // TODO remove when not necessary anymore (performance/video capture)
                mPointCloudAvailable = true;

                float[] average = TangoUtils.calculateAveragedDepth(pointCloudData.points, pointCloudData.numPoints);

                mOverlaySurfaceView.setNumPoints(pointCloudData.numPoints);
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
                    //Log.w(TAG, "Could not get depth camera transform at time " + pointCloudData.timestamp);
                }

                mCurrentTimeStamp = (float) pointCloudData.timestamp;
                final float frameDelta = (mCurrentTimeStamp - mPointCloudPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = mCurrentTimeStamp;
                mPointCloudCallbackCount++;

                // My writing to file function


                // Background task for writing to file
                // TODO refactor to top-level class or make static?
                Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mutex_on_mIsRecording.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            // todo: Crashlytics.log(Log.WARN, TAG, "InterruptedException aquiring recording mutext");
                        }
                        // Saving the frame or not, depending on the current mode.
                        if ( mIsRecording ) {
                            // TODO save files to local storage
                            updateScanningProgress(pointCloudData.numPoints, average[0], average[1]);
                            progressBar.setProgress(mProgress);

                            mPointCloudFilename = "pc_" +mQrCode+"_" + mNowTimeString + "_" + mode +
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
                            log.setQrCode(mQrCode);
                            log.setCreateDate(mNowTime);
                            // ToDo: Add user email from AppCenter Auth;
                            // log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                            log.setAge(age);

                            repository.insertFileLog(log);

                            //new OfflineTask().saveFileLog(log);
                            // Direct Upload to Firebase Storage
                            mNumberOfFilesWritten++;
                            //mTimeToTakeSnap = false;
                        }
                        mutex_on_mIsRecording.release();
                    }
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

        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                new Tango.OnFrameAvailableListener() {
                    @Override
                    public  void onFrameAvailable(TangoImageBuffer tangoImageBuffer, int i) {
                        if ( ! mIsRecording || ! mPointCloudAvailable) {
                            return;
                        }

                        Runnable thread = () -> {
                            TangoImageBuffer currentTangoImageBuffer = TangoUtils.copyImageBuffer(tangoImageBuffer);

                            // TODO save files to local storage
                            String currentImgFilename = "rgb_" +mQrCode+"_" + mNowTimeString + "_" +
                                    mode + "_" + currentTangoImageBuffer.timestamp + ".jpg";

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
                            log.setQrCode(mQrCode);
                            log.setCreateDate(mNowTime);
                            // ToDo: Add user email from AppCenter Auth;
                            // log.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                            log.setAge(age);
                            repository.insertFileLog(log);
                        };
                        thread.run();
                    }
                });
    }

    /**
     * Set the color camera background texture rotation and save the camera to display rotation.
     */
    private void setDisplayRotation() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
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
            Toast.makeText(getContext(), R.string.exception_tango_error, Toast.LENGTH_SHORT).show();
            // todo: Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in device setup");
        }
        /*mRenderer.getModelMatCalculator().SetDevice2IMUMatrix(device2IMUPose.getTranslationAsFloats(),device2IMUPose.getRotationAsFloats());*/
        // Set color camera to imu matrix in Model Matrix Calculator.
        TangoPoseData color2IMUPose = new TangoPoseData();

        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        try {
            color2IMUPose = mTango.getPoseAtTime(0.0, framePair);
        } catch (TangoErrorException e) {
            Toast.makeText(getContext(), R.string.exception_tango_error, Toast.LENGTH_SHORT).show();
            // todo: Crashlytics.log(Log.ERROR, TAG, "TangoErrorException in camera setup");
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fab.setImageResource(R.drawable.done);
                }
            });
        } else {
            mProgress = mProgress+progressToAdd;
        }

        Log.d("scan_progress", String.valueOf(mProgress));
        Log.d("scan_progress_step", String.valueOf(progressToAdd));
    }

    private void startScan() {
        mProgress = 0;

        resumeScan();
    }

    private void resumeScan() {
        if (mode == SCAN_PREVIEW)
            return;

        mIsRecording = true;
        fab.setImageResource(R.drawable.stop);
    }

    private void pauseScan() {
        mIsRecording = false;
        fab.setImageResource(R.drawable.recorder);
    }

    private void completeScan() {
        mIsRecording = false;

        ((ScanModeActivity)getActivity()).goToNextStep();
    }
}
