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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Size;
import android.util.SizeF;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.RenderToTexture;
import de.welthungerhilfe.cgm.scanner.utils.ComputerVisionUtils;

public abstract class AbstractARCamera implements GLSurfaceView.Renderer {

    private final float DEPTH_FUSION_LERP_MAX = 1.0f;
    private final float DEPTH_FUSION_LERP_MIN = 0.25f;
    private final float DEPTH_FUSION_MAX_DEPTH = 3;
    private final float DEPTH_FUSION_MAX_DIFF = 0.03f;
    private final float DEPTH_FUSION_MIN_DISTANCE = 0.25f;
    private final float DEPTH_FUSION_NOISE_FACTOR = 100;

    public interface Camera2DataListener
    {
        void onColorDataReceived(Bitmap bitmap, int frameIndex);

        void onDepthDataReceived(Depthmap depthmap, int frameIndex);
    }

    public enum DepthPreviewMode { OFF, SOBEL, PLANE, CENTER, CENTER_LOW_POWER, FOCUS, FOCUS_LOW_POWER, CALIBRATION };

    public enum LightConditions { NORMAL, BRIGHT, DARK };

    public enum PlaneMode { LOWEST, VISIBLE };

    public enum PreviewSize { CLIPPED, FULL, SMALL };

    public enum SkeletonMode { OFF, LINES, OUTLINE };

    protected final String CALIBRATION_IMAGE_FILE = "plant.jpg";

    //app connection
    protected Activity mActivity;
    protected ImageView mColorCameraPreview;
    protected ImageView mDepthCameraPreview;
    protected GLSurfaceView mGLSurfaceView;
    protected ArrayList<Object> mListeners;

    //camera calibration
    protected float[] mColorCameraIntrinsic;
    protected float[] mDepthCameraIntrinsic;
    protected boolean mHasCameraCalibration;

    //camera rendering
    protected int mCameraTextureId;
    protected RenderToTexture mRTT;
    protected Size mTextureRes;
    protected boolean mViewportChanged;
    protected int mViewportWidth;
    protected int mViewportHeight;

    //depthmap fusion
    private Depthmap mDepthmap;
    private Depthmap mLastDepthmap;
    private float mNoiseAmount;

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
    protected SkeletonMode mSkeletonMode;
    protected ArrayList<Float> mPlanes;
    protected ArrayList<PointF> mSkeleton;
    protected boolean mSkeletonValid;
    protected long mLastBright;
    protected long mLastDark;
    protected long mSessionStart;
    protected int mPersonCount = 0;

    protected abstract void closeCamera();
    protected abstract void openCamera();
    protected abstract void updateFrame();
    public abstract int getPersonCount();

    public AbstractARCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
        mActivity = activity;
        mComputerVision = new ComputerVisionUtils();
        mListeners = new ArrayList<>();
        mRTT = new RenderToTexture();

        mColorCameraIntrinsic = new float[4];
        mDepthCameraIntrinsic = new float[4];
        mHasCameraCalibration = false;

        mFrameIndex = 1;
        mPixelIntensity = 0;
        mPlanes = new ArrayList<>();
        mSkeleton = new ArrayList<>();
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
        mGLSurfaceView.setPreserveEGLContextOnPause(true);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLSurfaceView.setWillNotDraw(false);
    }

    public void onPause() {
        mGLSurfaceView.onPause();

        closeCamera();
    }

    public void onResume() {
        mGLSurfaceView.onResume();
        mRTT.reset();

        if (mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mActivity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        // Generate the background texture.
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mCameraTextureId = textures[0];
        int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        GLES20.glBindTexture(textureTarget, mCameraTextureId);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mViewportWidth = width;
        mViewportHeight = height;
        mViewportChanged = true;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized (AbstractARCamera.this) {
            updateFrame();
        }
    }

    protected void onProcessColorData(Bitmap bitmap) {
        for (Object listener : mListeners) {
            ((Camera2DataListener)listener).onColorDataReceived(bitmap, mFrameIndex);
        }

        //update preview window
        mActivity.runOnUiThread(() -> {
            float scale = getPreviewScale(bitmap);
            mColorCameraPreview.setImageBitmap(bitmap);
            mColorCameraPreview.setRotation(90);
            mColorCameraPreview.setScaleX(scale);
            mColorCameraPreview.setScaleY(scale);
            mDepthCameraPreview.setRotation(90);
            mDepthCameraPreview.setScaleX(scale);
            mDepthCameraPreview.setScaleY(scale);
        });
    }

    protected void onProcessDepthData(Depthmap depthmap) {
        if (depthmap == null) {
            return;
        }

        for (Object listener : mListeners) {
            ((Camera2DataListener)listener).onDepthDataReceived(depthmap, mFrameIndex);
        }

        Bitmap preview = getDepthPreview(depthmap, mPlanes, mColorCameraIntrinsic);
        Bitmap finalPreview = skeletonVisualisation(preview);
        mActivity.runOnUiThread(() -> mDepthCameraPreview.setImageBitmap(finalPreview));
    }

    public void addListener(Object listener) {
        mListeners.add(listener);
    }

    public void removeListener(Object listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    public Depthmap updateDepthmap(Image image, float[] position, float[] rotation) {
        if (image == null) {
            if (mDepthmap != null) {
                mDepthmap.position = position;
                mDepthmap.rotation = rotation;
                return mDepthmap;
            } else {
                Depthmap depthmap = new Depthmap(0, 0);
                depthmap.position = position;
                depthmap.rotation = rotation;
                depthmap.timestamp = 0;
                return depthmap;
            }
        }

        //extract data
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortDepthBuffer = buffer.asShortBuffer();
        ArrayList<Short> pixel = new ArrayList<>();
        while (shortDepthBuffer.hasRemaining()) {
            pixel.add(shortDepthBuffer.get());
        }

        //extract metadata
        int stride = plane.getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();
        if (mDepthmap == null) {
            mDepthmap = new Depthmap(width, height);
            mLastDepthmap = new Depthmap(width, height);
        }
        mDepthmap.distance = 0;
        mDepthmap.position = position;
        mDepthmap.rotation = rotation;
        mDepthmap.timestamp = image.getTimestamp();
        image.close();

        //swap depthmaps
        Depthmap temp = mLastDepthmap;
        mLastDepthmap = mDepthmap;
        mDepthmap = temp;

        //update depthmap
        int avgCount = 0;
        float avgDepth = 0;
        int diffCount = 0;
        float diffDepth = 0;
        int cx = width / 2;
        int cy = height / 2;
        int center = Integer.MAX_VALUE;
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
                    int value = Math.abs(x - cx) + Math.abs(y - cy);
                    if (center > value) {
                        center = value;
                        mDepthmap.distance = depthRange * 0.001f;
                    }
                    avgCount++;
                    avgDepth += depthRange * 0.001f;
                    mDepthmap.count++;
                }
                mDepthmap.confidence[x][y] = (byte) depthConfidence;
                mDepthmap.depth[x][y] = depthRange * 0.001f;
                if (mDepthmap.depth[x][y] < DEPTH_FUSION_MAX_DEPTH) {
                    diffDepth += Math.abs(mLastDepthmap.depth[x][y] - mDepthmap.depth[x][y]) > DEPTH_FUSION_MAX_DIFF ? 1 : 0;
                    diffCount++;
                }
            }
        }

        //depth fusion
        if (avgCount > 0) {
            avgDepth /= avgCount;
            if (avgDepth > DEPTH_FUSION_MIN_DISTANCE) {
                if (diffCount > 0) {
                    diffDepth /= diffCount;
                    diffDepth = diffDepth * diffDepth * DEPTH_FUSION_NOISE_FACTOR;
                    float lerp = Math.max(Math.min(diffDepth, DEPTH_FUSION_LERP_MAX), DEPTH_FUSION_LERP_MIN);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            if ((mDepthmap.depth[x][y] > 0) && (mLastDepthmap.depth[x][y] > 0)) {
                                mDepthmap.depth[x][y] = mLastDepthmap.depth[x][y] * (1 - lerp) + mDepthmap.depth[x][y] * lerp;
                            }
                        }
                    }
                }
            }
        }
        mNoiseAmount = diffDepth > 0.0005f ? diffDepth : 1000;
        return mDepthmap;
    }

    public String getCameraCalibration() {
        String output = "";
        output += "Color camera intrinsic:\n";
        output += mColorCameraIntrinsic[0] + " " + mColorCameraIntrinsic[1] + " " + mColorCameraIntrinsic[2] + " " + mColorCameraIntrinsic[3] + "\n";
        output += "Depth camera intrinsic:\n";
        output += mDepthCameraIntrinsic[0] + " " + mDepthCameraIntrinsic[1] + " " + mDepthCameraIntrinsic[2] + " " + mDepthCameraIntrinsic[3] + "\n";
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

    public float getDepthNoiseAmount() {
        return mNoiseAmount;
    }

    public Bitmap getDepthPreview(Depthmap depthmap, ArrayList<Float> planes, float[] calibration) {

        DepthPreviewMode mode = mDepthMode;
        switch (mDepthMode) {
            case CENTER_LOW_POWER:
                if (mFrameIndex % 60 == 0) {
                    mode = DepthPreviewMode.CENTER;
                } else {
                    mode = DepthPreviewMode.SOBEL;
                }
                break;
            case FOCUS_LOW_POWER:
                if (mFrameIndex % 60 == 0) {
                    mode = DepthPreviewMode.FOCUS;
                    break;
                } else {
                    return null;
                }
        }
        mTargetDistance = mTargetDistance * 0.9f + depthmap.distance * 0.1f;

        float bestPlane;
        float[] matrix = depthmap.getMatrix();
        switch (mode) {
            case CALIBRATION:
                Matrix.invertM(matrix, 0, matrix, 0);
                ArrayList<ComputerVisionUtils.Point3F> edges;
                edges = mComputerVision.getCalibrationToFEdges(depthmap.depth, calibration, matrix, mCalibrationImageEdges);

                mCalibrationImageSizeCV = null;
                if (mCalibrationImageEdges.length >= 4) {
                    float x = mCalibrationImageEdges[0].distanceTo(mCalibrationImageEdges[1]);
                    float y = mCalibrationImageEdges[0].distanceTo(mCalibrationImageEdges[3]);
                    if ((x > 0) && (y > 0)) {
                        mCalibrationImageSizeCV = new SizeF(x, y);
                    }
                }

                mCalibrationImageSizeToF = null;
                if (edges.size() >= 4) {
                    float x = edges.get(0).distanceTo(edges.get(1));
                    float y = edges.get(0).distanceTo(edges.get(3));
                    if ((x > 0) && (y > 0)) {
                        mCalibrationImageSizeToF = new SizeF(x, y);
                        mCalibrationImageSizeCV = mCalibrationImageSizeToF;
                    }
                }

                return mComputerVision.getDepthPreviewCalibration(depthmap.depth, calibration, matrix, mCalibrationImageEdges);
            case CENTER:
            case FOCUS:
                boolean otherColors = mDepthMode == DepthPreviewMode.CENTER;
                bestPlane = getPlane(depthmap.depth, planes, calibration, matrix, depthmap.position);
                Bitmap mask = mComputerVision.getDepthPreviewCenter(depthmap.depth, bestPlane, otherColors);
                mTargetHeight = mComputerVision.getCenterFocusHeight(mask, depthmap.depth, bestPlane, calibration, matrix);

                boolean valid = mComputerVision.isFocusValid(mask) && (mTargetHeight >= 0.45) && (mTargetHeight <= 1.3);
                if (!otherColors && !valid) {
                    mask = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                }
                if (mDepthMode == DepthPreviewMode.CENTER_LOW_POWER) {
                    return null;
                } else {
                    return mask;
                }
            case PLANE:
                bestPlane = getPlane(depthmap.depth, planes, calibration, matrix, depthmap.position);
                return mComputerVision.getDepthPreviewPlane(depthmap.depth, bestPlane, calibration, matrix);
            case SOBEL:
                return mComputerVision.getDepthPreviewSobel(depthmap.depth);
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
                if (this instanceof ARCoreCamera) {
                    return bitmapSize / mColorCameraPreview.getHeight() * (float)bitmap.getHeight();
                } else {
                    return bitmapSize / mColorCameraPreview.getHeight() * (float)bitmap.getWidth();
                }
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

    public void setSkeletonMode(SkeletonMode mode) {
        mSkeletonMode = mode;
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

    private float getPlane(float[][] depth, ArrayList<Float> planes, float[] calibration, float[] matrix, float[] position) {
        switch (mPlaneMode) {
            case LOWEST:
                return mComputerVision.getPlaneLowest(planes, position);
            case VISIBLE:
                return mComputerVision.getPlaneVisible(depth, planes, calibration, matrix, position);
        }
        return Integer.MAX_VALUE;
    }


    private Bitmap skeletonVisualisation(Bitmap preview) {

        if (mSkeletonMode == SkeletonMode.OFF) {
            return preview;
        }

        //skeleton visualisation
        if (mPersonCount == 1) {
            if (preview == null || (preview.getWidth() == 1)) {
                preview = Bitmap.createBitmap(240, 180, Bitmap.Config.ARGB_8888);
            }
            preview = preview.copy(Bitmap.Config.ARGB_8888, true);
            Canvas c = new Canvas(preview);

            //define look of the visualisation
            int color = Color.argb(128, 0, 255, 0);
            if ((mTargetDistance < AppConstants.TOO_NEAR) || (mTargetDistance > AppConstants.TOO_FAR)) {
                color = Color.argb(128, 255, 255, 0);
            } else if (!mSkeletonValid) {
                color = Color.argb(128, 255, 255, 0);
            }

            //draw bones
            for (int pass = 0; pass < ((mSkeletonMode == SkeletonMode.OUTLINE) ? 2 : 1); pass++) {
                int r = (mSkeletonMode == SkeletonMode.OUTLINE) ? (pass == 0 ? 20 : 15) : 1;
                Paint p = new Paint();
                p.setColor(pass == 0 ? color : Color.BLACK);
                p.setStrokeWidth(r * 2);
                for (int i = 0; i < mSkeleton.size(); i += 2) {
                    int x1 = (int) (mSkeleton.get(i).x * (float)preview.getWidth());
                    int y1 = (int) (mSkeleton.get(i).y * (float)preview.getHeight());
                    int x2 = (int) (mSkeleton.get(i + 1).x * (float)preview.getWidth());
                    int y2 = (int) (mSkeleton.get(i + 1).y * (float)preview.getHeight());
                    if ((x1 != 0) && (y1 != 0) && (x2 != 0) && (y2 != 0)) {
                        if (mSkeletonMode == SkeletonMode.OUTLINE) {
                            c.drawCircle(x1, y1, r, p);
                            c.drawCircle(x2, y2, r, p);
                        }
                        c.drawLine(x1, y1, x2, y2, p);
                    }
                }
            }

            //masking which creates outline
            if (mSkeletonMode == SkeletonMode.OUTLINE) {
                int w = preview.getWidth();
                int h = preview.getHeight();
                int[] pixels = new int[w * h];
                preview.getPixels(pixels, 0, w, 0, 0, w, h);
                for (int i = 0; i < pixels.length; i++) {
                    if (pixels[i] == Color.BLACK) {
                        pixels[i] = Color.TRANSPARENT;
                    }
                }
                preview.setPixels(pixels, 0, w, 0, 0, w, h);
            }
        }
        return preview;
    }
}
