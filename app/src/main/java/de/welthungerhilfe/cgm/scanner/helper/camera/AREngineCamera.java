/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.helper.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.widget.ImageView;

import com.google.ar.core.Pose;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARCameraIntrinsics;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsActivity;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;

public class AREngineCamera implements ICamera {

  private static final String TAG = AREngineCamera.class.getSimpleName();

  //AREngine API
  private ARSession mSession;
  private ARPose mPose;
  private final Object mLock;

  //App integration objects
  private Activity mActivity;
  private ImageView mColorCameraPreview;
  private ImageView mDepthCameraPreview;
  private GLSurfaceView mGLSurfaceView;
  private ArrayList<ARCoreUtils.Camera2DataListener> mListeners;
  private Bitmap mCache;
  private CameraCalibration mCameraCalibration;
  private int mFrameIndex;
  private float mPixelIntensity;
  private boolean mShowDepth;

  public AREngineCamera(Activity activity) {
    mActivity = activity;
    mListeners = new ArrayList<>();

    mCameraCalibration = new CameraCalibration();
    mLock = new Object();
    mFrameIndex = 1;
    mPixelIntensity = 0;
    mShowDepth = LocalPersistency.getBoolean(activity, SettingsActivity.KEY_SHOW_DEPTH);
  }

  public void addListener(Object listener) {
    mListeners.add((ARCoreUtils.Camera2DataListener) listener);
  }

  public void removeListener(Object listener) {
    mListeners.remove(listener);
  }

  @Override
  public void onCreate() {
    Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    bitmap.setPixel(0, 0, Color.TRANSPARENT);
    mColorCameraPreview = mActivity.findViewById(R.id.colorCameraPreview);
    mColorCameraPreview.setImageBitmap(bitmap);
    mDepthCameraPreview = mActivity.findViewById(R.id.depthCameraPreview);
    mDepthCameraPreview.setImageBitmap(bitmap);

    //setup AREngine cycle
    mGLSurfaceView = mActivity.findViewById(R.id.surfaceview);
    mGLSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
      private int[] textures = new int[1];
      private int width, height;

      @Override
      public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glGenTextures(1, textures, 0);
      }

      @Override
      public void onSurfaceChanged(GL10 gl10, int width, int height) {
        this.width = width;
        this.height = height;
      }

      @Override
      public void onDrawFrame(GL10 gl10) {
        synchronized (AREngineCamera.this) {
          updateFrame(textures[0], width, height);
        }
      }
    });
  }

  @Override
  public synchronized void onResume() {
    mGLSurfaceView.onResume();

    if (mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
          openCamera();
        }
      }
    }
  }

  @Override
  public synchronized void onPause() {
    mGLSurfaceView.onPause();

    closeCamera();
  }

  private void onProcessColorData(Image image) {
    if (image == null) {
      Log.w(TAG, "onImageAvailable: Skipping null image.");
      return;
    }
    final ByteBuffer yuvBytes = BitmapUtils.imageToByteBuffer(image);

    // Convert YUV to RGB
    final RenderScript rs = RenderScript.create(mActivity);
    final Bitmap bitmap     = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
    final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);
    final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
    allocationYuv.copyFrom(yuvBytes.array());
    ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    scriptYuvToRgb.setInput(allocationYuv);
    scriptYuvToRgb.forEach(allocationRgb);
    allocationRgb.copyTo(bitmap);

    mCache = bitmap;
    allocationYuv.destroy();
    allocationRgb.destroy();
    rs.destroy();

    //update preview window
    mActivity.runOnUiThread(() -> {
      float scale = bitmap.getWidth() / (float)bitmap.getHeight();
      //scale *= mColorCameraPreview.getHeight() / (float)bitmap.getWidth();
      mColorCameraPreview.setImageBitmap(bitmap);
      mColorCameraPreview.setRotation(90);
      mColorCameraPreview.setScaleX(scale);
      mColorCameraPreview.setScaleY(scale);
      mDepthCameraPreview.setRotation(90);
      mDepthCameraPreview.setScaleX(scale);
      mDepthCameraPreview.setScaleY(scale);
    });
    image.close();
  }

  private void onProcessDepthData(Image image) {
    if (mShowDepth) {
      Bitmap preview = ARCoreUtils.getDepthPreview(image, true);
      mActivity.runOnUiThread(() -> mDepthCameraPreview.setImageBitmap(preview));
    }

    Pose pose;
    synchronized (mLock) {
      float[] position = new float[3];
      float[] rotation = new float[4];
      mPose.getTranslation(position, 0);
      mPose.getRotationQuaternion(rotation, 0);
      pose = new Pose(position, rotation);
    }

    if (mCache != null) {
      for (ARCoreUtils.Camera2DataListener listener : mListeners) {
        listener.onDepthDataReceived(image, pose, mFrameIndex);
      }
      for (ARCoreUtils.Camera2DataListener listener : mListeners) {
        listener.onColorDataReceived(mCache, mFrameIndex);
      }

      mCache = null;
      mFrameIndex++;
    }
    image.close();
  }

  private void closeCamera() {
    if (mSession != null) {
      mSession.pause();
      mSession.stop();
      mSession = null;
    }
  }

  private void openCamera() {

    //check permissions
    if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    if (mSession == null) {
      try {
        // Create AREngine session that supports camera sharing.
        mSession = new ARSession(mActivity);
      } catch (Exception e) {
        Log.e(TAG, "Failed to create AREngine session", e);
        return;
      }

      // Enable auto focus mode while AREngine is running.
      ARWorldTrackingConfig config = new ARWorldTrackingConfig(mSession);
      config.setEnableItem(ARConfigBase.ENABLE_DEPTH);
      config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
      config.setLightingMode(ARConfigBase.LightingMode.AMBIENT_INTENSITY);
      mSession.configure(config);
    }

    try {
      mSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to start AREngine", e);
    }
  }

  @Override
  public CameraCalibration getCalibration() {
    return mCameraCalibration;
  }

  @Override
  public float getLightIntensity() {
    return mPixelIntensity;
  }

  private void updateFrame(int texture, int width, int height) {
    try {
      if (mSession == null) {
        return;
      }

      //get calibration from AREngine
      mSession.setCameraTextureName(texture);
      mSession.setDisplayGeometry(0, width, height);
      ARFrame frame = mSession.update();
      ARCameraIntrinsics intrinsics = frame.getCamera().getCameraImageIntrinsics();
      mCameraCalibration.colorCameraIntrinsic[0] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mCameraCalibration.colorCameraIntrinsic[1] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mCameraCalibration.colorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      mCameraCalibration.colorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      mCameraCalibration.depthCameraIntrinsic = mCameraCalibration.colorCameraIntrinsic;
      mCameraCalibration.setValid();

      //get pose from AREngine
      synchronized (mLock) {
        ARCamera camera = frame.getCamera();
        mPose = camera.getPose();
      }

      //get light estimation from AREngine
      mPixelIntensity = frame.getLightEstimate().getPixelIntensity() * 2.0f;

      //process camera data
      onProcessColorData(frame.acquireCameraImage());
      onProcessDepthData(frame.acquireDepthImage());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}