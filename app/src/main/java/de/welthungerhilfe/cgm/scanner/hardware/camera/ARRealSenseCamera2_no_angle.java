package de.welthungerhilfe.cgm.scanner.hardware.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Float3;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
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
import com.intel.realsense.librealsense.HoleFillingFilter;
import com.intel.realsense.librealsense.Intrinsic;
import com.intel.realsense.librealsense.MotionFrame;
import com.intel.realsense.librealsense.Option;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoStreamProfile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;

public class ARRealSenseCamera2_no_angle extends AbstractIntelARCamera  {


    private static final String TAG = "librs capture example";
    private boolean mIsStreaming = false;
    private Pipeline mPipeline;
    private RsContext mRsContext;
    private Align mAlign;

    // New variables for position and rotation tracking
    private float[] position = new float[3]; // x, y, z in meters
    private float[] rotation = new float[4]; // quaternion (x, y, z, w)
    private float[] velocity = new float[3]; // For position integration
    private long lastTimestamp = 0;

    public ARRealSenseCamera2_no_angle(Activity activity, AbstractIntelARCamera.DepthPreviewMode depthMode, AbstractIntelARCamera.PreviewSize previewSize) {
        super(activity, depthMode, previewSize);
        // Initialize position and rotation to match AR Engine's starting state
        position[0] = 0; position[1] = 0; position[2] = 0;
        rotation[0] = 0; rotation[1] = 0; rotation[2] = 0; rotation[3] = 1; // Identity quaternion
    }

    boolean isBackgrounThreadActive = false;

    @Override
    protected void closeCamera() {
        if (!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            stopStreaming();
            mPipeline.stop();
            AbstractIntelARCamera.getRsContext().removeDevicesChangedCallback();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
            mPipeline = null;
        }
    }

    @Override
    protected void openCamera() {
        mRsContext = AbstractIntelARCamera.getRsContext();
        mRsContext.setDevicesChangedCallback(mListener);
        mPipeline = new Pipeline(mRsContext);

        try (DeviceList dl = mRsContext.queryDevices()) {
            if (dl.getDeviceCount() > 0) {
                start();
            }
        }
    }

    private synchronized void start() {
        if (mIsStreaming)
            return;
        try {
            mIsStreaming = true;
            configAndStart();
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    HoleFillingFilter holeFillingFilter;

    private void configAndStart() throws Exception {
        try (Config config = new Config()) {
            options = new AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build();
            poseDetector = PoseDetection.getClient(options);

            config.enableStream(StreamType.DEPTH, 1280, 720);
            config.enableStream(StreamType.COLOR, 1280, 720);
           /* config.enableStream(StreamType.ACCEL, StreamFormat.MOTION_XYZ32F);
            config.enableStream(StreamType.GYRO, StreamFormat.MOTION_XYZ32F);*/ // Enable gyroscope stream

            holeFillingFilter = new HoleFillingFilter();
            holeFillingFilter.setValue(Option.HOLES_FILL, 2);

            mPipeline.start(config);
            mAlign = new Align(StreamType.COLOR);
            startStreaming();
        } catch (Exception e) {
            Log.d(TAG, "configAndStart failed: " + e.getMessage());
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
                            if (!intrisicGenerated) {
                                VideoStreamProfile videoStreamProfile = frames.getProfile().as(Extension.VIDEO_PROFILE);
                                Intrinsic intrinsics = videoStreamProfile.getIntrinsic();
                                LogFileUtils.logInfoOffline("Width: ", " " + intrinsics.getWidth());
                                LogFileUtils.logInfoOffline("Height: ", " " + intrinsics.getHeight());
                                LogFileUtils.logInfoOffline("Principal Point X (ppx): ", " " + intrinsics.getmPpx());
                                LogFileUtils.logInfoOffline("Principal Point Y (ppy): ", " " + intrinsics.getmPpy());
                                LogFileUtils.logInfoOffline("Focal Length X (fx): ", " " + intrinsics.getmFx());
                                LogFileUtils.logInfoOffline("Focal Length Y (fy): ", " " + intrinsics.getmFy());
                                LogFileUtils.logInfoOffline("Distortion Model: ", " " + intrinsics.getModel());

                                mColorCameraIntrinsic[0] = intrinsics.getmFy() / (float) intrinsics.getHeight();
                                mColorCameraIntrinsic[1] = intrinsics.getmFx() / (float) intrinsics.getWidth();
                                mColorCameraIntrinsic[2] = intrinsics.getmPpy() / (float) intrinsics.getHeight();
                                mColorCameraIntrinsic[3] = intrinsics.getmPpx() / (float) intrinsics.getWidth();
                                intrisicGenerated = true;
                            }

                            // Process accelerometer and gyroscope for position and rotation
                           /* try (Frame accelFrame = frames.first(StreamType.ACCEL, StreamFormat.MOTION_XYZ32F);
                                 Frame gyroFrame = frames.first(StreamType.GYRO, StreamFormat.MOTION_XYZ32F)) {
                                if (accelFrame != null && gyroFrame != null) {
                                    MotionFrame accel = accelFrame.as(Extension.MOTION_FRAME);
                                    MotionFrame gyro = gyroFrame.as(Extension.MOTION_FRAME);


                                    angle = captureGyroData(accel);

                                    Float3 accelData = accel.getMotionData();
                                    Float3 gyroData = gyro.getMotionData();

                                    // Align coordinates with AR Engine: Y up, Z forward
                                    float ax = accelData.x;         // X unchanged
                                    float ay = -accelData.z;        // Y = -Z
                                    float az = accelData.y;         // Z = Y
                                    float gx = gyroData.x;          // X unchanged
                                    float gy = -gyroData.z;         // Y = -Z
                                    float gz = gyroData.y;          // Z = Y

                                    // Calculate delta time
                                    long timestamp = (long) gyro.getTimestamp();
                                    float deltaTime = (lastTimestamp == 0) ? 0 : (timestamp - lastTimestamp) / 1000.0f;
                                    lastTimestamp = timestamp;

                                    // Update rotation and position
                                    updateRotation(gx, gy, gz, deltaTime);
                                    updatePosition(ax, ay, az, deltaTime);

                                    // Log position and rotation (optional, for debugging)

                                }
                            }
*/
                            // Existing gyro data capture (unchanged)
                           /* try (Frame accelFrame = frames.first(StreamType.ACCEL, StreamFormat.MOTION_XYZ32F)) {
                                MotionFrame motionFrame = accelFrame.as(Extension.MOTION_FRAME);
                                angle = captureGyroData(motionFrame);
                            } catch (Exception e) {
                                Log.d(TAG, "Accel frame error: " + e.getMessage());
                            }*/

                            // Process color frame (unchanged)
                            try (Frame f1 = frames.first(StreamType.COLOR)) {
                                colorFrame1 = f1;
                                bitmap1 = frameToBitmap(colorFrame1);
                                createPose(bitmap1);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        onProcessColorData(bitmap1, null, 0,0);
                                        onProcessAngle(position,rotation);
                                    }
                                });
                            }

                            // Process depth frame (unchanged)
                            try (Frame depth = frames.first(StreamType.DEPTH)) {
                                DepthFrame depthFrame1 = depth.as(Extension.DEPTH_FRAME);

                                mTargetDistance = depthFrame1.getDistance(depthFrame1.getWidth() / 2, depthFrame1.getHeight() / 2);

                            }

                            if (mFrameIndex % AppConstants.SCAN_FRAMESKIP_REALSENSE == 0 && !isBackgrounThreadActive) {
                                saveAlignFrames1(frames, mFrameIndex);
                            }

                        } catch (Exception e) {
                            Log.d(TAG, "Frame processing error: " + e.getMessage());
                        }

                        try {
                            Thread.sleep(50); // Adjust delay as needed
                        } catch (InterruptedException e) {
                            break;
                        }
                        mFrameIndex++;
                    } catch (Exception e) {
                        Log.d(TAG, "Streaming loop error: " + e.getMessage());
                    }
                }
            }
        });
        backgroundThread.start();
    }

    // Helper methods for position and rotation computation
    private void updateRotation(float gx, float gy, float gz, float dt) {
        float magnitude = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (magnitude > 0.0001f) {
            float sinAngle = (float) Math.sin(magnitude * dt / 2);
            float cosAngle = (float) Math.cos(magnitude * dt / 2);
            float qx = (gx / magnitude) * sinAngle;
            float qy = (gy / magnitude) * sinAngle;
            float qz = (gz / magnitude) * sinAngle;
            float qw = cosAngle;

            float[] newRot = quaternionMultiply(rotation, new float[]{qx, qy, qz, qw});
            System.arraycopy(newRot, 0, rotation, 0, 4);
        }
    }

    private float[] quaternionMultiply(float[] q1, float[] q2) {
        float w = q1[3] * q2[3] - q1[0] * q2[0] - q1[1] * q2[1] - q1[2] * q2[2];
        float x = q1[3] * q2[0] + q1[0] * q2[3] + q1[1] * q2[2] - q1[2] * q2[1];
        float y = q1[3] * q2[1] - q1[0] * q2[2] + q1[1] * q2[3] + q1[2] * q2[0];
        float z = q1[3] * q2[2] + q1[0] * q2[1] - q1[1] * q2[0] + q1[2] * q2[3];
        return new float[]{x, y, z, w};
    }

    private void updatePosition(float ax, float ay, float az, float dt) {
        for (int i = 0; i < 3; i++) {
            velocity[i] += (i == 0 ? ax : i == 1 ? ay : az) * dt;
            position[i] += velocity[i] * dt;
        }
    }

    // Existing SaveAlignFramesTask class (unchanged)
    private class SaveAlignFramesTask extends AsyncTask<Void, Void, Void> {
        private FrameSet frameSet;
        private int frameIndex;
        private DepthFrame depthFrameSave, depthFrameHoleFilling;
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

            //  FrameSet filteredDepthSet = holeFillingFilter.process(frameSet);

            try (FrameSet alignedFrames = mAlign.process(frameSet)) {


                try (Frame f = alignedFrames.first(StreamType.DEPTH)) {
                    depthFrameSave = f.as(Extension.DEPTH_FRAME);
                    //depthFrameSave = (DepthFrame) holeFillingFilter.process(depthFrameHoleFilling);
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
                //byteArray = fillZeroDepths(byteArray, width, height, stride);


            } catch (Exception e) {
                LogFileUtils.logInfoOffline(TAG, "SaveAlignFramesTask error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isBackgrounThreadActive = false;
            onProcessColorData(bitmap1, bitmapSave, frameIndex,0);
            onProcessDepthData(null, depthFrameSave, height, width, byteArray, frameIndex);
            frameSet.close();
        }
    }

 /*   public byte[] fillZeroDepths(byte[] byteArray, int width, int height, int stride) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);

        short[][] depth = new short[height][width];
        // Convert byte array to 2D short array
        for (int y = 0; y < height; y++) {
            int rowOffset = y * stride;
            for (int x = 0; x < width; x++) {
                depth[y][x] = buffer.getShort(rowOffset + x * 2);
            }
        }

        // Track visited zero-pixels
        boolean[][] visited = new boolean[height][width];

        // Process depth data to fill zeros
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (depth[y][x] == 0 && !visited[y][x]) {
                    fillConnectedZeros(depth, visited, x, y, width, height);
                }
            }
        }

        // Convert updated depth back to byte array
        buffer.rewind(); // Go back to beginning of buffer
        for (int y = 0; y < height; y++) {
            int rowOffset = y * stride;
            for (int x = 0; x < width; x++) {
                buffer.putShort(rowOffset + x * 2, depth[y][x]);
            }
        }

        return byteArray;
    }
    void fillConnectedZeros(short[][] depth, boolean[][] visited, int x, int y, int width, int height) {
        List<int[]> toFill = new ArrayList<>();
        int maxRadius = 5;

        // Collect connected zeros (for a patch)
        for (int r = 1; r <= maxRadius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        if (!visited[ny][nx]) {
                            if (depth[ny][nx] == 0) {
                                toFill.add(new int[]{nx, ny});
                                visited[ny][nx] = true;
                            } else {
                                // Found a non-zero value, use it to fill all previous zeros
                                short replacement = depth[ny][nx];
                                for (int[] pos : toFill) {
                                    depth[pos[1]][pos[0]] = replacement;
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }

        // No non-zero neighbor found, leave all as zero
    }*/

    void saveAlignFrames1(FrameSet frameSet, int frameIndex) {
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
        int width = 1280;
        int height = 720;
        byte[] data = new byte[width * height * 3];
        colorFrame.getData(data);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
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
        mPixelIntensity = averageBrightness / 255.0f * 2.0f;
        return bitmap;
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
        }

        @Override
        public void onDeviceDetach() {
            onSensorDisconnect();
        }
    };

    private synchronized void stop() {
        if (!mIsStreaming)
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
        float xAngleDeg = 0, yAngleDeg = 0, zAngleDeg = 0;
        try {
            if (motionFrame != null) {
                Float3 gyroData = motionFrame.getMotionData();
                float x = gyroData.x;
                float y = gyroData.y;
                float z = gyroData.z;

                float norm = (float) Math.sqrt(x * x + y * y + z * z);
                z /= norm;
                verticalAngle = Math.toDegrees(Math.acos(z));
                y /= norm;
                horizontalAngle = Math.toDegrees(Math.acos(y));
            }
        } catch (Exception e) {
            Log.d(TAG, "captureGyroData error: " + e.getMessage());
        }
        return String.valueOf(angle1);
    }

    PoseDetector poseDetector;
    AccuratePoseDetectorOptions options;
    String datax = "", datay = "";

    public void createPose(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
                    if (landmarks.isEmpty()) {
                        mPersonCount = "NA";
                        return;
                    }
                    PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                    PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                    PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
                    PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);

                    if (leftShoulder != null && rightShoulder != null) {
                        float humanCenterX = (leftShoulder.getPosition().x + rightShoulder.getPosition().x + leftKnee.getPosition().x + rightKnee.getPosition().x) / 4;
                        float humanCenterY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y + leftKnee.getPosition().y + rightKnee.getPosition().y) / 4;
                        float bitmapCenterX = bitmap.getWidth() / 2;
                        float bitmapCenterY = bitmap.getHeight() / 2;
                        float tolerance = 300.0f;
                        boolean isCentered = Math.abs(humanCenterX - bitmapCenterX) <= 145 &&
                                Math.abs(humanCenterY - bitmapCenterY) <= 185;
                        datax = "";
                        datay = "";
                        if (isCentered) {
                            onChildVisible(true);
                        } else {
                            if (Math.abs(humanCenterX - bitmapCenterX) > 145) {
                                datax = "x:- " + Math.abs(humanCenterX - bitmapCenterX);
                            }
                            if (Math.abs(humanCenterY - bitmapCenterY) > 185) {
                                datay = "Y:- " + Math.abs(humanCenterY - bitmapCenterY);
                            }
                            onChildVisible(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    mPersonCount = "NA";
                });
    }

    // Optional: Getter methods to access position and rotation
    public float[] getPosition() {
        return position.clone();
    }

    public float[] getRotation() {
        return rotation.clone();
    }
}
