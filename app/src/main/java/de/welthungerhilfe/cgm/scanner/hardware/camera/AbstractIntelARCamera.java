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

import com.intel.realsense.librealsense.DepthFrame;


import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.RenderToTexture;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;

public abstract class AbstractIntelARCamera implements GLSurfaceView.Renderer {

    public interface Camera2DataListener
    {
        void onColorDataReceived(Bitmap bitmap, int frameIndex);

        void onDepthDataReceived(Depthmap depthmap, int frameIndex, DepthFrame depthFrame, int height, int width, byte[] byteArray);

        void onAngleReceived(Double angle);

        void onDistancereceived(Float distance);
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
    protected ImageView mOutline;
    protected ImageView mDepthCameraPreview;
    protected GLSurfaceView mGLSurfaceView;
    protected ArrayList<Object> mListeners;
    int color;

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
    protected int mOrientation;

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
    protected float mOutlineAlpha = 0;
    protected int mPersonCount = 0;

    protected abstract void closeCamera();
    protected abstract void openCamera();
    protected abstract void updateFrame();
    public abstract int getPersonCount();

    public AbstractIntelARCamera(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
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
        mOrientation = -1;
    }

    public void onCreate(ImageView colorPreview, ImageView depthPreview, GLSurfaceView surfaceview, ImageView boundingBox) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, Color.TRANSPARENT);
        mColorCameraPreview = colorPreview;
        mColorCameraPreview.setImageBitmap(bitmap);
        mDepthCameraPreview = depthPreview;
        mDepthCameraPreview.setImageBitmap(bitmap);
        mOutline = boundingBox;

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
        synchronized (this) {
            updateFrame();
        }
    }

    protected void onProcessColorData(Bitmap bitmapPreview,Bitmap bitmapSave,int saveFrameIndex) {
        for (Object listener : mListeners) {
            if(saveFrameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0 && bitmapSave!=null && bitmapPreview!=null) {

                ((Camera2DataListener) listener).onColorDataReceived(bitmapSave, saveFrameIndex);
                LogFileUtils.logInfo2("ARRealsense1", "this is inside onProcess color data -> " +bitmapSave);

            }
        }
        LogFileUtils.logInfo2("ARRealsense1", "this is inside start streaming 5-> " +bitmapPreview);

        //update preview window
        mActivity.runOnUiThread(() -> {
            try {
                float scale = getPreviewScale(bitmapPreview);
                mColorCameraPreview.setImageBitmap(bitmapPreview);
                mColorCameraPreview.setRotation(90);
                mColorCameraPreview.setScaleX(scale);
                mColorCameraPreview.setScaleY(scale);
                mDepthCameraPreview.setRotation(90);
                mDepthCameraPreview.setScaleX(scale);
                mDepthCameraPreview.setScaleY(scale);
            }catch (Exception e){
                LogFileUtils.logInfo2("ERROR","this is error "+e.getMessage());
            }
        });
    }

    protected void onProcessDepthData(Depthmap depthmap, DepthFrame depthFrame, int height, int width, byte[] byteArray, int frameIndex) {
       /* if (depthmap == null) {
            return;
        }*/

        for (Object listener : mListeners) {
            if(frameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0) {

                ((Camera2DataListener)listener).onDepthDataReceived(depthmap, frameIndex,depthFrame,height,width,byteArray);
            }
        }



     /*   Bitmap preview = getDepthPreview(depthmap, mPlanes, mColorCameraIntrinsic);
        Bitmap finalPreview = skeletonVisualisation(preview);


        mActivity.runOnUiThread(() -> {
            mDepthCameraPreview.setImageBitmap(preview);
            if(mOutline!=null) {
                ImageViewCompat.setImageTintList(mOutline, ColorStateList.valueOf(color));
            }

           // mOutline.setColorFilter(color);
            Log.i("AbstractArCamera","this is value of outline "+color);
        });
*/
    }


    protected void onProcessDistance(float distance){
        for (Object listener : mListeners) {

            ((Camera2DataListener)listener).onDistancereceived(distance);
        }
    }
    protected void onProcessAngle(double angle){
        for (Object listener : mListeners) {

            ((Camera2DataListener)listener).onAngleReceived(angle);
        }

    }

    public void addListener(Object listener) {
        mListeners.add(listener);
    }

    public void removeListener(Object listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
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

        }
        return bitmapSize;
    }

    public float getTargetDistance() {
        return mTargetDistance;
    }

    public float getOrientation() {
        return mOrientation;
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
            mOutlineAlpha = -2;
            color = Color.WHITE;
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
            mOutlineAlpha = mOutlineAlpha * 0.9f + 0.1f;
            int alpha = Math.max(0, (int)(mOutlineAlpha * 128));
            color = Color.GREEN;
            if ((mTargetDistance < AppConstants.TOO_NEAR) || (mTargetDistance > AppConstants.TOO_FAR)) {
                color = Color.RED;
            } else if (!mSkeletonValid) {
                color = Color.RED;
            }
            color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));

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

