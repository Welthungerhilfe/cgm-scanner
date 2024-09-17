package de.welthungerhilfe.cgm.scanner.hardware.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Float3;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.intel.realsense.librealsense.Align;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.Intrinsic;
import com.intel.realsense.librealsense.MotionFrame;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;
import com.intel.realsense.librealsense.VideoStreamProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;

public class ARRealSenseCamera1 extends AbstractIntelARCamera{

    private static final String TAG = "librs capture example";
    private boolean mIsStreaming = false;
    private Pipeline mPipeline;

    private RsContext mRsContext;

    private Align mAlign;
    public ARRealSenseCamera1(Activity activity, DepthPreviewMode depthMode, PreviewSize previewSize) {
        super(activity, depthMode, previewSize);
    }

    boolean isBackgrounThreadActive = false;
    @Override
    protected void closeCamera() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            stopStreaming();
            mPipeline.stop();
            Log.d(TAG, "streaming stopped successfully");
        }  catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
            mPipeline = null;
        }
    }




    @Override
    protected void openCamera() {
        RsContext.init(mActivity);

        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);
        mPipeline = new Pipeline();

        try(DeviceList dl = mRsContext.queryDevices()){
            if(dl.getDeviceCount() > 0) {
                // showConnectLabel(false);
                start();
            }
        }
    }

    private synchronized void start() {
        if(mIsStreaming)
            return;
        try{
            mIsStreaming = true;

            configAndStart();
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {Log.d(TAG, "failed to start streaming");
        }
    }
    private void configAndStart() throws Exception {

        try(Config config  = new Config())
        {
            options =
                    new AccuratePoseDetectorOptions.Builder()
                            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                            .build();
            poseDetector = PoseDetection.getClient(options);


            config.enableStream(StreamType.DEPTH, 1280, 720);
            config.enableStream(StreamType.COLOR, 1280, 720);
            config.enableStream(StreamType.ACCEL, StreamFormat.MOTION_XYZ32F);

            config.enableStream(StreamType.ACCEL);



            // try statement needed here to release resources allocated by the Pipeline:start() method
            mPipeline.start(config);

            mAlign = new Align(StreamType.COLOR);

            startStreaming();

        }catch (Exception e){

        }
    }



    private Handler handler = new Handler(Looper.getMainLooper());
    private Thread backgroundThread;
    Bitmap bitmap1 = null;
    Frame colorFrame1;

    boolean intrisicGenerated = false;
    String angle;
    public void startStreaming() {
        backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!backgroundThread.isInterrupted()) {
                    try {
                        if (!mIsStreaming) {
                            return;
                        }

                        try (FrameSet frames = mPipeline.waitForFrames()) {

                            if(!intrisicGenerated) {
                                VideoStreamProfile videoStreamProfile = frames.getProfile().as(Extension.VIDEO_PROFILE);
                                Intrinsic intrinsics = videoStreamProfile.getIntrinsic();

                                LogFileUtils.logInfoOffline("Width: "," "+ intrinsics.getWidth());
                                LogFileUtils.logInfoOffline("Height: "," "+ intrinsics.getHeight());
                                LogFileUtils.logInfoOffline("Principal Point X (ppx): "," " + intrinsics.getmPpx());
                                LogFileUtils.logInfoOffline("Principal Point Y (ppy): "," " + intrinsics.getmPpy());
                                LogFileUtils.logInfoOffline("Focal Length X (fx): "," " + intrinsics.getmFx());
                                LogFileUtils.logInfoOffline("Focal Length Y (fy): "," " + intrinsics.getmFy());
                                LogFileUtils.logInfoOffline("Distortion Model: "," " + intrinsics.getModel());

                                mColorCameraIntrinsic[0] = intrinsics.getmFy()/ (float) intrinsics.getHeight();
                                mColorCameraIntrinsic[1] = intrinsics.getmFx() / (float) intrinsics.getWidth();
                                mColorCameraIntrinsic[2] = intrinsics.getmPpy() / (float) intrinsics.getHeight();
                                mColorCameraIntrinsic[3] = intrinsics.getmPpx() / (float) intrinsics.getWidth();

                            }
                            intrisicGenerated = true;

                            try( Frame accelFrame = frames.first(StreamType.ACCEL,StreamFormat.MOTION_XYZ32F)) {
                                MotionFrame motionFrame = accelFrame.as(Extension.MOTION_FRAME);
                                angle = captureGyroData(motionFrame);
                            }catch (Exception e){
                            }



                            // Process color frame for UI (on main thread)
                            try (Frame f1 = frames.first(StreamType.COLOR)) {
                                colorFrame1 = f1;
                                bitmap1 = frameToBitmap(colorFrame1);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onProcessColorData(bitmap1, null, 0);
                                    //    createPose(bitmap1);
                                       onProcessAngle();
                                    }
                                });
                            }

                            try (Frame depth = frames.first(StreamType.DEPTH)) {
                                DepthFrame depthFrame1 = depth.as(Extension.DEPTH_FRAME);

                                mTargetDistance = depthFrame1.getDistance(depthFrame1.getWidth() / 2, depthFrame1.getHeight() / 2);
                               /* handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onProcessDistance(depthValue);
                                    }
                                });*/
                            }
                            if (mFrameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0 && !isBackgrounThreadActive) {
                                saveAlignFrames1(frames,mFrameIndex);
                            }
                        } catch (Exception e) {
                        }


                        try {
                            Thread.sleep(50); // Adjust delay as needed
                        } catch (InterruptedException e) {
                            break;
                        }
                        mFrameIndex++;
                    } catch (Exception e) {

                    }
                }
            }
        });
        backgroundThread.start();
    }

    private class SaveAlignFramesTask extends AsyncTask<Void, Void, Void> {
        private FrameSet frameSet;
        private int frameIndex;
        private DepthFrame depthFrameSave;
        private Frame colorFrameSave;
        private Bitmap bitmapSave;
        private byte[] byteArray;
        private int width, height, stride, dataSize;

        public SaveAlignFramesTask(FrameSet frameSet, int frameIndex) {
            this.frameSet = frameSet.clone();
            this.frameIndex = frameIndex;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try (FrameSet alignedFrames = mAlign.process(frameSet)) {

                try (Frame f = alignedFrames.first(StreamType.DEPTH)) {
                    depthFrameSave = f.as(Extension.DEPTH_FRAME);
                }
                try (Frame f1 = alignedFrames.first(StreamType.COLOR)) {
                    colorFrameSave = f1;
                }

                bitmapSave = frameToBitmap(colorFrameSave);
                width = depthFrameSave.getWidth();
                height = depthFrameSave.getHeight();
                stride = depthFrameSave.getStride();
                dataSize = stride * height;
                byteArray = new byte[dataSize];
                depthFrameSave.getData(byteArray);

            } catch (Exception e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isBackgrounThreadActive = false;
            onProcessColorData(bitmap1, bitmapSave, frameIndex);
            onProcessDepthData(null, depthFrameSave, height, width, byteArray, frameIndex);


            frameSet.close();
        }
    }

    void saveAlignFrames1(FrameSet frameSet, int frameIndex){

        if (frameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0 && !isBackgrounThreadActive) {
            isBackgrounThreadActive = true;
            new SaveAlignFramesTask(frameSet, frameIndex).execute();
        }
    }




    public void stopStreaming() {
        if (backgroundThread != null) {
            backgroundThread.interrupt();
        }
    }


    public Bitmap frameToBitmap(Frame colorFrame) {

        // Get frame width and height
        int width = 1280;
        int height = 720;

        // Prepare a buffer to hold the frame data
        byte[] data = new byte[width * height * 3]; // RGB format

        // Get the frame data
        colorFrame.getData(data);

        // Create a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Convert RGB data to ARGB format
        int[] pixels = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int r = data[i * 3] & 0xFF;
            int g = data[i * 3 + 1] & 0xFF;
            int b = data[i * 3 + 2] & 0xFF;
            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        int totalBrightness = 0;
        for (int i = 0; i < data.length; i += 3) {
            int r = data[i] & 0xFF;
            int g = data[i + 1] & 0xFF;
            int b = data[i + 2] & 0xFF;
            int brightness = (r + g + b) / 3;
            totalBrightness += brightness;
        }
        int averageBrightness = totalBrightness / (width * height);
        mPixelIntensity = averageBrightness / 255.0f * 2.0f; // Scale to match your Huawei AR Engine example


        return bitmap;
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            // showConnectLabel(false);
        }

        @Override
        public void onDeviceDetach() {
            // showConnectLabel(true);
            stop();
        }
    };

    private synchronized void stop() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mPipeline.stop();
            stopStreaming();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }
    @Override
    protected void updateFrame() {

    }

    @Override
    public String getPersonCount() {
        return mPersonCount;
    }

    public String captureGyroData(MotionFrame motionFrame) {
        double angle1 = 0.0;
        double angle2 =0.0;
        double angle3 =0.0;
        float xAngleDeg=0, yAngleDeg=0, zAngleDeg=0;
        try {

            if (motionFrame != null) {
                Float3 gyroData = motionFrame.getMotionData();

                float x = gyroData.x;
                float y = gyroData.y;
                float z = gyroData.z;


                float norm = (float) Math.sqrt(x * x + y * y + z * z);
                z /= norm;

                // Calculate the vertical angle in degrees
                verticalAngle = Math.toDegrees(Math.acos(z));


                y /= norm;

                horizontalAngle = Math.toDegrees(Math.acos(y));

              //  double xAngleRad = Math.atan2(y, Math.sqrt(x * x + z * z));
              //   xAngleDeg = (float) Math.toDegrees(xAngleRad);
                //   LogFileUtils.logInfo2("ARRealsense1", "this is inside angle -> " + angle);

            /*    double xAngleRad = Math.atan2(y, x);
                double yAngleRad = Math.atan2(z, Math.sqrt(x * x + y * y));
                double zAngleRad = Math.atan2(y, z);

                 xAngleDeg = (float) Math.toDegrees(xAngleRad);
                yAngleDeg = (float) Math.toDegrees(yAngleRad);
                zAngleDeg = (float) Math.toDegrees(zAngleRad);*/

            }
        }catch (Exception e){

        }
        return String.valueOf(angle1);
    }

    PoseDetector poseDetector;
    AccuratePoseDetectorOptions options;

    public void createPose(Bitmap bitmap) {
        Log.i(TAG, "this is inside point 0");
            mPersonCount = "HA";
            Log.i(TAG, "this is inside point 1");
            if (bitmap == null) {
                return;
            }
            String[] ans = new String[2];
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            poseDetector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<Pose>() {
                                @Override
                                public void onSuccess(Pose pose) {
                                    // Task completed successfully
                                    // ...
                                    Log.i(TAG, "this is inside point 3");

                                    String poseCoordinates = null;
                                    float poseTotal = 0.0f;
                                    float poseScore = 0.0f;
                                    List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();

                                    if(allPoseLandmarks==null && allPoseLandmarks.size() <33){
                                        mPersonCount = "NA ";
                                        onProcessAngle();
                                    }
                                    if (allPoseLandmarks != null && allPoseLandmarks.size() > 0) {
                                        poseCoordinates = "";
                                        for (int i = 0; i < allPoseLandmarks.size(); i++) {
                                            poseCoordinates = poseCoordinates + " " + allPoseLandmarks.get(i).getInFrameLikelihood() + "," + allPoseLandmarks.get(i).getPosition().x + "," + allPoseLandmarks.get(i).getPosition().y;
                                            poseTotal = poseTotal + allPoseLandmarks.get(i).getInFrameLikelihood();
                                            if(allPoseLandmarks.get(i).getInFrameLikelihood() < 0.5){
                                                mPersonCount = "NA "+allPoseLandmarks.get(i).getInFrameLikelihood();
                                                break;
                                            }

                                        }

                                        onProcessAngle();

                                        poseScore = (float) (poseTotal / 33.0);
                                        Log.i("ScaneModeActivity", "this is value pf pose " + poseScore + " " + poseCoordinates);


                                        ans[0] = String.valueOf(poseScore);
                                        ans[1] = poseCoordinates;

                                    }
                                    if (ans[0] == null) {
                                        ans[0] = "0.0";
                                    }
                                    Log.i(TAG, "this is inside point 4" + ans[0] + " " + ans[1]);

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                    Log.i(TAG, "this is inside point 5");

                                    mPersonCount = "NA";
                                    onProcessAngle();
                                }
                            });
        }



}
