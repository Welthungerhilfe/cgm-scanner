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
package de.welthungerhilfe.cgm.scanner.hardware.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.widget.ImageView;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.hardware.gpu.BitmapHelper;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class TangoCamera extends AbstractARCamera implements GLSurfaceView.Renderer {

    public interface TangoCameraListener {
        void onTangoColorData(TangoImageBuffer tangoImageBuffer);

        void onTangoDepthData(TangoPointCloudData pointCloudData, float[] position, float[] rotation, TangoCameraIntrinsics[] calibration);
    }

    //constants
    private static final String TANGO_CORE = "com.google.tango";

    //Tango objects and status
    private Tango mTango;
    private boolean mPointCloudAvailable;

    public TangoCamera(Activity activity) {
        super(activity, DepthPreviewMode.OFF, PreviewSize.CLIPPED);
    }

    @Override
    public void onCreate(ImageView colorPreview, ImageView depthPreview, GLSurfaceView surfaceview) {
        super.onCreate(colorPreview, depthPreview, surfaceview);

        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);
    }

    @Override
    public void onResume() {
        if (mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startTango();
                    mGLSurfaceView.onResume();
                    mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
            }
        }
    }

    @Override
    public void onPause() {
        if (mTango == null) {
            return;
        }

        mGLSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                mTango.disconnect();
                mTango = null;
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

        mTango = new Tango(mActivity, () -> {
            // Synchronize against disconnecting while the service is being used in
            // the OpenGL thread or in the UI thread.
            synchronized (this) {
                try {
                    TangoConfig config = mTango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
                    config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
                    config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
                    config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);

                    mTango.connect(config);
                    startupTango();
                    TangoSupport.initialize(mTango);
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

    private void startupTango() {
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));
        TangoCameraIntrinsics[] calibration = {
                mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR),
                mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_DEPTH)
        };

        // Listen for new Tango data.
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                if (pose.statusCode == TangoPoseData.POSE_VALID) {
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
                }
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloudData) throws TangoErrorException {
                mPointCloudAvailable = true;
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

            //update preview window
            Bitmap bitmap = TangoUtils.imageBuffer2Bitmap(tangoImageBuffer);
            mActivity.runOnUiThread(() -> {
                float scale = getPreviewScale(bitmap);
                mColorCameraPreview.setImageBitmap(bitmap);
                mColorCameraPreview.setRotation(90);
                mColorCameraPreview.setScaleX(scale);
                mColorCameraPreview.setScaleY(scale);
                mDepthCameraPreview.setRotation(90);
                mDepthCameraPreview.setScaleX(scale);
                mDepthCameraPreview.setScaleY(scale);
            });

            mPixelIntensity = BitmapHelper.getPixelIntensity(bitmap);
            for (Object listener : mListeners) {
                ((TangoCameraListener)listener).onTangoColorData(tangoImageBuffer);
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {}

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {}

    @Override
    public void onDrawFrame(GL10 gl10) {}
}
