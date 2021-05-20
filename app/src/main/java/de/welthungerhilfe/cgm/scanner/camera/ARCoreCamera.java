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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.widget.ImageView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.utils.LogFileUtils;

public class ARCoreCamera extends AbstractARCamera implements GLSurfaceView.Renderer {

  private static final String TAG = ARCoreCamera.class.getSimpleName();

  //ARCore API
  private int mCameraTextureId;
  private boolean mInstallRequested;
  private ArrayList<Float> mPlanes;
  private Session mSession;
  private boolean mViewportChanged;
  private int mViewportWidth;
  private int mViewportHeight;
  private final Object mLock;

  public ARCoreCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
    super(activity, depthMode, previewSize);
    mLock = new Object();
    mPlanes = new ArrayList<>();
  }

  @Override
  public void onCreate(ImageView colorPreview, ImageView depthPreview, GLSurfaceView surfaceview) {
    super.onCreate(colorPreview, depthPreview, surfaceview);

    // Set up renderer.
    mGLSurfaceView.setPreserveEGLContextOnPause(true);
    mGLSurfaceView.setEGLContextClientVersion(2);
    mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    mGLSurfaceView.setRenderer(this);
    mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    mGLSurfaceView.setWillNotDraw(false);
  }

  @Override
  public synchronized void onResume() {

    if (mSession == null) {
      try {
        switch (ArCoreApk.getInstance().requestInstall(mActivity, !mInstallRequested)) {
          case INSTALL_REQUESTED:
            mInstallRequested = true;
            return;
          case INSTALLED:
            break;
        }

        mSession = new Session(/* context = */ mActivity);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }


      // Set calibration image
      AugmentedImageDatabase db = new AugmentedImageDatabase(mSession);
      try {
        Bitmap b = BitmapFactory.decodeStream(mGLSurfaceView.getContext().getAssets().open(CALIBRATION_IMAGE_FILE));
        db.addImage("image", b);
      } catch (Exception e) {
        LogFileUtils.logException(e);
      }

      // Enable auto focus mode while ARCore is running.
      Config config = mSession.getConfig();
      config.setAugmentedImageDatabase(db);
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
      config.setFocusMode(Config.FocusMode.AUTO);
      config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
      config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
      mSession.configure(config);

      // Choose the camera configuration
      int selectedRank = -1;
      CameraConfig selectedConfig = null;
      for (CameraConfig cameraConfig : mSession.getSupportedCameraConfigs()) {
        if (cameraConfig.getFacingDirection() == CameraConfig.FacingDirection.BACK) {
          Size resolution = cameraConfig.getImageSize();
          int w = resolution.getWidth();
          int h = resolution.getHeight();

          int rank = 0;
          if ((w > 1024) && (h > 1024)) {
            rank += 1;
          }
          if (cameraConfig.getDepthSensorUsage() == CameraConfig.DepthSensorUsage.REQUIRE_AND_USE) {
            rank += 2;
          }

          if (selectedRank < rank) {
            selectedRank = rank;
            selectedConfig = cameraConfig;
          }
        }
      }
      assert selectedConfig != null;
      mSession.setCameraConfig(selectedConfig);
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      mSession.resume();
    } catch (Exception e) {
      mSession = null;
      return;
    }
    mGLSurfaceView.onResume();
  }

  @Override
  public synchronized void onPause() {
    if (mSession != null) {
      mGLSurfaceView.onPause();
      mSession.pause();
    }
  }

  private void onProcessColorData(Image image) {
    final ByteBuffer yuvBytes = imageToByteBuffer(image);

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

    for (Object listener : mListeners) {
      ((Camera2DataListener)listener).onColorDataReceived(bitmap, mFrameIndex);
    }
    allocationYuv.destroy();
    allocationRgb.destroy();
    rs.destroy();

    //update preview window
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
    image.close();
  }

  private void onProcessDepthData(Image image) {
    if (image == null) {
      Log.w(TAG, "onImageAvailable: Skipping null image.");
      return;
    }

    if (!hasCameraCalibration()) {
      image.close();
      return;
    }

    float[] position;
    float[] rotation;
    synchronized (mLock) {
      position = mPosition;
      rotation = mRotation;
    }

    Bitmap preview = getDepthPreview(image, mPlanes, mColorCameraIntrinsic, mPosition, mRotation);
    mActivity.runOnUiThread(() -> mDepthCameraPreview.setImageBitmap(preview));

    for (Object listener : mListeners) {
      ((Camera2DataListener)listener).onDepthDataReceived(image, position, rotation, mFrameIndex);
    }
    image.close();
  }

  private void updateFrame(Frame frame) {
    try {
      //get calibration from ARCore
      CameraIntrinsics intrinsics = frame.getCamera().getImageIntrinsics();
      mColorCameraIntrinsic[0] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[1] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mColorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      mDepthCameraIntrinsic = mColorCameraIntrinsic;
      mHasCameraCalibration = true;

      //get calibration image dimension
      mCalibrationImageSizeCV = null;
      for (AugmentedImage img : frame.getUpdatedTrackables(AugmentedImage.class)) {
        Pose[] localBoundaryPoses = {
                Pose.makeTranslation(
                        -0.5f * img.getExtentX(),
                        0.0f,
                        -0.5f * img.getExtentZ()), // upper left
                Pose.makeTranslation(
                        0.5f * img.getExtentX(),
                        0.0f,
                        -0.5f * img.getExtentZ()), // upper right
                Pose.makeTranslation(
                        0.5f * img.getExtentX(),
                        0.0f,
                        0.5f * img.getExtentZ()), // lower right
                Pose.makeTranslation(
                        -0.5f * img.getExtentX(),
                        0.0f,
                        0.5f * img.getExtentZ()) // lower left
        };
        mCalibrationImageEdges = new Point3F[4];
        for (int i = 0; i < 4; ++i) {
          Pose p = img.getCenterPose().compose(localBoundaryPoses[i]);
          mCalibrationImageEdges[i] = new Point3F(p.tx(), p.ty(), p.tz());
        }
        mCalibrationImageSizeCV = new SizeF(img.getExtentX(), img.getExtentZ());
      }

      //get pose from ARCore
      synchronized (mLock) {
        Camera camera = frame.getCamera();
        Pose pose = camera.getPose();
        mPosition = pose.getTranslation();
        mRotation = pose.getRotationQuaternion();
      }

      //get planes
      mPlanes.clear();
      for (Plane plane : mSession.getAllTrackables(Plane.class)) {
        mPlanes.add(plane.getCenterPose().ty());
      }

      //get light estimation from ARCore
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

      //process camera data
      onProcessColorData(frame.acquireCameraImage());
      onProcessDepthData(frame.acquireRawDepthImage());
      mFrameIndex++;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Create the texture and pass it to ARCore session to be filled during update().
    // Generate the background texture.
    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    mCameraTextureId = textures[0];
    int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    GLES20.glBindTexture(textureTarget, mCameraTextureId);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    GLES20.glViewport(0, 0, width, height);
    mViewportWidth = width;
    mViewportHeight = height;
    mViewportChanged = true;
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    if (mSession == null) {
      return;
    }

    if (mViewportChanged) {
      mSession.setDisplayGeometry(0, mViewportWidth, mViewportHeight);
      mViewportChanged = false;
    }

    try {
      mSession.setCameraTextureName(mCameraTextureId);

      updateFrame(mSession.update());
    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }
}
