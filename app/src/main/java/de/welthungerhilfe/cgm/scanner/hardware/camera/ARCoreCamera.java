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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.util.EnumSet;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;

public class ARCoreCamera extends AbstractARCamera {

  private static final String TAG = ARCoreCamera.class.getSimpleName();

  //ARCore API
  private boolean mInstallRequested;
  private Session mSession;

  public ARCoreCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
    super(activity, depthMode, previewSize);
  }

  @Override
  protected void closeCamera() {
    if (mSession != null) {
      mSession.pause();
    }
  }

  @Override
  protected void openCamera() {
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
        CameraConfigFilter filter = new CameraConfigFilter(mSession);
        filter.setFacingDirection(CameraConfig.FacingDirection.BACK);
        filter.setStereoCameraUsage(EnumSet.of(CameraConfig.StereoCameraUsage.REQUIRE_AND_USE));
        List<CameraConfig> configs = mSession.getSupportedCameraConfigs(filter);
        Log.d(TAG, "Found " + configs.size() + " configs supporting stereo camera");
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
        LogFileUtils.logException(e,"ARCORE CAMERA");
      }

      // Enable auto focus mode while ARCore is running.
      Config config = mSession.getConfig();
      config.setAugmentedImageDatabase(db);
      if (mSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
        config.setDepthMode(Config.DepthMode.AUTOMATIC);
      } else {
        config.setDepthMode(Config.DepthMode.DISABLED);
      }
      config.setFocusMode(Config.FocusMode.AUTO);
      config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
      config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
      config.setUpdateMode(Config.UpdateMode.BLOCKING);
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
          if (cameraConfig.getStereoCameraUsage() == CameraConfig.StereoCameraUsage.REQUIRE_AND_USE) {
            rank += 2;
          }
          if (cameraConfig.getDepthSensorUsage() == CameraConfig.DepthSensorUsage.REQUIRE_AND_USE) {
            rank += 4;
          }

          if (selectedRank < rank) {
            selectedRank = rank;
            selectedConfig = cameraConfig;
          }
        }
      }
      assert selectedConfig != null;
      mSession.setCameraConfig(selectedConfig);

      // Get GPU image resolution
      mTextureRes = selectedConfig.getTextureSize();
      Log.d(TAG, "ARCore started with RGB " + mTextureRes.getWidth() + "x" + mTextureRes.getHeight());
    }

    try {
      mSession.resume();
    } catch (Exception e) {
      mSession = null;
    }
  }

  @Override
  protected void updateFrame() {
    if (mSession == null) {
      return;
    }

    try {
      if (mViewportChanged) {
        mSession.setDisplayGeometry(0, mViewportWidth, mViewportHeight);
        mViewportChanged = false;
      }
      mSession.setCameraTextureName(mCameraTextureId);
      Frame frame = mSession.update();

      //get calibration from ARCore
      CameraIntrinsics intrinsics = frame.getCamera().getImageIntrinsics();
      /*LogFileUtils.logInfo("ARCORE","arcore step1 intrinsics value "+intrinsics);
      LogFileUtils.logInfo("ARCORE","arcore step1 focalLength value "+intrinsics.getFocalLength()[0]+" "+intrinsics.getFocalLength()[1]);
      LogFileUtils.logInfo("ARCORE","arcore step1 imagedimension value "+intrinsics.getImageDimensions()[0]+" "+intrinsics.getImageDimensions()[1]);
      LogFileUtils.logInfo("ARCORE","arcore step1 principalpoint value "+intrinsics.getPrincipalPoint()[0]+" "+intrinsics.getPrincipalPoint()[0]);
*/
      mColorCameraIntrinsic[0] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[1] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mColorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      mDepthCameraIntrinsic = mColorCameraIntrinsic;
      mHasCameraCalibration = true;
     // LogFileUtils.logInfo("ARCORE","arcore step2 camerainstrics value "+mColorCameraIntrinsic[0]+" "+mColorCameraIntrinsic[1]+" "+mColorCameraIntrinsic[2]+" "+mColorCameraIntrinsic[3]);


      //get calibration image dimension
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
        mCalibrationImageEdges = new ComputerVisionUtils.Point3F[4];
        for (int i = 0; i < 4; ++i) {
          Pose p = img.getCenterPose().compose(localBoundaryPoses[i]);
          mCalibrationImageEdges[i] = new ComputerVisionUtils.Point3F(p.tx(), p.ty(), p.tz());
        }
      }

      //get pose from ARCore
      Camera camera = frame.getCamera();
      Pose pose = camera.getPose();

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

      //get camera data
      Bitmap color = null;
      Depthmap depth = null;
      try {
        color = mRTT.renderData(mCameraTextureId, mTextureRes);
        if (hasCameraCalibration() && mSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
          if (mFrameIndex % AppConstants.SCAN_FRAMESKIP == 0) {
            Image image = frame.acquireRawDepthImage();
            depth = updateDepthmap(image, pose.getTranslation(), pose.getRotationQuaternion());
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      //process camera data
      onProcessColorData(color);
      onProcessDepthData(depth);
      mFrameIndex++;

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int getPersonCount() {
    //unsupported on ARCore
    return 1;
  }

  @Override
  public float getTargetDistance() {
    //unsupported on ARCore
    return 1;
  }

  @Override
  public float getOrientation() {
    return 1;
  }
}
