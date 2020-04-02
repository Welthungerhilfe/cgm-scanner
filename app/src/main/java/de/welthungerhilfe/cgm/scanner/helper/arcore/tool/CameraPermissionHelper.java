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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

/** Helper to ask camera permission. */
public final class CameraPermissionHelper {
  private static final int CAMERA_PERMISSION_CODE = 0;
  private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

  /** Check to see we have the necessary permissions for this app. */
  public static boolean hasCameraPermission(Activity activity) {
    return activity.checkSelfPermission(CAMERA_PERMISSION)
        == PackageManager.PERMISSION_GRANTED;
  }

  /** Check to see we have the necessary permissions for this app, and ask for them if we don't. */
  public static void requestCameraPermission(Activity activity) {
    activity.requestPermissions(new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
  }

  /** Check to see if we need to show the rationale for this permission. */
  public static boolean shouldShowRequestPermissionRationale(Activity activity) {
    return activity.shouldShowRequestPermissionRationale(CAMERA_PERMISSION);
  }

  /** Launch Application Setting to grant permission. */
  public static void launchPermissionSettings(Activity activity) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
    activity.startActivity(intent);
  }
}
