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
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;

public class ARCoreCamera implements ICamera {

  public interface Camera2DataListener
  {
    void onColorDataReceived(Bitmap bitmap, int frameIndex);

    void onDepthDataReceived(Image image, int frameIndex);
  }

  /**
   * (-1, 1) ------- (1, 1)
   *   |    \           |
   *   |       \        |
   *   |          \     |
   *   |             \  |
   * (-1, -1) ------ (1, -1)
   * Ensure triangles are front-facing, to support glCullFace().
   * This quad will be drawn using GL_TRIANGLE_STRIP which draws two
   * triangles: v0->v1->v2, then v2->v1->v3.
   */
  private static final float[] QUAD_COORDS = new float[] {
    -1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f,
  };
  private static final int FLOAT_SIZE = 4;

  private static final String TAG = ARCoreCamera.class.getSimpleName();

  //Camera2 API
  private ImageReader mImageReaderDepth16;
  private ImageReader mImageReaderRGB;
  private HandlerThread backgroundThread;
  private Handler backgroundHandler;

  //ARCore API
  private Session sharedSession;

  //App integration objects
  private Activity mActivity;
  private ImageView mColorCameraPreview;
  private GLSurfaceView mGLSurfaceView;
  private ArrayList<Camera2DataListener> mListeners;
  private HashMap<Long, Bitmap> mCache;
  private String mCameraCalibration;
  private int mFrameIndex;
  private float mPixelIntensity;

  public ARCoreCamera(Activity activity) {
    mActivity = activity;
    mCache = new HashMap<>();
    mListeners = new ArrayList<>();
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
    mColorCameraPreview = mActivity.findViewById(R.id.colorCameraPreview);
    mColorCameraPreview.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));

    //set color camera
    mImageReaderRGB = ImageReader.newInstance(640,480, ImageFormat.YUV_420_888, 5);
    mImageReaderRGB.setOnImageAvailableListener(imageReader -> onProcessColorData(imageReader.acquireLatestImage()), backgroundHandler);

    //set depth camera
    mImageReaderDepth16 = ImageReader.newInstance(240, 180, ImageFormat.DEPTH16, 5);
    mImageReaderDepth16.setOnImageAvailableListener(imageReader -> onProcessDepthData(imageReader.acquireLatestImage()), backgroundHandler);


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
            startBackgroundThread();
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
    stopBackgroundThread();
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

    mCache.put(image.getTimestamp(), bitmap);
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
    });
    image.close();
  }

  private void onProcessDepthData(Image image) {
    if (image == null) {
      Log.w(TAG, "onImageAvailable: Skipping null image.");
      return;
    }
    if (!mCache.isEmpty()) {
      Bitmap bitmap = null;
      long bestDiff = Long.MAX_VALUE;
      for (Long timestamp : mCache.keySet()) {
        long diff = Math.abs(image.getTimestamp() - timestamp) / 1000; //in microseconds
        if (bestDiff > diff) {
          bestDiff = diff;
          bitmap = mCache.get(timestamp);
        }
      }

      if (bitmap != null && bestDiff < 50000) {
        for (Camera2DataListener listener : mListeners) {
          listener.onDepthDataReceived(image, mFrameIndex);
        }
        for (Camera2DataListener listener : mListeners) {
          listener.onColorDataReceived(bitmap, mFrameIndex);
        }

        mCache.clear();
        mFrameIndex++;
      }
    }
    image.close();
  }

  private void closeCamera() {
    if (null != mImageReaderDepth16) {
      mImageReaderDepth16.setOnImageAvailableListener(null, null);
      mImageReaderDepth16.close();
      mImageReaderDepth16 = null;
    }
    if (null != mImageReaderRGB) {
      mImageReaderRGB.setOnImageAvailableListener(null, null);
      mImageReaderRGB.close();
      mImageReaderRGB = null;
    }
    if (sharedSession != null) {
      sharedSession.pause();
      sharedSession.close();
      sharedSession = null;
    }
  }

  private void openCamera() {

    //check permissions
    if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    if (sharedSession == null) {
      try {
        // Create ARCore session that supports camera sharing.
        sharedSession = new Session(mActivity, EnumSet.of(Session.Feature.SHARED_CAMERA));
      } catch (Exception e) {
        Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
        return;
      }

      // Enable auto focus mode while ARCore is running.
      Config config = sharedSession.getConfig();
      config.setFocusMode(Config.FocusMode.AUTO);
      config.setLightEstimationMode(Config.LightEstimationMode.AMBIENT_INTENSITY);
      sharedSession.configure(config);
    }

    // Store the ARCore shared camera reference.
    SharedCamera sharedCamera = sharedSession.getSharedCamera();

    // Store the ID of the camera used by ARCore.
    String cameraId = sharedSession.getCameraConfig().getCameraId();

    // When ARCore is running, make sure it also updates our CPU image surface.
    ArrayList<Surface> surfaces = new ArrayList<>();
    surfaces.add(mImageReaderDepth16.getSurface());
    surfaces.add(mImageReaderRGB.getSurface());
    sharedCamera.setAppSurfaces(cameraId, surfaces);

    try {
      sharedSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to start ARCore", e);
    }
  }

  private void startBackgroundThread() {
    backgroundThread = new HandlerThread("sharedCameraBackground");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
  }

  // Stop background handler thread.
  private void stopBackgroundThread() {
    if (backgroundThread != null) {
      backgroundThread.quitSafely();
      try {
        backgroundThread.join();
        backgroundThread = null;
        backgroundHandler = null;
      } catch (InterruptedException e) {
        Log.e(TAG, "Interrupted while trying to join background handler thread", e);
      }
    }
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
          mActivity.runOnUiThread(() -> Toast.makeText(mActivity, "ARCore not installed\n" + e, Toast.LENGTH_LONG).show());
          mActivity.finish();
          return false;
        }
        break;
      case UNKNOWN_ERROR:
      case UNKNOWN_CHECKING:
      case UNKNOWN_TIMED_OUT:
      case UNSUPPORTED_DEVICE_NOT_CAPABLE:
        Log.e(
                TAG,
                "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned "
                        + availability);
        mActivity.runOnUiThread(() ->
                        Toast.makeText(mActivity, "ARCore is not supported on this device, "
                                        + "ArCoreApk.checkAvailability() returned "
                                        + availability, Toast.LENGTH_LONG).show());
        return false;
    }
    return true;
  }

  public String getCalibration() {
    return mCameraCalibration;
  }

  public float getLightIntensity() {
    return mPixelIntensity;
  }

  private void updateFrame(int texture, int width, int height) {
    try {
      if (sharedSession == null) {
        return;
      }

      //init buffers
      float[] projection = new float[16];
      ByteBuffer bbCoords = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
      bbCoords.order(ByteOrder.nativeOrder());
      FloatBuffer quadCoords = bbCoords.asFloatBuffer();
      quadCoords.put(QUAD_COORDS);
      quadCoords.position(0);
      ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
      bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
      FloatBuffer quadTexCoords = bbTexCoordsTransformed.asFloatBuffer();

      //get calibration from ARCore
      float nearClip = 0.1f;
      float farClip = 100.0f;
      sharedSession.setCameraTextureName(texture);
      sharedSession.setDisplayGeometry(0, width, height);
      Frame frame = sharedSession.update();
      frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadCoords, Coordinates2d.TEXTURE_NORMALIZED, quadTexCoords);
      Camera camera = frame.getCamera();
      camera.getProjectionMatrix(projection, 0, nearClip, farClip);
      Matrix.invertM(projection, 0, projection, 0);

      //get light estimation from ARCore
      mPixelIntensity = frame.getLightEstimate().getPixelIntensity();

      //extract calibration into string
      quadTexCoords.position(0);
      quadCoords.position(0);
      mCameraCalibration = "";
      mCameraCalibration += "Inverse of projection matrix:\n";
      for (int i = 0 ; i < projection.length; i++) {
        mCameraCalibration += projection[i] + (i % 4 == 3 ? "\n" : " ");
      }
      mCameraCalibration += "Camera clip:\n";
      mCameraCalibration += nearClip + " " + farClip + "\n";
      mCameraCalibration += "Frame clip:\n";
      for (int i = 0 ; i < QUAD_COORDS.length / 2; i++) {
        mCameraCalibration += quadCoords.get() + " " + quadCoords.get() + " -> " + quadTexCoords.get() + " " + quadTexCoords.get() + "\n";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
