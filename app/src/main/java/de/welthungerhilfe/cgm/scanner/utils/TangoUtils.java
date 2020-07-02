package de.welthungerhilfe.cgm.scanner.utils;

import android.net.Uri;

import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.experimental.TangoImageBuffer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

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

    // This function writes the XYZ points to .vtk files in binary
    public static Uri writePointCloudToVtkFile(TangoPointCloudData pointCloudData, File pointCloudSaveFolder, String pointCloudFilename) {

        ByteBuffer myBuffer = ByteBuffer.allocate(pointCloudData.numPoints * 4 * 4);
        myBuffer.order(ByteOrder.LITTLE_ENDIAN);

        myBuffer.asFloatBuffer().put(pointCloudData.points);

        File file = new File(pointCloudSaveFolder, pointCloudFilename+".vtk");

        try {

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));
            float confidence;

            out.write(("# vtk DataFile Version 3.0\n" +
                    "vtk output\n" +
                    "BINARY\n" +
                    "DATASET POLYDATA\n" +
                    "POINTS " + pointCloudData.numPoints + " float\n").getBytes());

            for (int i = 0; i < pointCloudData.numPoints; i++) {

                out.writeFloat(myBuffer.getFloat(4 * i * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 1) * 4));
                out.writeFloat(myBuffer.getFloat((4 * i + 2) * 4));
                confidence = myBuffer.getFloat((4 * i + 3) * 4);
            }

            out.write(("\nVERTICES 1 " + String.valueOf(pointCloudData.numPoints + 1) + "\n").getBytes());
            out.writeInt(pointCloudData.numPoints);
            for (int i = 0; i < pointCloudData.numPoints; i++) {
                out.writeInt(i);
            }

            out.write(("\nFIELD FieldData 1\n" + "timestamp 1 1 float\n").getBytes());
            out.writeFloat((float) pointCloudData.timestamp);

            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);
    }

    // This function writes the XYZC points to .pcd files in binary
    public static Uri writePointCloudToPcdFile(ByteBuffer buffer, int numPoints, double timestamp, String pose, File file) {

        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(("# timestamp 1 1 float "+timestamp+"\n").getBytes());
            out.write(("# .PCD v.7 - Point Cloud Data file format\n" +
                    "VERSION .7\n" +
                    "FIELDS x y z c\n" +
                    "SIZE 4 4 4 4\n" +
                    "TYPE F F F F\n" +
                    "COUNT 1 1 1 1\n" +
                    "WIDTH " + numPoints + "\n"+
                    "HEIGHT 1\n" +
                    "VIEWPOINT " + pose + "\n" +
                    "POINTS " + numPoints + "\n" +
                    "DATA ascii\n").getBytes());

            StringBuilder output = new StringBuilder();
            for (int i = 0; i < numPoints; i++) {
                float pcx =  buffer.getFloat();
                float pcy =  buffer.getFloat();
                float pcz = buffer.getFloat();
                float pcc = buffer.getFloat();
                output.append(String.format(Locale.US, "%f %f %f %f\n", pcx, pcy, pcz, pcc));
            }
            out.write(output.toString().getBytes());
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);
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
}
