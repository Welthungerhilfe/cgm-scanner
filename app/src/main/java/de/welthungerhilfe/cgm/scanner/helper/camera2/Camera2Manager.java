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

package de.welthungerhilfe.cgm.scanner.helper.camera2;

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

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Camera2Manager {

  public interface CameraDataListener
  {
    void OnColorDataReceived(Bitmap bitmap, long timestamp);

    void OnDepthDataReceived(Image image);
  }

  private static final String TAG = Camera2Manager.class.getSimpleName();

  //Camera2 API
  private ImageReader mImageReaderDepth16;
  private ImageReader mImageReaderRGB;
  private CameraDevice mCameraDevice;

  private String depthCameraId = "0";
  private int depthWidth = 240;
  private int depthHeight = 180;

  private ArrayList<CameraDataListener> listeners = new ArrayList<>();

  public void addListener(CameraDataListener listener) {
    listeners.add(listener);
  }

  public void removeListener(CameraDataListener listener) {
    listeners.remove(listener);
  }

  /**
   * Closes the current {@link CameraDevice}.
   */
  public void closeCamera() {
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

  /**
   * Opens the camera
   */
  public void openCamera(Activity context, int PERMISSION_CAMERA) {

    //check permissions
    if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      context.requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
      return;
    }

    //set depth camera
    initDepthCamera(context);
    mImageReaderDepth16 = ImageReader.newInstance(depthWidth, depthHeight, ImageFormat.DEPTH16, 5);
    mImageReaderDepth16.setOnImageAvailableListener(imageReader -> {
      Image image = imageReader.acquireLatestImage();
      if (image == null) {
        Log.w(TAG, "onImageAvailable: Skipping null image.");
        return;
      }
      for (CameraDataListener listener : listeners) {
        listener.OnDepthDataReceived(image);
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
      final RenderScript rs = RenderScript.create(context);

      final Bitmap bitmap     = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
      final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);

      final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
      allocationYuv.copyFrom(yuvBytes.array());

      ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
      scriptYuvToRgb.setInput(allocationYuv);
      scriptYuvToRgb.forEach(allocationRgb);

      allocationRgb.copyTo(bitmap);
      for (CameraDataListener listener : listeners) {
        listener.OnColorDataReceived(bitmap, image.getTimestamp());
      }
      allocationYuv.destroy();
      allocationRgb.destroy();
      rs.destroy();

      image.close();
    }, null);

    //start
    CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    try {
      manager.openCamera(depthCameraId, mCallBack, null);
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
    depthWidth = 240;
    depthHeight = 180;

    // Note 10, Note 10 5G
    if (Build.DEVICE.toUpperCase().startsWith("D1")) { depthWidth = 640; depthHeight = 480; }

    // Note 10+, Note 10+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("D2")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SC-01M")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCV45")) { depthWidth = 640; depthHeight = 480; }

    // S20, S20 5G
    if (Build.DEVICE.toUpperCase().startsWith("SC-51A")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCG01")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("X1")) { depthWidth = 640; depthHeight = 480; }

    // S20+, S20+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("Y2")) { depthWidth = 640; depthHeight = 480; }

    // S20 Ultra
    if (Build.DEVICE.toUpperCase().startsWith("Z3")) { depthWidth = 640; depthHeight = 480; }

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
                  depthCameraId = cameraId;
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
