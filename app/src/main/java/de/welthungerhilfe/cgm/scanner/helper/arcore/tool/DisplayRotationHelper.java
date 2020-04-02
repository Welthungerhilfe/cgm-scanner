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

package de.welthungerhilfe.cgm.scanner.helper.arcore.tool;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.ar.core.Session;

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus they require listening to the android display
 * events.
 */
public final class DisplayRotationHelper implements DisplayListener {
  private boolean viewportChanged;
  private int viewportWidth;
  private int viewportHeight;
  private final Display display;
  private final DisplayManager displayManager;
  private final CameraManager cameraManager;

  /**
   * Constructs the DisplayRotationHelper but does not register the listener yet.
   *
   * @param context the Android {@link Context}.
   */
  public DisplayRotationHelper(Context context) {
    displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    display = windowManager.getDefaultDisplay();
  }

  /** Registers the display listener. Should be called from {@link Activity#onResume()}. */
  public void onResume() {
    displayManager.registerDisplayListener(this, null);
  }

  /** Unregisters the display listener. Should be called from {@link Activity#onPause()}. */
  public void onPause() {
    displayManager.unregisterDisplayListener(this);
  }

  /**
   * Records a change in surface dimensions. This will be later used by {@link
   * #updateSessionIfNeeded(Session)}. Should be called from {@link
   * android.opengl.GLSurfaceView.Renderer
   * #onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)}.
   *
   * @param width the updated width of the surface.
   * @param height the updated height of the surface.
   */
  public void onSurfaceChanged(int width, int height) {
    viewportWidth = width;
    viewportHeight = height;
    viewportChanged = true;
  }

  /**
   * Updates the session display geometry if a change was posted either by {@link
   * #onSurfaceChanged(int, int)} call or by {@link #onDisplayChanged(int)} system callback. This
   * function should be called explicitly before each call to {@link Session#update()}. This
   * function will also clear the 'pending update' (viewportChanged) flag.
   *
   * @param session the {@link Session} object to update if display geometry changed.
   */
  public void updateSessionIfNeeded(Session session) {
    if (viewportChanged) {
      int displayRotation = display.getRotation();
      session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
      viewportChanged = false;
    }
  }

  /**
   *  Returns the aspect ratio of the GL surface viewport while accounting for the display rotation
   *  relative to the device camera sensor orientation.
   */
  public float getCameraSensorRelativeViewportAspectRatio(String cameraId) {
    float aspectRatio;
    int cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId);
    switch (cameraSensorToDisplayRotation) {
      case 90:
      case 270:
        aspectRatio = (float) viewportHeight / (float) viewportWidth;
        break;
      case 0:
      case 180:
        aspectRatio = (float) viewportWidth / (float) viewportHeight;
        break;
      default:
        throw new RuntimeException("Unhandled rotation: " + cameraSensorToDisplayRotation);
    }
    return aspectRatio;
  }

  /**
   * Returns the rotation of the back-facing camera with respect to the display. The value is one of
   * 0, 90, 180, 270.
   */
  public int getCameraSensorToDisplayRotation(String cameraId) {
    CameraCharacteristics characteristics;
    try {
      characteristics = cameraManager.getCameraCharacteristics(cameraId);
    } catch (CameraAccessException e) {
      throw new RuntimeException("Unable to determine display orientation", e);
    }

    // Camera sensor orientation.
    int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

    // Current display orientation.
    int displayOrientation = toDegrees(display.getRotation());

    // Make sure we return 0, 90, 180, or 270 degrees.
    return (sensorOrientation - displayOrientation + 360) % 360;
  }

  private int toDegrees(int rotation) {
    switch (rotation) {
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
      default:
        throw new RuntimeException("Unknown rotation " + rotation);
    }
  }

  @Override
  public void onDisplayAdded(int displayId) {}

  @Override
  public void onDisplayRemoved(int displayId) {}

  @Override
  public void onDisplayChanged(int displayId) {
    viewportChanged = true;
  }
}
