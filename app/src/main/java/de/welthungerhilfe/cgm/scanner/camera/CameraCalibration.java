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

public class CameraCalibration {

    public enum LightConditions { NORMAL, BRIGHT, DARK };

    public float[] colorCameraIntrinsic;
    public float[] depthCameraIntrinsic;
    public float[] depthCameraTranslation;
    private boolean valid;

    public CameraCalibration() {
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

    public void setValid() {
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

    public static LightConditions updateLight(LightConditions light, long lastBright, long lastDark, long sessionStart) {
        //prevent showing "too bright" directly after changing light conditions
        double diff = Math.abs(lastBright - System.currentTimeMillis());
        if (light == CameraCalibration.LightConditions.BRIGHT) {
            if (diff < 3000) {
                light = CameraCalibration.LightConditions.NORMAL;
            }
        }

        //prevent showing messages directly after session start
        if ((Math.abs(lastBright - sessionStart) < 2000) || (Math.abs(lastDark - sessionStart) < 2000)) {
            light = CameraCalibration.LightConditions.NORMAL;
        }

        //reset light state if there was longer no change
        diff = Math.min(diff, Math.abs(lastDark - System.currentTimeMillis()));
        if (diff > 1000) {
            light = CameraCalibration.LightConditions.NORMAL;
        }

        return light;
    }
}