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
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.SizeF;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.utils.ComputerVisionUtils;

public abstract class AbstractARCamera {

    public interface Camera2DataListener
    {
        void onColorDataReceived(Bitmap bitmap, int frameIndex);

        void onDepthDataReceived(Image image, float[] position, float[] rotation, int frameIndex);
    }

    public enum DepthPreviewMode { OFF, SOBEL, PLANE, CENTER, FOCUS, CALIBRATION };

    public enum LightConditions { NORMAL, BRIGHT, DARK };

    public enum PlaneMode { LOWEST, VISIBLE };

    public enum PreviewSize { CLIPPED, FULL, SMALL };

    protected final String CALIBRATION_IMAGE_FILE = "plant.jpg";

    //app connection
    protected Activity mActivity;
    protected ImageView mColorCameraPreview;
    protected ImageView mDepthCameraPreview;
    protected GLSurfaceView mGLSurfaceView;
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
    protected float mTargetDistance;
    protected float mTargetHeight;
    protected ComputerVisionUtils.Point3F[] mCalibrationImageEdges;
    protected SizeF mCalibrationImageSizeCV;
    protected SizeF mCalibrationImageSizeToF;

    //scene understanding
    protected ComputerVisionUtils mComputerVision;
    protected DepthPreviewMode mDepthMode;
    protected LightConditions mLight;
    protected PlaneMode mPlaneMode;
    protected PreviewSize mPreviewSize;
    protected long mLastBright;
    protected long mLastDark;
    protected long mSessionStart;

    public abstract void onResume();
    public abstract void onPause();

    public AbstractARCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
        mActivity = activity;
        mComputerVision = new ComputerVisionUtils();
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
        mDepthMode = depthMode;
        mPreviewSize = previewSize;
        mLight = LightConditions.NORMAL;
        mPlaneMode = PlaneMode.LOWEST;
        mLastBright = 0;
        mLastDark = 0;
        mSessionStart = 0;
        mTargetHeight = 0;
        mTargetDistance = 1;
    }

    public void onCreate(ImageView colorPreview, ImageView depthPreview, GLSurfaceView surfaceview) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, Color.TRANSPARENT);
        mColorCameraPreview = colorPreview;
        mColorCameraPreview.setImageBitmap(bitmap);
        mDepthCameraPreview = depthPreview;
        mDepthCameraPreview.setImageBitmap(bitmap);
        mGLSurfaceView = surfaceview;
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

    public SizeF getCalibrationImageSize(boolean tof) {
        return tof ? mCalibrationImageSizeToF : mCalibrationImageSizeCV;
    }

    public float getLightIntensity() {
        return mPixelIntensity;
    }

    public LightConditions getLightConditionState() {
        mLight = updateLight(mLight, mLastBright, mLastDark, mSessionStart);
        return mLight;
    }

    public Bitmap getDepthPreview(Image image, boolean reorder, ArrayList<Float> planes, float[] calibration, float[] position, float[] rotation) {

        //get short buffer
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        if (reorder) {
            buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();

        //get buffer as array
        ArrayList<Integer> pixel = new ArrayList<>();
        while (shortDepthBuffer.hasRemaining()) {
            pixel.add((int) shortDepthBuffer.get());
        }

        //get depthmap
        float distance = 0;
        int center = Integer.MAX_VALUE;
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
                if (depthRange > 0) {
                    int value = Math.abs(x - width / 2) + Math.abs(y - height / 2);
                    if (center > value) {
                        center = value;
                        distance = depthRange * 0.001f;
                    }
                }
                depth[x][y] = depthRange;
            }
        }
        mTargetDistance = mTargetDistance * 0.9f + distance * 0.1f;

        float bestPlane;
        float[] matrix;
        switch (mDepthMode) {
            case CALIBRATION:
                matrix = mComputerVision.matrixCalculate(position, rotation);
                Matrix.invertM(matrix, 0, matrix, 0);
                ArrayList<ComputerVisionUtils.Point3F> edges;
                edges = mComputerVision.getCalibrationToFEdges(depth, calibration, matrix, mCalibrationImageEdges);

                mCalibrationImageSizeToF = null;
                if (edges.size() >= 4) {
                    float x = edges.get(0).distanceTo(edges.get(1));
                    float y = edges.get(0).distanceTo(edges.get(3));
                    if ((x > 0) && (y > 0)) {
                        mCalibrationImageSizeToF = new SizeF(x, y);
                    }
                }

                return mComputerVision.getDepthPreviewCalibration(depth, calibration, matrix, mCalibrationImageEdges);
            case CENTER:
            case FOCUS:
                boolean otherColors = mDepthMode == DepthPreviewMode.CENTER;
                matrix = mComputerVision.matrixCalculate(position, rotation);
                bestPlane = getPlane(depth, planes, calibration, matrix, position);
                Bitmap mask = mComputerVision.getDepthPreviewCenter(depth, bestPlane, calibration, matrix, otherColors);
                mTargetHeight = mComputerVision.getCenterFocusHeight(mask, depth, bestPlane, calibration, matrix);
                return mask;
            case PLANE:
                matrix = mComputerVision.matrixCalculate(position, rotation);
                bestPlane = getPlane(depth, planes, calibration, matrix, position);
                return mComputerVision.getDepthPreviewPlane(depth, bestPlane, calibration, matrix);
            case SOBEL:
                return mComputerVision.getDepthPreviewSobel(depth);
            default:
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    public float getPreviewScale(Bitmap bitmap) {
        float bitmapSize = bitmap.getWidth() / (float)bitmap.getHeight();
        switch (mPreviewSize) {
            case CLIPPED:
                return bitmapSize;
            case FULL:
                return bitmapSize * mColorCameraPreview.getHeight() / (float)bitmap.getWidth();
            case SMALL:
                return bitmapSize / mColorCameraPreview.getHeight() * (float)bitmap.getWidth();
        }
        return bitmapSize;
    }

    public float getTargetDistance() {
        return mTargetDistance;
    }

    public float getTargetHeight() {
        return mTargetHeight;
    }

    public void setPlaneMode(PlaneMode mode) {
        mPlaneMode = mode;
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

    private float getPlane(float[][] depth, ArrayList<Float> planes, float[] calibration, float[] matrix, float[] position) {
        switch (mPlaneMode) {
            case LOWEST:
                return mComputerVision.getPlaneLowest(planes, position);
            case VISIBLE:
                return mComputerVision.getPlaneVisible(depth, planes, calibration, matrix, position);
        }
        return Integer.MAX_VALUE;
    }
}
