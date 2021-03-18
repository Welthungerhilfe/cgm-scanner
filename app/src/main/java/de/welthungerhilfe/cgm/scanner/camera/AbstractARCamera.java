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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public abstract class AbstractARCamera {

    public interface Camera2DataListener
    {
        void onColorDataReceived(Bitmap bitmap, int frameIndex);

        void onDepthDataReceived(Image image, float[] position, float[] rotation, int frameIndex);
    }

    public enum LightConditions { NORMAL, BRIGHT, DARK };

    //app connection
    protected Activity mActivity;
    protected ImageView mColorCameraPreview;
    protected ImageView mDepthCameraPreview;
    protected ArrayList<Object> mListeners;
    protected final Object mLock;

    //camera calibration
    protected float[] mColorCameraIntrinsic;
    protected float[] mDepthCameraIntrinsic;
    protected float[] mDepthCameraTranslation;
    protected boolean mHasCameraCalibration;

    //camera pose
    protected float[] mPosition;
    protected float[] mRotation;

    //AR status
    protected int mFrameIndex;
    protected float mPixelIntensity;
    protected boolean mShowDepth;
    protected LightConditions mLight;
    protected long mLastBright;
    protected long mLastDark;
    protected long mSessionStart;

    public abstract void onResume();
    public abstract void onPause();

    public AbstractARCamera(Activity activity, boolean showDepth) {
        mActivity = activity;
        mListeners = new ArrayList<>();
        mLock = new Object();

        mPosition = new float[3];
        mRotation = new float[4];

        mColorCameraIntrinsic = new float[4];
        mDepthCameraIntrinsic = new float[4];
        mDepthCameraTranslation = new float[3];
        mHasCameraCalibration = false;

        mFrameIndex = 1;
        mPixelIntensity = 0;
        mShowDepth = showDepth;
        mLight = LightConditions.NORMAL;
        mLastBright = 0;
        mLastDark = 0;
        mSessionStart = 0;
    }

    public void onCreate(int colorPreview, int depthPreview, int surfaceview) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, Color.TRANSPARENT);
        mColorCameraPreview = mActivity.findViewById(colorPreview);
        mColorCameraPreview.setImageBitmap(bitmap);
        mDepthCameraPreview = mActivity.findViewById(depthPreview);
        mDepthCameraPreview.setImageBitmap(bitmap);
    }

    public void addListener(Object listener) {
        mListeners.add(listener);
    }

    public void removeListener(Object listener) {
        mListeners.remove(listener);
    }

    public Depthmap extractDepthmap(Image image, float[] position, float[] rotation, boolean reorder) {
        if (image == null) {
            Depthmap depthmap = new Depthmap(0, 0);
            depthmap.position = position;
            depthmap.rotation = rotation;
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
        depthmap.position = position;
        depthmap.rotation = rotation;
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

    public String getCameraCalibration() {
        String output = "";
        output += "Color camera intrinsic:\n";
        output += mColorCameraIntrinsic[0] + " " + mColorCameraIntrinsic[1] + " " + mColorCameraIntrinsic[2] + " " + mColorCameraIntrinsic[3] + "\n";
        output += "Depth camera intrinsic:\n";
        output += mDepthCameraIntrinsic[0] + " " + mDepthCameraIntrinsic[1] + " " + mDepthCameraIntrinsic[2] + " " + mDepthCameraIntrinsic[3] + "\n";
        output += "Depth camera position:\n";
        output += mDepthCameraTranslation[0] + " " + mDepthCameraTranslation[1] + " " + mDepthCameraTranslation[2] + "\n";
        return output;
    }

    public boolean hasCameraCalibration() {
        return mHasCameraCalibration;
    }

    public float getLightIntensity() {
        return mPixelIntensity;
    }

    public LightConditions getLightConditionState() {
        mLight = updateLight(mLight, mLastBright, mLastDark, mSessionStart);
        return mLight;
    }

    public Bitmap getDepthPreview(Image image, boolean reorder) {
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

    public LightConditions updateLight(LightConditions light, long lastBright, long lastDark, long sessionStart) {
        //prevent showing "too bright" directly after changing light conditions
        double diff = Math.abs(lastBright - System.currentTimeMillis());
        if (light == LightConditions.BRIGHT) {
            if (diff < 3000) {
                light = LightConditions.NORMAL;
            }
        }

        //prevent showing messages directly after session start
        if ((Math.abs(lastBright - sessionStart) < 2000) || (Math.abs(lastDark - sessionStart) < 2000)) {
            light = LightConditions.NORMAL;
        }

        //reset light state if there was longer no change
        diff = Math.min(diff, Math.abs(lastDark - System.currentTimeMillis()));
        if (diff > 1000) {
            light = LightConditions.NORMAL;
        }

        return light;
    }

    protected ByteBuffer imageToByteBuffer(Image image) {
        final int  width  = image.getWidth();
        final int  height = image.getHeight();

        final Image.Plane[] planes     = image.getPlanes();
        final byte[]        rowData    = new byte[planes[0].getRowStride()];
        final int           bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer    output     = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset;
        int outputStride;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer      = planes[planeIndex].getBuffer();
            final int        rowStride   = planes[planeIndex].getRowStride();
            final int        pixelStride = planes[planeIndex].getPixelStride();

            final int shift         = (planeIndex == 0) ? 0 : 1;
            final int widthShifted  = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(0);

            for (int row = 0; row < heightShifted; row++) {
                int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return output;
    }
}
