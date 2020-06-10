package de.welthungerhilfe.cgm.scanner.utils;

import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.welthungerhilfe.cgm.scanner.helper.camera.ARCoreCamera;

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
public class ARCoreUtils {

    public static class Depthmap {
        byte[] confidence;
        short[] depth;
        int count;
        int width;
        int height;
        long timestamp;

        private Depthmap(int width, int height) {
            this.width = width;
            this.height = height;

            count = 0;
            confidence = new byte[width * height];
            depth = new short[width * height];
        }

        private float getConfidence(int x, int y) {
            return confidence[y * width + x] / 7.0f;
        }

        public int getCount() {
            return count;
        }

        public byte[] getData() {
            int index = 0;
            byte[] output = new byte[width * height * 3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    short depthRange = depth[y * width + x];
                    output[index++] = (byte) (depthRange / 256);
                    output[index++] = (byte) (depthRange % 256);
                    output[index++] = confidence[y * width + x];
                }
            }
            return output;
        }

        private float getDepth(int x, int y) {
            return depth[y * width + x] * 0.001f;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public long getTimestamp() { return timestamp; }
    }

    public static Depthmap extractDepthmap(Image image) {
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

        Depthmap depthmap = new Depthmap(width, height);
        depthmap.timestamp = image.getTimestamp();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int depthSample = pixel.get((y / 2) * stride + x);
                int depthRange = depthSample & 0x1FFF;
                int depthConfidence = ((depthSample >> 13) & 0x7);
                if ((x < 1) || (y < 1) || (x >= width - 1) || (y >= height - 1)) {
                    depthConfidence = 0;
                    depthRange = 0;
                }
                if (depthRange > 0) {
                    depthmap.count++;
                }
                depthmap.confidence[y * width + x] = (byte) depthConfidence;
                depthmap.depth[y * width + x] = (short) depthRange;
            }
        }
        return depthmap;
    }

    public static void writeDepthmap(Depthmap depthmap, File file) {
        try {
            byte[] data = depthmap.getData();
            FileOutputStream stream = new FileOutputStream(file);
            ZipOutputStream zip = new ZipOutputStream(stream);
            byte[] info = (depthmap.getWidth() + "x" + depthmap.getHeight() + "_0.001_7\n").getBytes();
            zip.putNextEntry(new ZipEntry("data"));
            zip.write(info, 0, info.length);
            zip.write(data, 0, data.length);
            zip.flush();
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // This function writes the XYZC points to .pcd files in binary
    public static void writeDepthmapToPcdFile(Depthmap depthmap, ARCoreCamera.CameraCalibration calibration,
                                             long timestamp, File pointCloudSaveFolder, String pointCloudFilename) {

        File file = new File(pointCloudSaveFolder, pointCloudFilename);

        try {
            int count = 0;
            float fx = calibration.getIntrinsic(false)[0] * (float)depthmap.getWidth();
            float fy = calibration.getIntrinsic(false)[1] * (float)depthmap.getHeight();
            float cx = calibration.getIntrinsic(false)[2] * (float)depthmap.getWidth();
            float cy = calibration.getIntrinsic(false)[3] * (float)depthmap.getHeight();
            float stepx = Math.max(depthmap.getWidth() / 240.0f, 1.0f);
            float stepy = Math.max(depthmap.getHeight() / 180.0f, 1.0f);
            StringBuilder output = new StringBuilder();
            for (float y = 0; y < depthmap.getHeight(); y += stepy) {
                for (float x = 0; x < depthmap.getWidth(); x += stepx) {
                    float confidence = depthmap.getConfidence((int)x, (int)y);
                    float depth = depthmap.getDepth((int)x, (int)y);
                    if (depth > 0) {
                        float pcx =-(x - cx) * depth / fx;
                        float pcy = (y - cy) * depth / fy;
                        output.append(String.format(Locale.US, "%f %f %f %f\n", pcx, pcy, depth, confidence));
                        count++;
                    }
                }
            }

            String header = "# timestamp 1 1 float " + timestamp + "\n" +
                    "# .PCD v.7 - Point Cloud Data file format\n" +
                    "VERSION .7\n" +
                    "FIELDS x y z c\n" +
                    "SIZE 4 4 4 4\n" +
                    "TYPE F F F F\n" +
                    "COUNT 1 1 1 1\n" +
                    "WIDTH " + count + "\n" +
                    "HEIGHT 1\n" +
                    "VIEWPOINT 0 0 0 1 0 0 0\n" +
                    "POINTS " + count + "\n" +
                    "DATA ascii\n";

            FileOutputStream out = new FileOutputStream(file);
            out.write(header.getBytes());
            out.write(output.toString().getBytes());
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
