/**
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
package de.welthungerhilfe.cgm.scanner.helper.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.tango.CameraSurfaceRenderer;

import static com.projecttango.tangosupport.TangoSupport.initialize;

public class TangoCamera implements ICamera {

    public interface TangoCameraListener {
        void onTangoColorData(TangoImageBuffer tangoImageBuffer);

        void onTangoDepthData(TangoPointCloudData pointCloudData, String pose);
    }

    //constants
    private static final String TAG = TangoCamera.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;

    //Tango objects and status
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected;
    private boolean mPointCloudAvailable;
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private int mDisplayRotation = Surface.ROTATION_0;
    private Semaphore mutex_on_mIsRecording;
    private AtomicBoolean mIsFrameAvailableTangoThread;
    private String mPose;

    //App integration objects
    private Activity mActivity;
    private GLSurfaceView mCameraSurfaceView;
    private CameraSurfaceRenderer mRenderer;
    private ArrayList<TangoCameraListener> mListeners;

    public TangoCamera(Activity activity) {
        mActivity = activity;
        mListeners = new ArrayList<>();

        mIsConnected = false;
        mIsFrameAvailableTangoThread = new AtomicBoolean(false);
        mPose = "0 0 0 1 0 0 0";
    }

    @Override
    public void addListener(Object listener) {
        mListeners.add((TangoCameraListener) listener);
    }

    @Override
    public void removeListener(Object listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onCreate() {
        mCameraSurfaceView = mActivity.findViewById(R.id.surfaceview);
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
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, mConnectedTextureIdGlThread);

                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // If there is a new RGB camera frame available, update the texture and
                        // scene camera pose.
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {

                            mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                        }
                    }
                } catch (TangoErrorException e) {
                    Crashes.trackError(e);
                }
            }
        });

        mCameraSurfaceView.setRenderer(mRenderer);
    }

    @Override
    public void onResume() {
        if (mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startTango();
                    mCameraSurfaceView.onResume();
                    mCameraSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
            }
        }
    }

    @Override
    public void onPause() {
        if (mTango == null) {
            return;
        }

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
                Log.e(TAG, e.getMessage());
                Crashes.trackError(e);
            }
        }
    }

    private void startTango() {
        if (mTango != null) {
            return;
        }

        mPointCloudAvailable = false;
        mutex_on_mIsRecording = new Semaphore(1,true);

        mTango = new Tango(mActivity, () -> {
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
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    Crashes.trackError(e);
                }
            }
        });
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

            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                if (pose.statusCode == TangoPoseData.POSE_VALID) {
                    try {
                        mutex_on_mIsRecording.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Crashes.trackError(e);
                    }
                    mPose = "";
                    mPose += pose.rotation[0] + " ";
                    mPose += pose.rotation[1] + " ";
                    mPose += pose.rotation[2] + " ";
                    mPose += pose.rotation[3] + " ";
                    mPose += pose.translation[0] + " ";
                    mPose += pose.translation[1] + " ";
                    mPose += pose.translation[2];
                    mutex_on_mIsRecording.release();
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloudData) throws TangoErrorException {
                // set to true for next RGB image to be written
                // TODO remove when not necessary anymore (performance/video capture)
                mPointCloudAvailable = true;

                try {
                    mutex_on_mIsRecording.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Crashes.trackError(e);
                }
                for (TangoCameraListener listener : mListeners) {
                    listener.onTangoDepthData(pointCloudData, mPose);
                }
                mutex_on_mIsRecording.release();
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
            if ( ! mPointCloudAvailable) {
                return;
            }
            for (TangoCameraListener listener : mListeners) {
                listener.onTangoColorData(tangoImageBuffer);
            }
        });
    }

    private void setDisplayRotation() {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // We also need to update the camera texture UV coordinates. This must be run in the OpenGL
        // thread.
        mCameraSurfaceView.queueEvent(() -> {
            if (mIsConnected) {
                mRenderer.updateColorCameraTextureUv(mDisplayRotation);
            }
        });
    }
}
