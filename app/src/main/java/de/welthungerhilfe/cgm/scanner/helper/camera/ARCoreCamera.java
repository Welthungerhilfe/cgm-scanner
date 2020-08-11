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
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.ImageView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;

public class ARCoreCamera implements ICamera {

  private static final boolean DEPTH_VISUALISATION_ENABLED = false;

  public static class CameraCalibration {
    private float[] colorCameraIntrinsic;
    private float[] depthCameraIntrinsic;
    private float[] depthCameraTranslation;
    private boolean valid;

    private CameraCalibration() {
      colorCameraIntrinsic = new float[4];
      depthCameraIntrinsic = new float[4];
      depthCameraTranslation = new float[3];
      valid = false;
    }

    public float[] getIntrinsic(boolean rgbCamera) {
      return rgbCamera ? colorCameraIntrinsic : depthCameraIntrinsic;
    }

    public boolean isValid() {
      return valid;
    }

    private void setValid() {
      valid = true;
    }

    @Override
    public String toString() {
      String output = "";
      output += "Color camera intrinsic:\n";
      output += colorCameraIntrinsic[0] + " " + colorCameraIntrinsic[1] + " " + colorCameraIntrinsic[2] + " " + colorCameraIntrinsic[3] + "\n";
      output += "Depth camera intrinsic:\n";
      output += depthCameraIntrinsic[0] + " " + depthCameraIntrinsic[1] + " " + depthCameraIntrinsic[2] + " " + depthCameraIntrinsic[3] + "\n";
      output += "Depth camera position:\n";
      output += depthCameraTranslation[0] + " " + depthCameraTranslation[1] + " " + depthCameraTranslation[2] + "\n";
      return output;
    }
  }


  public interface Camera2DataListener
  {
    void onColorDataReceived(Bitmap bitmap, int frameIndex);

    void onDepthDataReceived(Image image, Pose pose, int frameIndex);
  }

  private static final String TAG = ARCoreCamera.class.getSimpleName();

  //Camera2 API
  private ImageReader mImageReaderDepth16;
  private CameraDevice mCameraDevice;
  private String mColorCameraId;
  private String mDepthCameraId;
  private int mDepthWidth;
  private int mDepthHeight;

  //ARCore API
  private Session mSession;
  private Pose mPose;
  private final Object mLock;

  //App integration objects
  private Activity mActivity;
  private ImageView mColorCameraPreview;
  private ImageView mDepthCameraPreview;
  private GLSurfaceView mGLSurfaceView;
  private ArrayList<Camera2DataListener> mListeners;
  private final HashMap<Long, Bitmap> mCache;
  private CameraCalibration mCameraCalibration;
  private int mFrameIndex;
  private float mPixelIntensity;

  public ARCoreCamera(Activity activity) {
    mActivity = activity;
    mCache = new HashMap<>();
    mListeners = new ArrayList<>();

    mCameraCalibration = new CameraCalibration();
    mLock = new Object();
    mFrameIndex = 1;
    mPixelIntensity = 0;
  }

  public void addListener(Object listener) {
    mListeners.add((Camera2DataListener) listener);
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

    //setup ARCore cycle
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

    synchronized (mCache) {
      mCache.put(image.getTimestamp(), bitmap);
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
    if (DEPTH_VISUALISATION_ENABLED) {
      mDepthCameraPreview.setImageBitmap(getDepthPreview(image));
    }

    Pose pose;
    synchronized (mLock) {
      pose = mPose;
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
      for (Camera2DataListener listener : mListeners) {
        listener.onDepthDataReceived(image, pose, mFrameIndex);
      }
      for (Camera2DataListener listener : mListeners) {
        listener.onColorDataReceived(bitmap, mFrameIndex);
      }

      synchronized (mCache) {
        mCache.clear();
        mFrameIndex++;
      }
    }
    image.close();
  }

  private Bitmap getDepthPreview(Image image) {
    Image.Plane plane = image.getPlanes()[0];
    ByteBuffer buffer = plane.getBuffer();
    ShortBuffer shortDepthBuffer = buffer.asShortBuffer();

    ArrayList<Short> pixel = new ArrayList<>();
    while (shortDepthBuffer.hasRemaining()) {
      pixel.add(shortDepthBuffer.get());
    }
    int stride = plane.getRowStride();
    int width = image.getWidth();
    int height = image.getHeight();

    float[][] depth = new float[width][height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int depthSample = pixel.get((y / 2) * stride + x);
        int depthRange = depthSample & 0x1FFF;
        if ((x < 1) || (y < 1) || (x >= width - 1) || (y >= height - 1)) {
          depthRange = 0;
        }
        depth[x][y] = depthRange;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    for (int y = 1; y < height - 1; y++) {
      for (int x = 1; x < width - 1; x++) {

        float mx = depth[x][y] - depth[x - 1][y];
        float px = depth[x][y] - depth[x + 1][y];
        float my = depth[x][y] - depth[x][y - 1];
        float py = depth[x][y] - depth[x][y + 1];
        float value = Math.abs(mx) + Math.abs(px) + Math.abs(my) + Math.abs(py);
        int r = (int) Math.max(0, Math.min(1.0f * value, 255));
        int g = (int) Math.max(0, Math.min(2.0f * value, 255));
        int b = (int) Math.max(0, Math.min(3.0f * value, 255));
        bitmap.setPixel(x, y, Color.argb(128, r, g, b));
      }
    }
    return bitmap;
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

      // Enable auto focus mode while ARCore is running.
      Config config = mSession.getConfig();
      config.setFocusMode(Config.FocusMode.AUTO);
      config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
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
          if (cameraConfig.getDepthSensorUsage() == CameraConfig.DepthSensorUsage.REQUIRE_AND_USE) {
            rank += 1;
          }
          if ((w > 1024) && (h > 1024)) {
            rank += 2;
          }
          if (Math.abs(mDepthWidth / (float)mDepthHeight - w / (float)h) < 0.0001f) {
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
                  mCameraCalibration.depthCameraTranslation = characteristics.get(CameraCharacteristics.LENS_POSE_TRANSLATION);
                  mCameraCalibration.depthCameraIntrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                  if (mCameraCalibration.depthCameraIntrinsic != null) {
                    mCameraCalibration.depthCameraIntrinsic[0] /= (float)mDepthWidth;
                    mCameraCalibration.depthCameraIntrinsic[1] /= (float)mDepthHeight;
                    mCameraCalibration.depthCameraIntrinsic[2] /= (float)mDepthWidth;
                    mCameraCalibration.depthCameraIntrinsic[3] /= (float)mDepthHeight;
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

  public CameraCalibration getCalibration() {
    return mCameraCalibration;
  }

  public float getLightIntensity() {
    return mPixelIntensity;
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
      mCameraCalibration.colorCameraIntrinsic[0] = intrinsics.getFocalLength()[0] / (float)intrinsics.getImageDimensions()[0];
      mCameraCalibration.colorCameraIntrinsic[1] = intrinsics.getFocalLength()[1] / (float)intrinsics.getImageDimensions()[1];
      mCameraCalibration.colorCameraIntrinsic[2] = intrinsics.getPrincipalPoint()[0] / (float)intrinsics.getImageDimensions()[0];
      mCameraCalibration.colorCameraIntrinsic[3] = intrinsics.getPrincipalPoint()[1] / (float)intrinsics.getImageDimensions()[1];
      if (mColorCameraId.compareTo(mDepthCameraId) == 0) {
        mCameraCalibration.depthCameraIntrinsic = mCameraCalibration.colorCameraIntrinsic;
      }
      mCameraCalibration.setValid();

      //get pose from ARCore
      synchronized (mLock) {
        Camera camera = frame.getCamera();
        mPose = camera.getPose();
      }

      //get light estimation from ARCore
      mPixelIntensity = frame.getLightEstimate().getPixelIntensity();

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
