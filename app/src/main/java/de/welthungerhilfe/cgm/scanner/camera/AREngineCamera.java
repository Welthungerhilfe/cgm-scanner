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
package de.welthungerhilfe.cgm.scanner.camera;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.utils.RenderToTexture;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AREngineCamera extends AbstractARCamera {

  private static final String TAG = AREngineCamera.class.getSimpleName();

  private static final String ACTION_HUAWEI_DOWNLOAD_QUIK = "com.huawei.appmarket.intent.action.AppDetail";

  private static final String HUAWEI_MARTKET_NAME = "com.huawei.appmarket";

  private static final String PACKAGE_NAME_KEY = "APP_PACKAGENAME";

  private static final String PACKAGENAME_ARSERVICE = "com.huawei.arengine.service";

  //AREngine API
  private ARSession mSession;
  private RenderToTexture mRTT;
  private Size mTextureRes;
  private boolean mFirstRequest;

  //App integration objects
  private GLSurfaceView mGLSurfaceView;
  private Bitmap mCache;

  public AREngineCamera(Activity activity, boolean showDepth) {
    super(activity, showDepth);
    mRTT = new RenderToTexture();
  }

  @Override
  public void onCreate(int colorPreview, int depthPreview, int surfaceview) {
    super.onCreate(colorPreview, depthPreview, surfaceview);

    //setup AREngine cycle
    mGLSurfaceView = mActivity.findViewById(surfaceview);
    mGLSurfaceView.setEGLContextClientVersion(3);
    mGLSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
      private final int[] textures = new int[1];
      private int width, height;

      @Override
      public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
        mRTT.init();
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

  private void onProcessColorData(Bitmap bitmap) {
    if (bitmap == null) {
      return;
    }

    mCache = bitmap;

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
  }

  private void onProcessDepthData(Image image) {
    if (mShowDepth) {
      Bitmap preview = getDepthPreview(image, true);
      mActivity.runOnUiThread(() -> mDepthCameraPreview.setImageBitmap(preview));
    }

    float[] position;
    float[] rotation;
    synchronized (mLock) {
      position = mPosition;
      rotation = mRotation;
    }

    if (mCache != null) {
      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onDepthDataReceived(image, position, rotation, mFrameIndex);
      }
      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onColorDataReceived(mCache, mFrameIndex);
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

      // Get GPU image resolution
      mTextureRes = mSession.getCameraConfig().GetTextureDimensions();
      Log.d(TAG, "AREngine started with RGB " + mTextureRes.getWidth() + "x" + mTextureRes.getHeight());
    }

    try {
      mSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to start AREngine", e);
    }
  }

  private void installAREngine() {
    if (!mFirstRequest) {
      return;
    }
    mFirstRequest = false;

    new Thread(() -> {
      mActivity.runOnUiThread(() -> {
        try {
          Intent intent = new Intent(ACTION_HUAWEI_DOWNLOAD_QUIK);
          intent.putExtra(PACKAGE_NAME_KEY, PACKAGENAME_ARSERVICE);
          intent.setPackage(HUAWEI_MARTKET_NAME);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          mActivity.startActivity(intent);
        } catch (SecurityException e) {
          Log.w(TAG, "the target app has no permission of media");
        } catch (ActivityNotFoundException e) {
          Log.w(TAG, "the target activity is not found: " + e.getMessage());
        }
      });
      Utils.sleep(100);
      mActivity.finish();
    }).start();
  }

  public static boolean shouldUseAREngine() {
    String manufacturer = Build.MANUFACTURER.toUpperCase();
    return manufacturer.startsWith("HONOR") || manufacturer.startsWith("HUAWEI");
  }

  private void updateFrame(int texture, int width, int height) {
    try {
      if (mSession == null) {
        return;
      }

      //get calibration from AREngine
      mSession.setCameraTextureName(texture);
      mSession.setDisplayGeometry(0, width, height);
      float[] projection = new float[16];
      ARFrame frame = mSession.update();
      frame.getCamera().getProjectionMatrix(projection, 0, 0.001f, 100);
      mColorCameraIntrinsic[0] = Math.abs(projection[5] / 2.0f);
      mColorCameraIntrinsic[1] = Math.abs(projection[0] / 2.0f);
      mColorCameraIntrinsic[2] = Math.abs((1.0f - projection[9]) / 2.0f);
      mColorCameraIntrinsic[3] = Math.abs((1.0f - projection[8]) / 2.0f);
      mDepthCameraIntrinsic = mColorCameraIntrinsic;
      mHasCameraCalibration = true;

      //get pose from AREngine
      synchronized (mLock) {
        ARCamera camera = frame.getCamera();
        ARPose pose = camera.getPose();
        pose.getTranslation(mPosition, 0);
        pose.getRotationQuaternion(mRotation, 0);
      }

      //get light estimation from AREngine
      mPixelIntensity = frame.getLightEstimate().getPixelIntensity() * 2.0f;
      if (mPixelIntensity > 1.5f) {
        mLight = LightConditions.BRIGHT;
        mLastBright = System.currentTimeMillis();
      }
      if (mPixelIntensity < 0.25f) {
        mLight = LightConditions.DARK;
        mLastDark = System.currentTimeMillis();
      }
      if (mSessionStart == 0) {
        mSessionStart = System.currentTimeMillis();
      }

      //get camera data
      Bitmap color = null;
      Image depth = null;
      try {
        color = mRTT.renderData(texture, mTextureRes);
        depth = frame.acquireDepthImage();
      } catch (Exception e) {
        e.printStackTrace();
        installAREngine();
      }

      //process camera data
      onProcessColorData(color);
      onProcessDepthData(depth);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
