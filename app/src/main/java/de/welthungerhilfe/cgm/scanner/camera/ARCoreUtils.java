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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;

import com.google.ar.core.Pose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ARCoreUtils {

    public interface Camera2DataListener
    {
        void onColorDataReceived(Bitmap bitmap, int frameIndex);

        void onDepthDataReceived(Image image, Pose pose, int frameIndex);
    }

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

    public static Depthmap extractDepthmap(Image image, Pose pose, boolean reorder) {
        if (image == null) {
            Depthmap depthmap = new Depthmap(0, 0);
            depthmap.position = pose.getTranslation();
            depthmap.rotation = pose.getRotationQuaternion();
            depthmap.timestamp = 0;
            return depthmap;
        }
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        if (reorder) {
            buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
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

    public static Bitmap getDepthPreview(Image image, boolean reorder) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        if (reorder) {
            buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();

        ArrayList<Integer> pixel = new ArrayList<>();
        while (shortDepthBuffer.hasRemaining()) {
            pixel.add((int) shortDepthBuffer.get());
        }
        int stride = plane.getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();

        float[][] depth = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int depthSample = pixel.get((y / 2) * stride + x);
                int depthRange = depthSample & 0x1FFF;
                if ((x < 1) || (y < 1) || (x >= width - 1) || (y >= height - 1)) {
                    depthRange = 0;
                }
                depth[x][y] = depthRange;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                float mx = depth[x][y] - depth[x - 1][y];
                float px = depth[x][y] - depth[x + 1][y];
                float my = depth[x][y] - depth[x][y - 1];
                float py = depth[x][y] - depth[x][y + 1];
                float value = Math.abs(mx) + Math.abs(px) + Math.abs(my) + Math.abs(py);
                int r = (int) Math.max(0, Math.min(1.0f * value, 255));
                int g = (int) Math.max(0, Math.min(2.0f * value, 255));
                int b = (int) Math.max(0, Math.min(3.0f * value, 255));
                bitmap.setPixel(x, y, Color.argb(128, r, g, b));
            }
        }
        return bitmap;
    }

    public static boolean shouldUseAREngine() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return false;
        }
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("HONOR") || manufacturer.startsWith("HUAWEI");
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
