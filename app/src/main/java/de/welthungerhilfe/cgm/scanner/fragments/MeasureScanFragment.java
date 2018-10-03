package de.welthungerhilfe.cgm.scanner.fragments;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.RecorderActivity;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.tango.CameraSurfaceRenderer;
import de.welthungerhilfe.cgm.scanner.tango.OverlaySurface;

public class MeasureScanFragment extends Fragment {
    TextView txtTitle;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_measure_scan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        txtTitle = view.findViewById(R.id.txtTitle);

        mCameraSurfaceView = view.findViewById(R.id.surfaceview);
        mOverlaySurfaceView = view.findViewById(R.id.overlaySurfaceView);
    }

    // TODO: implement own code&documentation or attribute Apache License 2.0 Copyright Google
    @Override
    public void onResume() {
        super.onResume();
    }
}
