/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.camera;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TangoUtils {

    private static final int JPG_COMPRESSION = 90;

    public static float getPixelIntensity(TangoImageBuffer imageBuffer) {
        int max = 0;
        int summary = 0;
        byte[] array = TangoUtils.copyImageBuffer(imageBuffer).data.array();
        int width = imageBuffer.width;
        int height = imageBuffer.height;
        for (int x = 0; x < width; x += width / 20) {
            for (int y = 0; y < height; y += height / 20) {
                int UVIndex = width * height + 2 * (x / 2) + (y / 2) * width;
                int Y = array[y * width + x] & 0xff;
                float U = (float)(array[UVIndex] & 0xff) - 128.0f;
                float V = (float)(array[UVIndex + 1] & 0xff) - 128.0f;

                //do the YUV -> RGB conversion
                float Yf = 1.164f*((float)Y) - 16.0f;
                int R = (int)(Yf + 1.596f*V);
                int G = (int)(Yf - 0.813f*V - 0.391f*U);
                int B = (int)(Yf            + 2.018f*U);

                //clip rgb values to 0-255
                R = Math.min(Math.max(R, 0), 255);
                G = Math.min(Math.max(G, 0), 255);
                B = Math.min(Math.max(B, 0), 255);

                //update pixel intensity
                max += 3 * 255;
                summary += R + G + B;
            }
        }
        return summary / (float)max * 1.5f;
    }

    public static TangoImageBuffer copyImageBuffer(TangoImageBuffer imageBuffer) {
        ByteBuffer clone = ByteBuffer.allocateDirect(imageBuffer.data.capacity());
        imageBuffer.data.rewind();
        clone.put(imageBuffer.data);
        imageBuffer.data.rewind();
        clone.flip();
        return new TangoImageBuffer(imageBuffer.width, imageBuffer.height,
                imageBuffer.stride, imageBuffer.frameNumber,
                imageBuffer.timestamp, imageBuffer.format, clone);
    }

    public static Depthmap extractDepthmap(ByteBuffer buffer, int numPoints, float[] position, float[] rotation, double timestamp, TangoCameraIntrinsics calibration) {
        int width = 180;
        int height = 135;

        Depthmap depthmap = new Depthmap(width, height);
        depthmap.position = position;
        depthmap.rotation = rotation;
        depthmap.timestamp = (long) timestamp;
        for (int i = 0; i < numPoints; i++) {
            float px = buffer.getFloat();
            float py = buffer.getFloat();
            float pz = buffer.getFloat();
            float pc = buffer.getFloat();
            double tx = px * calibration.fx / pz + calibration.cx;
            double ty = py * calibration.fy / pz + calibration.cy;
            int x = width - (int)(width * tx / (double)calibration.width) - 1;
            int y = height - (int)(height * ty / (double)calibration.height) - 1;
            if (x >= 0 && y >= 0 && x < width && y < height) {
                depthmap.confidence[y * width + x] = (byte) (7 * pc);
                depthmap.depth[y * width + x] = (short) (pz * 1000);
            }
        }

        return depthmap;
    }

    public static boolean isTangoSupported() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            String device = Build.DEVICE.toUpperCase();

            //Asus Zenfone AR
            if (device.compareTo("ASUS_A002") == 0) return true;
            if (device.compareTo("ASUS_A002_1") == 0) return true;
            //Lenovo Phab 2 Pro
            if (device.compareTo("PB2PRO") == 0) return true;
        }

        return false;
    }

    public static void writeImageToFile(TangoImageBuffer currentTangoImageBuffer, File file) {

        currentTangoImageBuffer = TangoUtils.copyImageBuffer(currentTangoImageBuffer);
        int currentImgWidth = currentTangoImageBuffer.width;
        int currentImgHeight = currentTangoImageBuffer.height;
        byte[] YuvImageByteArray = currentTangoImageBuffer.data.array();

        try (FileOutputStream out = new FileOutputStream(file)) {
            YuvImage yuvImage = new YuvImage(YuvImageByteArray, ImageFormat.NV21, currentImgWidth, currentImgHeight, null);
            yuvImage.compressToJpeg(new Rect(0, 0, currentImgWidth, currentImgHeight), JPG_COMPRESSION, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
