package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoPointCloudData;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.activities.RecorderActivity;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.service.FirebaseUploadService;

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
public class PointCloudUtils {

    // This function writes the XYZ points to .vtk files in binary
    public static Uri writePointCloudToVtkFile(TangoPointCloudData pointCloudData,
                                       File pointCloudSaveFolder, String pointCloudFilename) {

        ByteBuffer myBuffer = ByteBuffer.allocate(pointCloudData.numPoints * 4 * 4);
        myBuffer.order(ByteOrder.LITTLE_ENDIAN);

        myBuffer.asFloatBuffer().put(pointCloudData.points);

        File file = new File(pointCloudSaveFolder, pointCloudFilename);

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

}
