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
package de.welthungerhilfe.cgm.scanner.camera;

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
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;
import com.microsoft.appcenter.crashes.Crashes;
import com.projecttango.tangosupport.TangoSupport;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class TangoCamera extends AbstractARCamera {

    public interface TangoCameraListener {
        void onTangoColorData(TangoImageBuffer tangoImageBuffer);

        void onTangoDepthData(TangoPointCloudData pointCloudData, float[] position, float[] rotation, TangoCameraIntrinsics[] calibration);
    }

    //constants
    private static final String TAG = TangoCamera.class.getSimpleName();
    private static final String TANGO_CORE = "com.google.tango";
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

    //App integration objects
    private GLSurfaceView mCameraSurfaceView;
    private TangoCameraRenderer mRenderer;

    public TangoCamera(Activity activity) {
        super(activity, false);

        mIsConnected = false;
        mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    }

    @Override
    public void onCreate(int colorPreview, int depthPreview, int surfaceview) {
        mCameraSurfaceView = mActivity.findViewById(surfaceview);
        mCameraSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new TangoCameraRenderer(new TangoCameraRenderer.RenderCallback() {

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
                mTango = null;
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Crashes.trackError(e);
            }
        }
    }

    private void startTango() {
        if (mTango != null) {
            return;
        }

        //check if Tango Core is installed
        if (!Utils.isPackageInstalled(mActivity, TANGO_CORE)) {
            mActivity.runOnUiThread(() -> {
                Utils.openPlayStore(mActivity, TANGO_CORE);
            });
            Utils.sleep(100);
            mActivity.finish();
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
                    TangoSupport.initialize(mTango);
                    mIsConnected = true;

                    setDisplayRotation();
                } catch (TangoOutOfDateException e) {
                    mActivity.runOnUiThread(() -> {
                        Utils.openPlayStore(mActivity, TANGO_CORE);
                    });
                    Utils.sleep(100);
                    mActivity.finish();
                } catch (Exception e) {
                    e.printStackTrace();
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
            public void onPoseAvailable(TangoPoseData pose) {
                if (pose.statusCode == TangoPoseData.POSE_VALID) {
                    try {
                        mutex_on_mIsRecording.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Crashes.trackError(e);
                    }
                    pose = TangoSupport.getPoseAtTime(0,
                            TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            TangoSupport.ROTATION_0);
                    mPosition[0] = (float) pose.translation[0];
                    mPosition[1] = (float) pose.translation[1];
                    mPosition[2] = (float) pose.translation[2];
                    mRotation[0] = (float) pose.rotation[0];
                    mRotation[1] = (float) pose.rotation[1];
                    mRotation[2] = (float) pose.rotation[2];
                    mRotation[3] = (float) pose.rotation[3];
                    mutex_on_mIsRecording.release();
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloudData) throws TangoErrorException {
                mPointCloudAvailable = true;

                try {
                    mutex_on_mIsRecording.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Crashes.trackError(e);
                }
                TangoCameraIntrinsics[] calibration = {
                        mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR),
                        mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_DEPTH)
                };
                mColorCameraIntrinsic[0] = (float)calibration[0].fx / (float)calibration[0].width;
                mColorCameraIntrinsic[1] = (float)calibration[0].fy / (float)calibration[0].height;
                mColorCameraIntrinsic[2] = (float)calibration[0].cx / (float)calibration[0].width;
                mColorCameraIntrinsic[3] = (float)calibration[0].cy / (float)calibration[0].height;
                mDepthCameraIntrinsic[0] = (float)calibration[1].fx / (float)calibration[1].width;
                mDepthCameraIntrinsic[1] = (float)calibration[1].fy / (float)calibration[1].height;
                mDepthCameraIntrinsic[2] = (float)calibration[1].cx / (float)calibration[1].width;
                mDepthCameraIntrinsic[3] = (float)calibration[1].cy / (float)calibration[1].height;
                mHasCameraCalibration = true;

                float[] position;
                float[] rotation;
                synchronized (mLock) {
                    position = mPosition;
                    rotation = mRotation;
                }
                for (Object listener : mListeners) {
                    ((TangoCameraListener)listener).onTangoDepthData(pointCloudData, position, rotation, calibration);
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

            @Override
            public void onTangoEvent(TangoEvent event) {
                super.onTangoEvent(event);

                switch (event.eventKey) {
                    case TangoEvent.DESCRIPTION_FISHEYE_OVER_EXPOSED:
                        mLight = LightConditions.BRIGHT;
                        mLastBright = System.currentTimeMillis();
                        break;
                    case TangoEvent.DESCRIPTION_FISHEYE_UNDER_EXPOSED:
                        mLight = LightConditions.DARK;
                        mLastDark = System.currentTimeMillis();
                        break;
                }
            }
        });

        mTango.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, (tangoImageBuffer, i) -> {
            if ( ! mPointCloudAvailable) {
                return;
            }
            mPixelIntensity = TangoUtils.getPixelIntensity(tangoImageBuffer);
            for (Object listener : mListeners) {
                ((TangoCameraListener)listener).onTangoColorData(tangoImageBuffer);
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
