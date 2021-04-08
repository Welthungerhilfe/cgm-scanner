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
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

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
import com.google.ar.core.SharedCamera;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.utils.LogFileUtils;

public class ARCoreCamera extends AbstractARCamera {

  private static final String TAG = ARCoreCamera.class.getSimpleName();

  //Camera2 API
  private ImageReader mImageReaderDepth16;
  private CameraDevice mCameraDevice;
  private String mColorCameraId;
  private String mDepthCameraId;
  private int mDepthWidth;
  private int mDepthHeight;

  //ARCore API
  private ArrayList<Float> mPlanes;
  private Session mSession;
  private final Object mLock;

  //App integration objects
  private GLSurfaceView mGLSurfaceView;
  private final HashMap<Long, Bitmap> mCache;

  public ARCoreCamera(Activity activity, boolean showDepth) {
    super(activity, showDepth);

    mCache = new HashMap<>();
    mLock = new Object();
    mPlanes = new ArrayList<>();
  }

  @Override
  public void onCreate(int colorPreview, int depthPreview, int surfaceview) {
    super.onCreate(colorPreview, depthPreview, surfaceview);

    //setup ARCore cycle
    mGLSurfaceView = mActivity.findViewById(surfaceview);
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
        synchronized (ARCoreCamera.this) {
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
          if (isARCoreSupportedAndUpToDate()) {
            setupDepthSensor();
            openCamera();
          }
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

    if (mDepthCameraId == null) {
      float[] position;
      float[] rotation;
      synchronized (mLock) {
        position = mPosition;
        rotation = mRotation;
      }

      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onDepthDataReceived(null, position, rotation, mFrameIndex);
      }
      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onColorDataReceived(bitmap, mFrameIndex);
      }
      mFrameIndex++;
    } else {
      synchronized (mCache) {
        mCache.put(image.getTimestamp(), bitmap);
      }
    }
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
    if (image == null) {
      Log.w(TAG, "onImageAvailable: Skipping null image.");
      return;
    }

    float[] position;
    float[] rotation;
    synchronized (mLock) {
      position = mPosition;
      rotation = mRotation;
    }

    if (mDepthMode != DepthPreviewMode.OFF) {
      mDepthCameraPreview.setImageBitmap(getDepthPreview(image, false, mPlanes, mDepthCameraIntrinsic, mPosition, mRotation));
    }

    Bitmap bitmap = null;
    long bestDiff = Long.MAX_VALUE;

    synchronized (mCache) {
      if (!mCache.isEmpty()) {
        for (Long timestamp : mCache.keySet()) {
          long diff = Math.abs(image.getTimestamp() - timestamp) / 1000; //in microseconds
          if (bestDiff > diff) {
            bestDiff = diff;
            bitmap = mCache.get(timestamp);
          }
        }
      }
    }

    if (bitmap != null && bestDiff < 50000) {
      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onDepthDataReceived(image, position, rotation, mFrameIndex);
      }
      for (Object listener : mListeners) {
        ((Camera2DataListener)listener).onColorDataReceived(bitmap, mFrameIndex);
      }

      synchronized (mCache) {
        mCache.clear();
        mFrameIndex++;
      }
    }
    image.close();
  }

  private void closeCamera() {
    if (mCameraDevice != null) {
      mCameraDevice.close();
      mCameraDevice = null;
    }
    if (mImageReaderDepth16 != null) {
      mImageReaderDepth16.setOnImageAvailableListener(null, null);
      mImageReaderDepth16.close();
      mImageReaderDepth16 = null;
    }
    if (mSession != null) {
      mSession.pause();
      mSession.close();
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
        // Create ARCore session that supports camera sharing.
        mSession = new Session(mActivity, EnumSet.of(Session.Feature.SHARED_CAMERA));
      } catch (Exception e) {
        Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
        return;
      }

      // Set calibration image
      AugmentedImageDatabase db = new AugmentedImageDatabase(mSession);
      try {
        Bitmap b = BitmapFactory.decodeStream(mGLSurfaceView.getContext().getAssets().open("earth.jpg"));
        db.addImage("earth", b);
      } catch (Exception e) {
        LogFileUtils.logException(e);
      }

      // Enable auto focus mode while ARCore is running.
      Config config = mSession.getConfig();
      config.setAugmentedImageDatabase(db);
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
          if (Math.abs(mDepthWidth / (float)mDepthHeight - w / (float)h) < 0.0001f) {
            rank += 2;
          }
          if (cameraConfig.getDepthSensorUsage() == CameraConfig.DepthSensorUsage.DO_NOT_USE) {
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
    }

    // Store the ARCore shared camera reference.
    SharedCamera sharedCamera = mSession.getSharedCamera();

    // Store the ID of the camera used by ARCore.
    mColorCameraId = mSession.getCameraConfig().getCameraId();

    // When ARCore is running, make sure it also updates our CPU image surface.
    if (mDepthCameraId != null) {
      if (mColorCameraId.compareTo(mDepthCameraId) == 0) {
        ArrayList<Surface> surfaces = new ArrayList<>();
        surfaces.add(mImageReaderDepth16.getSurface());
        sharedCamera.setAppSurfaces(mColorCameraId, surfaces);
      } else {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
          manager.openCamera(mDepthCameraId, mSeparatedCameraCallback, null);
        } catch (CameraAccessException e) {
          e.printStackTrace();
        } catch (Exception e) {
          throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
      }
    }

    try {
      mSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to start ARCore", e);
    }
  }

  private void setupDepthSensor() {
    //set depth camera resolution
    mDepthWidth = 240;
    mDepthHeight = 180;
    if (Build.MANUFACTURER.toUpperCase().startsWith("SAMSUNG")) {
      //all supported Samsung devices except S10 5G have VGA resolution
      if (!Build.MODEL.startsWith("beyondx")) {
        mDepthWidth = 320;
        mDepthHeight = 240;
      }
    }

    //detect depth camera
    try {
      CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
      for (String cameraId : manager.getCameraIdList()) {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null) {
          if (facing == CameraCharacteristics.LENS_FACING_BACK) {
            int[] ch = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (ch != null) {
              for (int c : ch) {
                if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                  mDepthCameraId = cameraId;
                  mDepthCameraTranslation = characteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION);
                  mDepthCameraIntrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                  if (mDepthCameraIntrinsic != null) {
                    mDepthCameraIntrinsic[0] /= (float)mDepthWidth;
                    mDepthCameraIntrinsic[1] /= (float)mDepthHeight;
                    mDepthCameraIntrinsic[2] /= (float)mDepthWidth;
                    mDepthCameraIntrinsic[3] /= (float)mDepthHeight;
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //set image reader
    mImageReaderDepth16 = ImageReader.newInstance(mDepthWidth, mDepthHeight, ImageFormat.DEPTH16, 5);
    mImageReaderDepth16.setOnImageAvailableListener(imageReader -> onProcessDepthData(imageReader.acquireLatestImage()), null);
  }

  private boolean isARCoreSupportedAndUpToDate() {
    // Make sure ARCore is installed and supported on this device.
    ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(mActivity);
    switch (availability) {
      case SUPPORTED_INSTALLED:
        break;
      case SUPPORTED_APK_TOO_OLD:
      case SUPPORTED_NOT_INSTALLED:
        try {
          // Request ARCore installation or update if needed.
          ArCoreApk.InstallStatus installStatus =
                  ArCoreApk.getInstance().requestInstall(mActivity, true);
          switch (installStatus) {
            case INSTALL_REQUESTED:
              Log.e(TAG, "ARCore installation requested.");
              return false;
            case INSTALLED:
              break;
          }
        } catch (Exception e) {
          Log.e(TAG, "ARCore not installed", e);
          mActivity.finish();
          return false;
        }
        break;
      case UNKNOWN_ERROR:
      case UNKNOWN_CHECKING:
      case UNKNOWN_TIMED_OUT:
      case UNSUPPORTED_DEVICE_NOT_CAPABLE:
        Log.e(TAG, "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned " + availability);
        mActivity.finish();
        return false;
    }
    return true;
  }

  private void updateFrame(int texture, int width, int height) {
    try {
      if (mSession == null) {
        return;
      }

      //get calibration from ARCore
      mSession.setCameraTextureName(texture);
      mSession.setDisplayGeometry(0, width, height);
      Frame frame = mSession.update();
      CameraIntrinsics intrinsics = frame.getCamera().getImageIntrinsics();
      mColorCameraIntrinsic[0] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[1] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mColorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      mColorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      if ((mDepthCameraId != null) && (mColorCameraId.compareTo(mDepthCameraId) == 0)) {
        mDepthCameraIntrinsic = mColorCameraIntrinsic;
      }
      mHasCameraCalibration = true;

      //get calibration image dimension
      for (AugmentedImage img : frame.getUpdatedTrackables(AugmentedImage.class)) {
        Log.d("XXX", img.getExtentX() + "x" + img.getExtentZ());
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

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private CameraDevice.StateCallback mSeparatedCameraCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice cameraDevice) {
      Surface imageReaderSurface = mImageReaderDepth16.getSurface();
      mCameraDevice = cameraDevice;

      try {
        final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        requestBuilder.addTarget(imageReaderSurface);

        cameraDevice.createCaptureSession(Collections.singletonList(imageReaderSurface),new CameraCaptureSession.StateCallback() {

          @Override
          public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            try {
              cameraCaptureSession.setRepeatingRequest(requestBuilder.build(),null,null);
            } catch (CameraAccessException e) {
              e.printStackTrace();
            }
          }
          @Override
          public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
          }
        },null);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }
    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
    }
    @Override
    public void onError(CameraDevice cameraDevice, int i) {
    }
  };
}
