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
package de.welthungerhilfe.cgm.scanner.hardware.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Depthmap {
    byte[][] confidence;
    float[][] depth;
    float distance;
    int count;
    int width;
    int height;
    float[] position;
    float[] rotation;
    long timestamp;

    public Depthmap(int width, int height) {
        this.width = width;
        this.height = height;

        count = 0;
        distance = 0;
        confidence = new byte[width][height];
        depth = new float[width][height];
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
                short depthRange = (short) (depth[x][y] * 1000);
                output[index++] = (byte) (depthRange / 256);
                output[index++] = (byte) (depthRange % 256);
                output[index++] = confidence[x][y];
            }
        }
        return output;
    }

    public float[] getMatrix() {
        float[] matrix = {1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1};

        double sqw = rotation[3] * rotation[3];
        double sqx = rotation[0] * rotation[0];
        double sqy = rotation[1] * rotation[1];
        double sqz = rotation[2] * rotation[2];

        double invs = 1 / (sqx + sqy + sqz + sqw);
        matrix[0] = (float) ((sqx - sqy - sqz + sqw) * invs);
        matrix[5] = (float) ((-sqx + sqy - sqz + sqw) * invs);
        matrix[10] = (float) ((-sqx - sqy + sqz + sqw) * invs);

        double tmp1 = rotation[0] * rotation[1];
        double tmp2 = rotation[2] * rotation[3];
        matrix[1] = (float) (2.0 * (tmp1 + tmp2) * invs);
        matrix[4] = (float) (2.0 * (tmp1 - tmp2) * invs);

        tmp1 = rotation[0] * rotation[2];
        tmp2 = rotation[1] * rotation[3];
        matrix[2] = (float) (2.0 * (tmp1 - tmp2) * invs);
        matrix[8] = (float) (2.0 * (tmp1 + tmp2) * invs);

        tmp1 = rotation[1] * rotation[2];
        tmp2 = rotation[0] * rotation[3];
        matrix[6] = (float) (2.0 * (tmp1 + tmp2) * invs);
        matrix[9] = (float) (2.0 * (tmp1 - tmp2) * invs);

        matrix[12] = position[0];
        matrix[13] = position[1];
        matrix[14] = position[2];
        return matrix;
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

    public void save(File file) {
        try {
            byte[] data = getData();
            FileOutputStream stream = new FileOutputStream(file);
            ZipOutputStream zip = new ZipOutputStream(stream);
            byte[] info = (getWidth() + "x" + getHeight() + "_0.001_7_" + getPose("_") + "\n").getBytes();
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
