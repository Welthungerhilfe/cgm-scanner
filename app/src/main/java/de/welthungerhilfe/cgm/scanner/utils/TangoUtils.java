package de.welthungerhilfe.cgm.scanner.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
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
public class TangoUtils {

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

    public static ARCoreUtils.Depthmap extractDepthmap(ByteBuffer buffer, int numPoints, double[] pose, double timestamp, TangoCameraIntrinsics calibration) {
        int width = 180;
        int height = 135;

        ARCoreUtils.Depthmap depthmap = new ARCoreUtils.Depthmap(width, height);
        depthmap.position = new float[] {(float) pose[4], (float) pose[5], (float) pose[6]};
        depthmap.rotation = new float[] {(float) pose[0], (float) pose[1], (float) pose[2], (float) pose[3]};
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

    public static void writeImageToFile(TangoImageBuffer currentTangoImageBuffer, File file) {

        currentTangoImageBuffer = TangoUtils.copyImageBuffer(currentTangoImageBuffer);
        int currentImgWidth = currentTangoImageBuffer.width;
        int currentImgHeight = currentTangoImageBuffer.height;
        byte[] YuvImageByteArray = currentTangoImageBuffer.data.array();

        try (FileOutputStream out = new FileOutputStream(file)) {
            YuvImage yuvImage = new YuvImage(YuvImageByteArray, ImageFormat.NV21, currentImgWidth, currentImgHeight, null);
            yuvImage.compressToJpeg(new Rect(0, 0, currentImgWidth, currentImgHeight), BitmapUtils.JPG_COMPRESSION, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
