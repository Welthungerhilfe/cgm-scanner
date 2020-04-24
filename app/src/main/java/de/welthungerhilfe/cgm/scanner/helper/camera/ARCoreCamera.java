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
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.ConditionVariable;
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
  private final ConditionVariable safeToExitApp = new ConditionVariable();
  private CameraCaptureSession captureSession;

  //ARCore API
  private Session sharedSession;
  private CameraDevice cameraDevice;

  //App integration objects
  private Activity mActivity;
  private ImageView mColorCameraPreview;
  private ArrayList<Camera2DataListener> mListeners;
  private String mCameraCalibration;

  public ARCoreCamera(Activity activity) {
    mActivity = activity;
    mListeners = new ArrayList<>();
  }

  public void addListener(Object listener) {
    mListeners.add((Camera2DataListener) listener);
  }

  public void removeListener(Object listener) {
    mListeners.remove(listener);
  }

  @Override
  public void onStart() {
  }

  @Override
  public void onCreate() {
    mColorCameraPreview = mActivity.findViewById(R.id.colorCameraPreview);
    mColorCameraPreview.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));

    GLSurfaceView glSurfaceView = mActivity.findViewById(R.id.surfaceview);
    glSurfaceView.setRenderer(new GLSurfaceView.Renderer() {

      private int textures[] = new int[1];
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
        updateCalibration(textures[0], width, height);
      }
    });
  }

  @Override
  public void onResume() {
    startBackgroundThread();
    openCamera();
  }

  @Override
  public void onPause() {
    closeCamera();
    stopBackgroundThread();
  }

  private void closeCamera() {
    if (captureSession != null) {
      captureSession.close();
      captureSession = null;
    }
    if (cameraDevice != null) {
      safeToExitApp.close();
      cameraDevice.close();
      safeToExitApp.block();
    }
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

  private synchronized void openCamera() {

    //check permissions
    if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    //set depth camera
    final int[] frameIndex = {1};
    HashMap<Long, Bitmap> cache = new HashMap<>();
    mImageReaderDepth16 = ImageReader.newInstance(240, 180, ImageFormat.DEPTH16, 5);
    mImageReaderDepth16.setOnImageAvailableListener(imageReader -> {
      Image image = imageReader.acquireLatestImage();
      if (image == null) {
        Log.w(TAG, "onImageAvailable: Skipping null image.");
        return;
      }
      if (!cache.isEmpty()) {
        Bitmap bitmap = null;
        long bestDiff = Long.MAX_VALUE;
        for (Long timestamp : cache.keySet()) {
          long diff = Math.abs(image.getTimestamp() - timestamp) / 1000; //in microseconds
          if (bestDiff > diff) {
            bestDiff = diff;
            bitmap = cache.get(timestamp);
          }
        }

        if (bitmap != null && bestDiff < 50000) {
          for (Camera2DataListener listener : mListeners) {
            listener.onDepthDataReceived(image, frameIndex[0]);
          }
          for (Camera2DataListener listener : mListeners) {
            listener.onColorDataReceived(bitmap, frameIndex[0]);
          }

          cache.clear();
          frameIndex[0]++;
        }
      }
      image.close();
    }, backgroundHandler);

    //set color camera
    mImageReaderRGB = ImageReader.newInstance(640,480, ImageFormat.YUV_420_888, 5);
    mImageReaderRGB.setOnImageAvailableListener(imageReader -> {
      Image image = imageReader.acquireLatestImage();
      if (image == null) {
        Log.w(TAG, "onImageAvailable: Skipping null image.");
        return;
      }
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

      cache.put(image.getTimestamp(), bitmap);
      allocationYuv.destroy();
      allocationRgb.destroy();
      rs.destroy();

      //update preview window
      mActivity.runOnUiThread(() -> {
        float scale = bitmap.getWidth() / (float)bitmap.getHeight();
        scale *= mColorCameraPreview.getHeight() / (float)bitmap.getWidth();
        mColorCameraPreview.setImageBitmap(bitmap);
        mColorCameraPreview.setRotation(90);
        mColorCameraPreview.setScaleX(scale);
        mColorCameraPreview.setScaleY(scale);
      });
      image.close();
    }, backgroundHandler);

    // Don't open camera if already opened.
    if (cameraDevice != null) {
      return;
    }

    // Make sure that ARCore is installed, up to date, and supported on this device.
    if (!isARCoreSupportedAndUpToDate()) {
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

      // Wrap our callback in a shared camera callback.
      CameraDevice.StateCallback wrappedCallback = sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler);

      // Store a reference to the camera system service.
      // Reference to the camera system service.
      CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);

      // Open the camera device using the ARCore wrapped callback.
      cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
      sharedSession.resume();
    } catch (Exception e) {
      Log.e(TAG, "Failed to open camera", e);
    }
  }

  private ByteBuffer imageToByteBuffer(final Image image) {
    final Rect crop   = image.getCropRect();
    final int  width  = crop.width();
    final int  height = crop.height();

    final Image.Plane[] planes     = image.getPlanes();
    final byte[]        rowData    = new byte[planes[0].getRowStride()];
    final int           bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
    final ByteBuffer    output     = ByteBuffer.allocateDirect(bufferSize);

    int channelOffset = 0;
    int outputStride = 0;

    for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
      if (planeIndex == 0) {
        channelOffset = 0;
        outputStride = 1;
      } else if (planeIndex == 1) {
        channelOffset = width * height + 1;
        outputStride = 2;
      } else if (planeIndex == 2) {
        channelOffset = width * height;
        outputStride = 2;
      }

      final ByteBuffer buffer      = planes[planeIndex].getBuffer();
      final int        rowStride   = planes[planeIndex].getRowStride();
      final int        pixelStride = planes[planeIndex].getPixelStride();

      final int shift         = (planeIndex == 0) ? 0 : 1;
      final int widthShifted  = width >> shift;
      final int heightShifted = height >> shift;

      buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

      for (int row = 0; row < heightShifted; row++)
      {
        final int length;

        if (pixelStride == 1 && outputStride == 1)
        {
          length = widthShifted;
          buffer.get(output.array(), channelOffset, length);
          channelOffset += length;
        }
        else
        {
          length = (widthShifted - 1) * pixelStride + 1;
          buffer.get(rowData, 0, length);

          for (int col = 0; col < widthShifted; col++)
          {
            output.array()[channelOffset] = rowData[col * pixelStride];
            channelOffset += outputStride;
          }
        }

        if (row < heightShifted - 1)
        {
          buffer.position(buffer.position() + rowStride - length);
        }
      }
    }

    return output;
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

  public synchronized String getCalibration() {
    return mCameraCalibration;
  }

  private synchronized void updateCalibration(int texture, int width, int height) {
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
      sharedSession.setCameraTextureName(texture);
      sharedSession.setDisplayGeometry(0, width, height);
      Frame frame = sharedSession.update();
      frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadCoords, Coordinates2d.TEXTURE_NORMALIZED, quadTexCoords);
      Camera camera = frame.getCamera();
      camera.getProjectionMatrix(projection, 0, 0.1f, 100.0f);

      //extract calibration into string
      quadTexCoords.position(0);
      quadCoords.position(0);
      mCameraCalibration = "";
      mCameraCalibration += "Projection matrix:\n";
      for (int i = 0 ; i < projection.length; i++) {
        mCameraCalibration += projection[i] + (i % 4 == 3 ? "\n" : " ");
      }
      mCameraCalibration += "Frame clip:\n";
      for (int i = 0 ; i < QUAD_COORDS.length / 2; i++) {
        mCameraCalibration += quadCoords.get() + " " + quadCoords.get() + " -> " + quadTexCoords.get() + " " + quadTexCoords.get() + "\n";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private final CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice cameraDevice) {
      Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
      ARCoreCamera.this.cameraDevice = cameraDevice;
    }

    @Override
    public void onClosed(CameraDevice cameraDevice) {
      Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " closed.");
      ARCoreCamera.this.cameraDevice = null;
      safeToExitApp.open();
    }

    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
      Log.w(TAG, "Camera device ID " + cameraDevice.getId() + " disconnected.");
      cameraDevice.close();
      ARCoreCamera.this.cameraDevice = null;
    }

    @Override
    public void onError(CameraDevice cameraDevice, int error) {
      Log.e(TAG, "Camera device ID " + cameraDevice.getId() + " error " + error);
      cameraDevice.close();
      ARCoreCamera.this.cameraDevice = null;
      // Fatal error. Quit application.
      mActivity.finish();
    }
  };
}
