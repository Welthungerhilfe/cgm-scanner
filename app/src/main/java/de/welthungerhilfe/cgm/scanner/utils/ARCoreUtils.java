package de.welthungerhilfe.cgm.scanner.utils;

import android.media.Image;

import com.google.ar.core.Pose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        float[] position;
        float[] rotation;
        long timestamp;

        Depthmap(int width, int height) {
            this.width = width;
            this.height = height;

            count = 0;
            confidence = new byte[width * height];
            depth = new short[width * height];
            position = new float[] {0, 0, 0};
            rotation = new float[] {0, 0, 0, 1};
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

        public String getPose(String separator) {
            String output = "";
            output += rotation[0] + separator;
            output += rotation[1] + separator;
            output += rotation[2] + separator;
            output += rotation[3] + separator;
            output += position[0] + separator;
            output += position[1] + separator;
            output += position[2];
            return output;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }

        public long getTimestamp() { return timestamp; }
    }

    public static Depthmap extractDepthmap(Image image, Pose pose) {
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
        depthmap.position = pose.getTranslation();
        depthmap.rotation = pose.getRotationQuaternion();
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
            byte[] info = (depthmap.getWidth() + "x" + depthmap.getHeight() + "_0.001_7_" + depthmap.getPose("_") + "\n").getBytes();
            zip.putNextEntry(new ZipEntry("data"));
            zip.write(info, 0, info.length);
            zip.write(data, 0, data.length);
            zip.flush();
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
