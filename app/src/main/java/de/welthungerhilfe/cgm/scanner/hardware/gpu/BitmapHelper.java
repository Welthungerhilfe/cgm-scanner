/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.hardware.gpu;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;

import de.welthungerhilfe.cgm.scanner.AppConstants;

public class BitmapHelper {

    public static final int JPG_COMPRESSION = 90;

    public static Bitmap getAcceptableBitmap(Bitmap bmp) {
        float ratio = 0;
        float scaledWidth = 0, scaledHeight = 0;
        if (bmp.getHeight() > AppConstants.MAX_IMAGE_SIZE) {
            ratio = (float)AppConstants.MAX_IMAGE_SIZE / bmp.getHeight();
            scaledWidth = bmp.getWidth() * ratio;
            scaledHeight= bmp.getHeight() * ratio;
        }
        if (bmp.getWidth() > AppConstants.MAX_IMAGE_SIZE) {
            ratio = (float) AppConstants.MAX_IMAGE_SIZE / bmp.getWidth();
            scaledWidth = bmp.getWidth() * ratio;
            scaledHeight= bmp.getHeight() * ratio;
        }

        if (ratio == 0)
            return bmp;
        else
            return getResizedBitmap(bmp, (int)scaledWidth, (int)scaledHeight);
    }
    public static Bitmap getBitmap(int w, int h){
        int[] b = new int[w * h];
        int[] bt = new int[w * h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int i = 0, k = 0; i < h; i++, k++) {
            for (int j = 0; j < w; j++) {
                int pix = b[i * w + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                bt[(h - k - 1) * w + j] = (pix & 0xff00ff00) | pr | pb;
            }
        }

        return Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
    }
    public static byte[] getByteData(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int w, int h) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) w) / width;
        float scaleHeight = ((float) h) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

    public static void writeBitmapToFile(Bitmap bitmap, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPG_COMPRESSION, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
