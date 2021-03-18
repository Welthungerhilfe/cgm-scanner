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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Depthmap {
    byte[] confidence;
    short[] depth;
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
