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
package de.welthungerhilfe.cgm.scanner.hardware.camera;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.Image;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;

import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARAugmentedImageDatabase;
import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARCameraIntrinsics;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARCoordinateSystemType;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.ARWorldBodyTrackingConfig;
import com.huawei.hiar.ARWorldTrackingConfig;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.utils.ComputerVisionUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AREngineCamera extends AbstractARCamera {

  private static final String TAG = AREngineCamera.class.getSimpleName();

  private static final String ACTION_HUAWEI_DOWNLOAD_QUIK = "com.huawei.appmarket.intent.action.AppDetail";

  private static final String HUAWEI_MARTKET_NAME = "com.huawei.appmarket";

  private static final String PACKAGE_NAME_KEY = "APP_PACKAGENAME";

  private static final String PACKAGENAME_ARSERVICE = "com.huawei.arengine.service";

  //AREngine API
  private Bitmap mCache;
  private boolean mFirstRequest;
  private int mOrientation;
  private int mPersonCount = 0;
  private ARSession mSession;
  private ArrayList<Float> mPlanes;
  private ArrayList<PointF> mSkeleton;
  private boolean mSkeletonValid;

  public AREngineCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
    super(activity, depthMode, previewSize);
    mPlanes = new ArrayList<>();
    mSkeleton = new ArrayList<>();

    OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity)
    {
      @Override
      public void onOrientationChanged(int orientation)
      {
        mOrientation = orientation;
      }
    };

    if (orientationEventListener.canDetectOrientation()) {
      orientationEventListener.enable();
    }
  }

  private void onProcessColorData(Bitmap bitmap) {
    if (bitmap == null) {
      return;
    }

    mCache = bitmap;

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
  }

  private void onProcessDepthData(Image image) {
    float[] position;
    float[] rotation;
    synchronized (mLock) {
      position = mPosition;
      rotation = mRotation;
    }

    if (!hasCameraCalibration()) {
      image.close();
      return;
    }

    Bitmap preview = getDepthPreview(image, mPlanes, mColorCameraIntrinsic, mPosition, mRotation);
    Bitmap finalPreview = skeletonVisualisation(preview, true);
    mActivity.runOnUiThread(() -> mDepthCameraPreview.setImageBitmap(finalPreview));

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

  @Override
  protected ByteOrder getDepthByteOrder() {
    return ByteOrder.LITTLE_ENDIAN;
  }

  @Override
  protected void closeCamera() {
    if (mSession != null) {
      mSession.pause();
      mSession.stop();
      mSession = null;
    }
  }

  @Override
  protected void openCamera() {

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

      // Set calibration image
      ARAugmentedImageDatabase db = new ARAugmentedImageDatabase(mSession);
      try {
        Bitmap b = BitmapFactory.decodeStream(mGLSurfaceView.getContext().getAssets().open(CALIBRATION_IMAGE_FILE));
        db.addImage("image", b);
      } catch (Exception e) {
        LogFileUtils.logException(e);
      }

      // Set AR configuration
      ARConfigBase config;
      if (mDepthMode == DepthPreviewMode.CALIBRATION) {
        ARWorldTrackingConfig worldTrackingConfig = new ARWorldTrackingConfig(mSession);
        worldTrackingConfig.setAugmentedImageDatabase(db);
        worldTrackingConfig.setPlaneFindingMode(ARConfigBase.PlaneFindingMode.HORIZONTAL_ONLY);
        config = worldTrackingConfig;
      } else {
        ARWorldBodyTrackingConfig bodyTrackingConfig = new ARWorldBodyTrackingConfig(mSession);
        bodyTrackingConfig.setPlaneFindingMode(ARConfigBase.PlaneFindingMode.HORIZONTAL_ONLY);
        config = bodyTrackingConfig;
      }
      config.setEnableItem(ARConfigBase.ENABLE_DEPTH);
      config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
      config.setLightingMode(ARConfigBase.LightingMode.AMBIENT_INTENSITY);
      config.setPowerMode(ARConfigBase.PowerMode.PERFORMANCE_FIRST);
      config.setUpdateMode(ARConfigBase.UpdateMode.BLOCKING);
      mSession.configure(config);

      // Get GPU image resolution
      mTextureRes = mSession.getCameraConfig().getTextureDimensions();
      Log.d(TAG, "AREngine started with RGB " + mTextureRes.getWidth() + "x" + mTextureRes.getHeight());
    }

    try {
      mSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to start AREngine", e);
    }
  }

  @Override
  protected void updateFrame() {
    try {
      if (mSession == null) {
        return;
      }

      if (mViewportChanged) {
        mSession.setCameraTextureName(mCameraTextureId);
        mSession.setDisplayGeometry(0, mViewportWidth, mViewportHeight);
        mViewportChanged = false;
      }

      //get calibration from AREngine
      ARFrame frame = mSession.update();
      ARCameraIntrinsics intrinsics = frame.getCamera().getCameraImageIntrinsics();
      mColorCameraIntrinsic[0] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mColorCameraIntrinsic[1] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      mColorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      if (Build.MODEL.startsWith("VOG") && (Build.VERSION.SDK_INT <= 28)) {
        mColorCameraIntrinsic[0] = 0.77124846f;
        mColorCameraIntrinsic[1] = 1.0283293f;
        mColorCameraIntrinsic[2] = 0.4991906f;
        mColorCameraIntrinsic[3] = 0.5016229f;
      }
      mDepthCameraIntrinsic = mColorCameraIntrinsic;
      mHasCameraCalibration = true;

      //get calibration image dimension
      for (ARAugmentedImage img : frame.getUpdatedTrackables(ARAugmentedImage.class)) {
        ARPose[] localBoundaryPoses = {
                ARPose.makeTranslation(
                        -0.5f * img.getExtentX(),
                        0.0f,
                        -0.5f * img.getExtentZ()), // upper left
                ARPose.makeTranslation(
                        0.5f * img.getExtentX(),
                        0.0f,
                        -0.5f * img.getExtentZ()), // upper right
                ARPose.makeTranslation(
                        0.5f * img.getExtentX(),
                        0.0f,
                        0.5f * img.getExtentZ()), // lower right
                ARPose.makeTranslation(
                        -0.5f * img.getExtentX(),
                        0.0f,
                        0.5f * img.getExtentZ()) // lower left
        };
        mCalibrationImageEdges = new ComputerVisionUtils.Point3F[4];
        for (int i = 0; i < 4; ++i) {
          ARPose p = img.getCenterPose().compose(localBoundaryPoses[i]);
          mCalibrationImageEdges[i] = new ComputerVisionUtils.Point3F(p.tx(), p.ty(), p.tz());
        }
      }

      //get planes
      mPlanes.clear();
      for (ARPlane plane : mSession.getAllPlanes()) {
        mPlanes.add(plane.getCenterPose().ty());
      }

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
        color = mRTT.renderData(mCameraTextureId, mTextureRes);
        depth = frame.acquireDepthImage();
      } catch (Exception e) {
        e.printStackTrace();
        installAREngine();
      }

      //process camera data
      getBodySkeleton();
      onProcessColorData(color);
      onProcessDepthData(depth);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int getPersonCount() {
    return mPersonCount;
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

  private void getBodySkeleton() {
    mPersonCount = 0;
    mSkeleton.clear();
    mSkeletonValid = true;
    if ((mOrientation < 45) || (mOrientation > 315)) {
      Collection<ARBody> bodies = mSession.getAllTrackables(ARBody.class);
      for (ARBody body : bodies) {
        if (body.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
          continue;
        }
        if (body.getCoordinateSystemType() != ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
          continue;
        }
        mPersonCount++;

        //TODO:apply frame.transformDisplayUvCoords instead of yOffset and yScale
        float yOffset = 0.16f;
        float yScale = 0.75f;
        float[] points = body.getSkeletonPoint2D();
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i : body.getBodySkeletonConnection()) {
          indices.add(i);
        }
        indices.add(ARBody.ARBodySkeletonType.BodySkeleton_l_Sho.ordinal() - 1);
        indices.add(ARBody.ARBodySkeletonType.BodySkeleton_l_Hip.ordinal() - 1);
        indices.add(ARBody.ARBodySkeletonType.BodySkeleton_r_Sho.ordinal() - 1);
        indices.add(ARBody.ARBodySkeletonType.BodySkeleton_r_Hip.ordinal() - 1);
        for (int i : indices) {
          float x = points[i * 3 + 1] * -0.5f + 0.5f;
          float y = points[i * 3] * -0.5f + 0.5f;
          if ((x > 0) && (y > 0)) {
            mSkeleton.add(new PointF(x, (y + yOffset) * yScale));
          } else {
            mSkeleton.add(new PointF(0, 0));
            mSkeletonValid = false;
          }
        }
      }
    }
  }

  private Bitmap skeletonVisualisation(Bitmap preview, boolean outline) {

    //skeleton visualisation
    if (mPersonCount == 1) {
      if (preview.getWidth() == 1) {
        preview = Bitmap.createBitmap(240, 180, Bitmap.Config.ARGB_8888);
      }
      preview = preview.copy(Bitmap.Config.ARGB_8888, true);
      Canvas c = new Canvas(preview);

      //define look of the visualisation
      int color = Color.argb(128, 0, 255, 0);
      if ((mTargetDistance < AppConstants.TOO_NEAR) || (mTargetDistance > AppConstants.TOO_FAR)) {
        color = Color.argb(128, 255, 255, 0);
      } else if (!mSkeletonValid) {
        color = Color.argb(128, 255, 255, 0);
      }

      //draw bones
      for (int pass = 0; pass < (outline ? 2 : 1); pass++) {
        int r = outline ? (pass == 0 ? 20 : 15) : 1;
        Paint p = new Paint();
        p.setColor(pass == 0 ? color : Color.BLACK);
        p.setStrokeWidth(r * 2);
        for (int i = 0; i < mSkeleton.size(); i += 2) {
          int x1 = (int) (mSkeleton.get(i).x * (float)preview.getWidth());
          int y1 = (int) (mSkeleton.get(i).y * (float)preview.getHeight());
          int x2 = (int) (mSkeleton.get(i + 1).x * (float)preview.getWidth());
          int y2 = (int) (mSkeleton.get(i + 1).y * (float)preview.getHeight());
          if ((x1 != 0) && (y1 != 0) && (x2 != 0) && (y2 != 0)) {
            if (outline) {
              c.drawCircle(x1, y1, r, p);
              c.drawCircle(x2, y2, r, p);
            }
            c.drawLine(x1, y1, x2, y2, p);
          }
        }
      }

      //masking which creates outline
      if (outline) {
        int w = preview.getWidth();
        int h = preview.getHeight();
        int[] pixels = new int[w * h];
        preview.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
          if (pixels[i] == Color.BLACK) {
            pixels[i] = Color.TRANSPARENT;
          }
        }
        preview.setPixels(pixels, 0, w, 0, 0, w, h);
      }
    }
    return preview;
  }

  public static boolean shouldUseAREngine() {
    String manufacturer = Build.MANUFACTURER.toUpperCase();
    return manufacturer.startsWith("HONOR") || manufacturer.startsWith("HUAWEI");
  }
}
