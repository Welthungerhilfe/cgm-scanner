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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;

public class Camera2Camera implements ICamera {

  public interface Camera2DataListener
  {
    void onColorDataReceived(Bitmap bitmap, long timestamp, int frameIndex);

    void onDepthDataReceived(Image image, int frameIndex);
  }

  private static final String TAG = Camera2Camera.class.getSimpleName();

  //Camera2 API
  private ImageReader mImageReaderDepth16;
  private ImageReader mImageReaderRGB;
  private CameraDevice mCameraDevice;
  private int mColorCameraFrame;
  private int mDepthCameraFrame;
  private String mDepthCameraId = "0";
  private int mDepthWidth = 240;
  private int mDepthHeight = 180;

  //App integration objects
  private Activity mActivity;
  private ImageView mColorCameraPreview;
  private ArrayList<Camera2DataListener> mListeners;

  public Camera2Camera(Activity activity) {
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

    mActivity.findViewById(R.id.surfaceview).setVisibility(View.GONE);
  }

  @Override
  public void onResume() {
    openCamera(mActivity);
  }

  @Override
  public void onPause() {
    closeCamera();
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
    if (null != mCameraDevice)
    {
      mCameraDevice.close();
      mCameraDevice = null;
    }
  }

  private void openCamera(Activity activity) {

    //check permissions
    if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    //set depth camera
    initDepthCamera(activity);
    mImageReaderDepth16 = ImageReader.newInstance(mDepthWidth, mDepthHeight, ImageFormat.DEPTH16, 5);
    mImageReaderDepth16.setOnImageAvailableListener(imageReader -> {
      Image image = imageReader.acquireLatestImage();
      if (image == null) {
        Log.w(TAG, "onImageAvailable: Skipping null image.");
        return;
      }
      mDepthCameraFrame++;
      for (Camera2DataListener listener : mListeners) {
        listener.onDepthDataReceived(image, mDepthCameraFrame);
      }
      image.close();
    }, null);

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
      final RenderScript rs = RenderScript.create(activity);
      final Bitmap bitmap     = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
      final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);
      final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
      allocationYuv.copyFrom(yuvBytes.array());
      ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
      scriptYuvToRgb.setInput(allocationYuv);
      scriptYuvToRgb.forEach(allocationRgb);
      allocationRgb.copyTo(bitmap);

      mColorCameraFrame++;
      for (Camera2DataListener listener : mListeners) {
        listener.onColorDataReceived(bitmap, image.getTimestamp(), mColorCameraFrame);
      }
      allocationYuv.destroy();
      allocationRgb.destroy();
      rs.destroy();

      //update preview window
      float scale = bitmap.getWidth() / (float)bitmap.getHeight();
      scale *= mColorCameraPreview.getHeight() / (float)bitmap.getWidth();
      mColorCameraPreview.setImageBitmap(bitmap);
      mColorCameraPreview.setRotation(90);
      mColorCameraPreview.setScaleX(scale);
      mColorCameraPreview.setScaleY(scale);

      image.close();
    }, null);

    //start
    CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
    try {
      manager.openCamera(mDepthCameraId, mCallBack, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (Exception e) {
      throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
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

  private CameraDevice.StateCallback mCallBack = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice cameraDevice) {
      mCameraDevice = cameraDevice;

      try {
        ArrayList<Surface> surfaces = new ArrayList<>();
        surfaces.add(mImageReaderDepth16.getSurface());
        surfaces.add(mImageReaderRGB.getSurface());
        final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        for (Surface s : surfaces) {
          requestBuilder.addTarget(s);
        }

        cameraDevice.createCaptureSession(surfaces,new CameraCaptureSession.StateCallback() {

          @Override
          public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {};

            try {
              HandlerThread handlerThread = new HandlerThread("CameraBackgroundThread");
              handlerThread.start();
              Handler handler = new Handler(handlerThread.getLooper());
              cameraCaptureSession.setRepeatingRequest(requestBuilder.build(),captureCallback,handler);

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

  private void initDepthCamera(Context context) {
    mDepthWidth = 240;
    mDepthHeight = 180;

    // Note 10, Note 10 5G
    if (Build.DEVICE.toUpperCase().startsWith("D1")) { mDepthWidth = 640; mDepthHeight = 480; }

    // Note 10+, Note 10+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("D2")) { mDepthWidth = 640; mDepthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SC-01M")) { mDepthWidth = 640; mDepthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCV45")) { mDepthWidth = 640; mDepthHeight = 480; }

    // S20, S20 5G
    if (Build.DEVICE.toUpperCase().startsWith("SC-51A")) { mDepthWidth = 640; mDepthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCG01")) { mDepthWidth = 640; mDepthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("X1")) { mDepthWidth = 640; mDepthHeight = 480; }

    // S20+, S20+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("Y2")) { mDepthWidth = 640; mDepthHeight = 480; }

    // S20 Ultra
    if (Build.DEVICE.toUpperCase().startsWith("Z3")) { mDepthWidth = 640; mDepthHeight = 480; }

    try {
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
