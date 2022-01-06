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
package de.welthungerhilfe.cgm.scanner.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.Stack;

public class ComputerVisionUtils {

    public static class AABB {
        int maxX;
        int maxY;
        int minX;
        int minY;

        public AABB() {
            maxX = Integer.MIN_VALUE;
            maxY = Integer.MIN_VALUE;
            minX = Integer.MAX_VALUE;
            minY = Integer.MAX_VALUE;
        }

        public void addPoint(int x, int y) {
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
        }

        public boolean isInside(AABB outside) {
            if (maxX > outside.maxX) return false;
            if (maxY > outside.maxY) return false;
            if (minX < outside.minX) return false;
            if (minY < outside.minY) return false;
            return true;
        }

        @Override
        public String toString() {
            return minX + ":" + minY + "x" + maxX + ":" + maxY;
        }
    }

    public static class Point3F {
        float x;
        float y;
        float z;

        public Point3F(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float distanceTo(Point3F p) {
            float dx = x - p.x;
            float dy = y - p.y;
            float dz = z - p.z;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }
    }

    private final static int ALPHA_DEFAULT = 128;
    private final static int ALPHA_FOCUS = 64;

    public ArrayList<Point3F> getCalibrationToFEdges(float[][] depth, float[] calibration, float[] matrix, Point3F[] cvEdges) {
        int w = depth.length;
        int h = depth[0].length;
        float fx = calibration[0] * (float)w;
        float fy = calibration[1] * (float)h;
        float cx = calibration[2] * (float)w;
        float cy = calibration[3] * (float)h;

        ArrayList<Point3F> edges = new ArrayList<>();
        if (cvEdges != null) {
            for (Point3F p : cvEdges) {
                float[] point = matrixTransformPoint(matrix, p.x, p.y, p.z);
                int tx = (int) (-point[0] * fx / point[2] + cx);
                int ty = (int) (point[1] * fy / point[2] + cy);
                if ((tx >= 0) && (ty >= 0) && (tx < w) && (ty < h)) {
                    float z = depth[tx][ty];
                    if (z > 0) {
                        float vx = (tx - cx) * z / fx;
                        float vy = (ty - cy) * z / fy;
                        edges.add(new Point3F(vx, vy, z));
                    }
                }
            }
        }
        return edges;
    }

    public float getCenterFocusHeight(Bitmap mask, float[][] depth, float plane, float[] calibration, float[] matrix) {
        float maxHeight = 0;

        if (Math.abs(plane - Integer.MAX_VALUE) >= 0.1f) {
            int w = depth.length;
            int h = depth[0].length;
            int[] pixels = new int[w * h];
            mask.getPixels(pixels, 0, w, 0, 0, w, h);
            float fx = calibration[0] * (float)w;
            float fy = calibration[1] * (float)h;
            float cx = calibration[2] * (float)w;
            float cy = calibration[3] * (float)h;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (Color.alpha(pixels[y * w + x]) == ALPHA_FOCUS) {
                        float z = depth[x][y];
                        if (z > 0) {
                            float tx = (x - cx) * z / fx;
                            float ty = (y - cy) * z / fy;
                            float[] point = matrixTransformPoint(matrix, -tx, ty, z);
                            float height = Math.abs(point[1] + plane);
                            if (maxHeight < height) {
                                maxHeight = height;
                            }
                        }
                    }
                }
            }
        }
        return maxHeight;
    }

    public Bitmap getDepthPreviewCalibration(float[][] depth, float[] calibration, float[] matrix, Point3F[] cvEdges) {
        int w = depth.length;
        int h = depth[0].length;
        float fx = calibration[0] * (float)w;
        float fy = calibration[1] * (float)h;
        float cx = calibration[2] * (float)w;
        float cy = calibration[3] * (float)h;
        Paint paint = new Paint();
        paint.setColor(Color.argb(255, 255, 0, 255));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int lx = 0, ly = 0;
        boolean first = true;
        if (cvEdges != null) {
            for (int i = 0; i < 5; i++) {
                Point3F p = cvEdges[i % 4];
                float[] point = matrixTransformPoint(matrix, p.x, p.y, p.z);
                int tx = (int) (-point[0] * fx / point[2] + cx);
                int ty = (int) (point[1] * fy / point[2] + cy);
                if (!first) {
                    canvas.drawLine(tx, ty, lx, ly, paint);
                } else {
                    first = false;
                }
                lx = tx;
                ly = ty;
            }
        }
        return bitmap;
    }

    public Bitmap getDepthPreviewCenter(float[][] depth, float plane, boolean otherColors) {
        Bitmap bitmap = getDepthPreviewSobel(depth);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        if (Math.abs(plane - Integer.MAX_VALUE) >= 0.1f) {
            ArrayList<Point> dirs = new ArrayList<>();
            dirs.add(new Point(-1, 0));
            dirs.add(new Point(1, 0));
            dirs.add(new Point(0, -1));
            dirs.add(new Point(0, 1));
            Stack<Point> stack = new Stack<>();
            Point p = new Point(w / 2, h / 2);
            stack.push(p);
            while (!stack.isEmpty()) {
                p = stack.pop();

                int index = p.y * w + p.x;
                int color = pixels[index];
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int a = Color.alpha(color);
                if ((r < 64) && (a != ALPHA_FOCUS)) {
                    pixels[index] = Color.argb(ALPHA_FOCUS, r, b, g);

                    for (Point d : dirs) {
                        Point t = new Point(p.x + d.x, p.y + d.y);
                        if ((t.x < 0) || (t.y < 0) || (t.x >= w) || (t.y >= h)) {
                            continue;
                        }
                        stack.add(t);
                    }
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                int color = pixels[index];
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int a = Color.alpha(color);
                if ((b > g) || (r > g) || (a != ALPHA_FOCUS)) {
                    if (!otherColors) {
                        pixels[index] = Color.TRANSPARENT;
                    }
                } else {
                    pixels[index] = Color.argb(ALPHA_FOCUS, 255, 255, 255);
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    public Bitmap getDepthPreviewPlane(float[][] depth, float plane, float[] calibration, float[] matrix) {
        Bitmap bitmap = getDepthPreviewSobel(depth);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        float fx = calibration[0] * (float)w;
        float fy = calibration[1] * (float)h;
        float cx = calibration[2] * (float)w;
        float cy = calibration[3] * (float)h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float z = depth[x][y];
                if (z > 0) {
                    float tx = (x - cx) * z / fx;
                    float ty = (y - cy) * z / fy;
                    float[] point = matrixTransformPoint(matrix, -tx, ty, z);
                    if (-point[1] - plane < 0.1f) {
                        int index = y * w + x;
                        int color = pixels[index];
                        int r = Color.red(color);
                        int g = Color.green(color);
                        int b = Color.blue(color);
                        pixels[index] = Color.argb(ALPHA_DEFAULT, b, g, r);
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    public Bitmap getDepthPreviewSobel(float[][] depth) {

        int width = depth.length;
        int height = depth[0].length;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                float mx = depth[x][y] - depth[x - 1][y];
                float px = depth[x][y] - depth[x + 1][y];
                float my = depth[x][y] - depth[x][y - 1];
                float py = depth[x][y] - depth[x][y + 1];
                float value = (Math.abs(mx) + Math.abs(px) + Math.abs(my) + Math.abs(py));
                int r = (int) Math.max(0, Math.min(1000.0f * value, 255));
                int g = (int) Math.max(0, Math.min(2000.0f * value, 255));
                int b = (int) Math.max(0, Math.min(3000.0f * value, 255));
                bitmap.setPixel(x, y, Color.argb(ALPHA_DEFAULT, r, g, b));
            }
        }
        return bitmap;
    }

    public float getPlaneLowest(ArrayList<Float> planes, float[] position) {
        float bestValue = Integer.MAX_VALUE;
        for (Float f : planes) {
            float value = f - 2.0f * position[1];
            if (bestValue > value) {
                bestValue = value;
            }
        }
        return bestValue;
    }

    public float getPlaneVisible(float[][] depth, ArrayList<Float> planes, float[] calibration, float[] matrix, float[] position) {
        int w = depth.length;
        int h = depth[0].length;
        float fx = calibration[0] * (float)w;
        float fy = calibration[1] * (float)h;
        float cx = calibration[2] * (float)w;
        float cy = calibration[3] * (float)h;
        int bestCount = 0;
        float bestValue = Integer.MAX_VALUE;
        for (Float f : planes) {
            int count = 0;
            float value = f - 2.0f * position[1];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float z = depth[x][y];
                    if (z > 0) {
                        float tx = (x - cx) * z / fx;
                        float ty = (y - cy) * z / fy;
                        float[] point = matrixTransformPoint(matrix, -tx, ty, z);
                        if (Math.abs(point[1] + value) < 0.1f) {
                            count++;
                        }
                    }
                }
            }
            if (bestCount < count) {
                bestCount = count;
                bestValue = value;
            }
        }
        return bestValue;
    }

    public boolean isFocusValid(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        AABB focus = new AABB();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                int color = pixels[index];
                if (Color.alpha(color) == ALPHA_FOCUS) {
                    pixels[index] = Color.argb(ALPHA_FOCUS, 255, 255, 255);
                    focus.addPoint(x, y);
                }
            }
        }

        int m = Math.max(w, h) / 10;
        AABB valid = new AABB();
        valid.addPoint(m, m);
        valid.addPoint(w - m, h - m);
        return focus.isInside(valid);
    }

    private float[] matrixTransformPoint(float[] matrix, float x, float y, float z) {
        float[] output = {0, 0, 0, 1};
        output[0] = x * matrix[0] + y * matrix[4] + z * matrix[8] + matrix[12];
        output[1] = x * matrix[1] + y * matrix[5] + z * matrix[9] + matrix[13];
        output[2] = x * matrix[2] + y * matrix[6] + z * matrix[10] + matrix[14];
        output[3] = x * matrix[3] + y * matrix[7] + z * matrix[11] + matrix[15];

        output[0] /= Math.abs(output[3]);
        output[1] /= Math.abs(output[3]);
        output[2] /= Math.abs(output[3]);
        output[3] = 1;
        return output;
    }
}
