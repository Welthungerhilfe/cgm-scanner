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

package de.welthungerhilfe.cgm.scanner.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Environment;

import com.google.atap.tangoservice.experimental.TangoImageBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

/**
 * Created by Emerald on 2/21/2018.
 */

public class BitmapUtils {

    public static File saveBitmap(byte[] imgData, String fileName) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CGM Scanner");
        if (!mediaStorageDir.exists())
            mediaStorageDir.mkdir();
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(imgData);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pictureFile;
    }

    public static void saveBitmap(Bitmap bitmap, String fileName) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CGM Scanner");
        if (!mediaStorageDir.exists())
            mediaStorageDir.mkdir();
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + fileName);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getRotatedBitmap(Bitmap bmp, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
    }

    public static Bitmap getRotatedBitmap(byte[] data, float degree) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap rotatedBmp = getRotatedBitmap(bmp, degree);

        return rotatedBmp;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int w, int h) {
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    public static byte[] getResizedByte(byte[] data, int w, int h) {
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getRotatedByte(Bitmap bmp, float degree) {
        Bitmap rotatedBmp = getRotatedBitmap(bmp, degree);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getRotatedByte(byte[] data, float degree) {
        Bitmap rotatedBmp = getRotatedBitmap(data, degree);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getResizedByte(Bitmap bm, int w, int h) {
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

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

    public static byte[] getByteData(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }


    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    //@AddTrace(name = "convertYUV420_NV21toRGB8888")
    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f*v);
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)(1.772f*u);
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }

    public static Uri writeImageToFile(TangoImageBuffer currentTangoImageBuffer, File rgbSaveFolder, String currentImgFilename) {
        File currentImg = new File(rgbSaveFolder,currentImgFilename);

        int currentImgWidth = currentTangoImageBuffer.width;
        int currentImgHeight = currentTangoImageBuffer.height;

        // TODO performance:
        // 1. write only to file here (or write video from GLSurface)
        // 2. queue in upload service #18
        // 3. post-processing (rotate) in UploadService

        // switched heigth and width for rotated image
            /*
            byte[] YuvImageByteArray = rotateYUV420Degree90(currentTangoImageBuffer.data.array(), currentImgWidth, currentImgHeight);
            int tmp = currentImgWidth;
            currentImgWidth = currentImgHeight;
            currentImgHeight = tmp;
            */
        byte[] YuvImageByteArray = currentTangoImageBuffer.data.array();

        try (FileOutputStream out = new FileOutputStream(currentImg)) {
            YuvImage yuvImage = new YuvImage(YuvImageByteArray, ImageFormat.NV21, currentImgWidth, currentImgHeight, null);
            yuvImage.compressToJpeg(new Rect(0, 0, currentImgWidth, currentImgHeight), 50, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(currentImg);
    }

}
