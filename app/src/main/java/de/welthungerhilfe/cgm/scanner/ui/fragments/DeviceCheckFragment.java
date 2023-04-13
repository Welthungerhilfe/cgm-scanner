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
package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.SizeF;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentDeviceCheckBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.hardware.GPS;
import de.welthungerhilfe.cgm.scanner.hardware.camera.ARCoreCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.AbstractARCamera;
import de.welthungerhilfe.cgm.scanner.hardware.camera.Depthmap;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.NetworkUtils;
import de.welthungerhilfe.cgm.scanner.network.service.ApiService;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.TestView;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DeviceCheckFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, AbstractARCamera.Camera2DataListener {

    public enum IssueType { RGB_DEFECT, TOF_DEFECT, BATTERY_LOW, GPS_FAILED, STORAGE_LOW, BACKEND_ERROR, CHECK_REFUSED };

    private final int CALIBRATIONS_MIN = 30; //measures
    private final int CALIBRATIONS_TIMEOUT = 10000; //miliseconds
    private final float CALIBRATION_TOLERANCE_RGB = 0.04f; //meters
    private final float CALIBRATION_TOLERANCE_TOF = 0.05f; //meters
    private final SizeF CALIBRATION_IMAGE_SIZE = new SizeF(0.15f, 0.15f); //meters

    private final int BATTERY_MIN = 50; //percent
    private final int STORAGE_MIN = 4; //gigabytes

    private ArrayList<SizeF> calibrationsRGB;
    private ArrayList<SizeF> calibrationsToF;
    private long calibrationTimestamp;

    private Context context;
    private AbstractARCamera camera;
    private TutorialData tutorialData;
    private FragmentDeviceCheckBinding fragmentDeviceCheckBinding;
    String TAG = DeviceCheckFragment.class.getSimpleName();

    public static DeviceCheckFragment newInstance(TutorialData tutorialData) {

        Bundle args = new Bundle();
        args.putParcelable("TutorialData",tutorialData);

        DeviceCheckFragment fragment = new DeviceCheckFragment();
        fragment.calibrationsRGB = new ArrayList<>();
        fragment.calibrationsToF = new ArrayList<>();
        fragment.calibrationTimestamp = Long.MAX_VALUE;
        fragment.setArguments(args);
        return fragment;
    }


    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentDeviceCheckBinding = DataBindingUtil.inflate(inflater,R.layout.fragment_device_check, container, false);
        tutorialData = getArguments().getParcelable("TutorialData");
        return fragmentDeviceCheckBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        fragmentDeviceCheckBinding.tvTitle.setText(tutorialData.getTitle());
        if (tutorialData.getImage() == 0) {
            fragmentDeviceCheckBinding.ivTitle.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        } else {
            fragmentDeviceCheckBinding.ivTitle.setImageResource(tutorialData.getImage());
        }
        fragmentDeviceCheckBinding.instruction.setText(tutorialData.getInstruction1());
        fragmentDeviceCheckBinding.guide.setText(tutorialData.getInstruction2());
        if (tutorialData.getInstruction2().length() == 0) {
            fragmentDeviceCheckBinding.guide.setVisibility(View.GONE);
        } else {
            fragmentDeviceCheckBinding.guide.setVisibility(View.VISIBLE);
        }
        fragmentDeviceCheckBinding.guide.setOnCheckedChangeListener(this);
        fragmentDeviceCheckBinding.btnNext.setOnClickListener(this);
        fragmentDeviceCheckBinding.stepView.go(tutorialData.getPosition(),true);

        camera = null;
        switch (tutorialData.getPosition()) {
            case 1:
                setCameraTestLayout();
                startCamera();
                break;
            case 2:
                setDeviceTestLayout();
                new Thread(batteryCheck).start();
                new Thread(deviceStorageCheck).start();
                new Thread(gpsCheck).start();
                new Thread(internetConnectionCheck).start();
                break;
            case 3:
                setSummaryLayout();
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.removeListener(this);
            camera.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (camera != null) {
            if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(activity), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, BaseActivity.PERMISSION_CAMERA);
            } else {
                camera.onResume();
                camera.addListener(this);
            }
        }
        if (tutorialData.getPosition() == 2) {
            new Thread(gpsCheck).start();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateNextButton();
    }

    @Override
    public void onClick(View v) {
        if (canContinue()) {
            int steps = 1;

            //skip issue summary screen when there are no issues
            DeviceCheckActivity activity = (DeviceCheckActivity)context;
            if ((tutorialData.getPosition() == 2) && (!activity.hasIssues())) {
                steps = 2;
            }

            //next screen
            for (int i = 0; i < steps; i++) {
                activity.gotoNext();
            }
        }
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex) {
        //dummy
    }

    @Override
    public void onDepthDataReceived(Depthmap depthmap, int frameIndex) {

        if (frameIndex % 10 == 0) {
            SizeF calibrationRGB = camera.getCalibrationImageSize(false);
            SizeF calibrationToF = camera.getCalibrationImageSize(true);
            Log.i("Tag","this is value of "+camera.getCameraCalibration());
            LogFileUtils.logInfo(TAG,"this is device check fragment onDepthDataReceived "+calibrationRGB+" "+calibrationsToF);
            if (calibrationRGB != null) {
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    updateCalibration(calibrationRGB, calibrationsRGB);
                    updateCalibrationResult(calibrationsRGB, fragmentDeviceCheckBinding.test1, CALIBRATION_TOLERANCE_RGB, IssueType.RGB_DEFECT);
                    if (calibrationToF != null) {
                        updateCalibration(calibrationToF, calibrationsToF);
                        updateCalibrationResult(calibrationsToF, fragmentDeviceCheckBinding.test2, CALIBRATION_TOLERANCE_TOF, IssueType.TOF_DEFECT);
                    }
                });
            }

            if ((System.currentTimeMillis() - calibrationTimestamp >= CALIBRATIONS_TIMEOUT)) {
                updateCalibrationResult(calibrationsRGB, fragmentDeviceCheckBinding.test1, CALIBRATION_TOLERANCE_RGB, IssueType.RGB_DEFECT);
                updateCalibrationResult(calibrationsToF, fragmentDeviceCheckBinding.test2, CALIBRATION_TOLERANCE_TOF, IssueType.TOF_DEFECT);
                calibrationTimestamp = Long.MAX_VALUE;
            }
        }
    }

    public void updateIssues(ArrayList<IssueType> issues) {
        StringBuilder text = new StringBuilder();
        for (IssueType issue : issues) {
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(getIssueDescription(issue));
        }
        fragmentDeviceCheckBinding.instruction.setText(text.toString());
    }

    private boolean canContinue() {
        if (tutorialData.getPosition() == 1) {
            if (fragmentDeviceCheckBinding.test1.isFinished()) {
                if (fragmentDeviceCheckBinding.test2.isFinished()) {
                    return true;
                }
            }
            return false;
        }
        if (tutorialData.getPosition() == 2) {
            if (fragmentDeviceCheckBinding.test1.isFinished()) {
                if (fragmentDeviceCheckBinding.test2.isFinished()) {
                    if (fragmentDeviceCheckBinding.test3.isFinished()) {
                        if (fragmentDeviceCheckBinding.test4.isFinished()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return fragmentDeviceCheckBinding.guide.isChecked() || (fragmentDeviceCheckBinding.guide.getVisibility() == View.GONE);
    }

    private String getIssueDescription(IssueType issue) {
        switch (issue) {
            case RGB_DEFECT: return getString(R.string.device_check4_rgb);
            case TOF_DEFECT: return getString(R.string.device_check4_tof);
            case BATTERY_LOW: return getString(R.string.device_check4_battery);
            case GPS_FAILED: return getString(R.string.device_check4_gps);
            case STORAGE_LOW: return getString(R.string.device_check4_storage);
            case BACKEND_ERROR: return getString(R.string.device_check4_backend);
        }
        return issue.toString();
    }

    private void setCameraTestLayout() {
        fragmentDeviceCheckBinding.test1.setTitle(getString(R.string.device_check21));
        fragmentDeviceCheckBinding.test2.setTitle(getString(R.string.device_check22));
        fragmentDeviceCheckBinding.test1.setResult(getString(R.string.device_check_detecting_image));
        fragmentDeviceCheckBinding.test2.setResult(getString(R.string.device_check_detecting_image));
        fragmentDeviceCheckBinding.test1.setState(TestView.TestState.INITIALIZE);
        fragmentDeviceCheckBinding.test2.setState(TestView.TestState.INITIALIZE);
        fragmentDeviceCheckBinding.test1.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.test2.setVisibility(View.VISIBLE);

        fragmentDeviceCheckBinding.scrollView.setVisibility(View.GONE);
        fragmentDeviceCheckBinding.colorCameraPreview.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.depthCameraPreview.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.surfaceview.setVisibility(View.VISIBLE);
    }

    private void setDeviceTestLayout() {
        fragmentDeviceCheckBinding.scrollView.setVisibility(View.GONE);
        fragmentDeviceCheckBinding.test1.setTitle(getString(R.string.device_check31));
        fragmentDeviceCheckBinding.test2.setTitle(getString(R.string.device_check32));
        fragmentDeviceCheckBinding.test3.setTitle(getString(R.string.device_check33));
        fragmentDeviceCheckBinding.test4.setTitle(getString(R.string.device_check34));
        fragmentDeviceCheckBinding.test1.setResult(getString(R.string.device_check_testing));
        fragmentDeviceCheckBinding.test2.setResult(getString(R.string.device_check_testing));
        fragmentDeviceCheckBinding.test3.setResult(getString(R.string.device_check_testing));
        fragmentDeviceCheckBinding.test4.setResult(getString(R.string.device_check_testing));
        fragmentDeviceCheckBinding.test1.setState(TestView.TestState.TESTING);
        fragmentDeviceCheckBinding.test2.setState(TestView.TestState.TESTING);
        fragmentDeviceCheckBinding.test3.setState(TestView.TestState.TESTING);
        fragmentDeviceCheckBinding.test4.setState(TestView.TestState.TESTING);
        fragmentDeviceCheckBinding.test1.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.test2.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.test3.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.test4.setVisibility(View.VISIBLE);
    }

    private void setSummaryLayout() {
        fragmentDeviceCheckBinding.btnNext.setText(getString(R.string.done).toUpperCase());
        fragmentDeviceCheckBinding.btnContactSupport.setVisibility(View.VISIBLE);
        fragmentDeviceCheckBinding.btnContactSupport.setOnClickListener(view -> {
            StringBuilder footer = new StringBuilder();
            DeviceCheckActivity activity = (DeviceCheckActivity)context;
            for (IssueType issue : activity.getIssues()) {
                if (footer.length() > 0) {
                    footer.append(";");
                }
                footer.append(issue.toString());
            }

            ContactSupportDialog.show(activity, null, "Issues: " + footer.toString());
        });
    }

    private void startCamera() {
        AbstractARCamera.DepthPreviewMode depthMode = AbstractARCamera.DepthPreviewMode.CALIBRATION;
        AbstractARCamera.PreviewSize previewSize = AbstractARCamera.PreviewSize.SMALL;
        /*if (AREngineCamera.shouldUseAREngine()) {
            camera = new AREngineCamera(getActivity(), depthMode, previewSize);
        } else {*/
            camera = new ARCoreCamera(getActivity(), depthMode, previewSize);
     //   }
        if (camera != null) {
            camera.onCreate(fragmentDeviceCheckBinding.colorCameraPreview,
                    fragmentDeviceCheckBinding.depthCameraPreview,
                    fragmentDeviceCheckBinding.surfaceview);
        }
    }

    private void updateCalibration(SizeF calibration, ArrayList<SizeF> calibrations) {
        if (calibrations.size() < CALIBRATIONS_MIN) {
            calibrations.add(calibration);
        }
    }

    private void updateCalibrationResult(ArrayList<SizeF> calibrations, TestView result, float tolerance, IssueType onFail) {
        LogFileUtils.logInfo(TAG,"this is device check fragment updateCalibrationResult "+calibrations+" "+tolerance);

        if (calibrations.size() == 1) {
            result.setResult(getString(R.string.device_check_testing));
            result.setState(TestView.TestState.TESTING);

            Display d = getActivity().getWindowManager().getDefaultDisplay();
            fragmentDeviceCheckBinding.arHandImage.setVisibility(View.VISIBLE);
            fragmentDeviceCheckBinding.arHandImage.getAnimation().setOffset(d.getWidth() / 2, d.getHeight() / 2);
            calibrationTimestamp = System.currentTimeMillis();
        } else if ((calibrations.size() == CALIBRATIONS_MIN) || (System.currentTimeMillis() - calibrationTimestamp >= CALIBRATIONS_TIMEOUT)) {
            boolean valid = false;

            for (int i = 2; i < calibrations.size(); i++) {
                SizeF calibration = calibrations.get(i);
                LogFileUtils.logInfo(TAG,"this is device check fragment height & weight "+calibration.getHeight()+" "+calibration.getWidth()+" "+tolerance);

                float dx = Math.abs(calibration.getWidth() - CALIBRATION_IMAGE_SIZE.getWidth());
                float dy = Math.abs(calibration.getHeight() - CALIBRATION_IMAGE_SIZE.getHeight());
                float avg = (dx + dy) / 2.0f;
                if (avg <= tolerance) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                result.setResult(getString(R.string.ok));
                result.setState(TestView.TestState.SUCCESS);
            } else {
                result.setResult(getString(R.string.device_check_failed));
                result.setState(TestView.TestState.ERROR);
                addIssue(onFail);
            }
            if (canContinue()) {
                fragmentDeviceCheckBinding.arHandImage.getAnimation().setOffset(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
            updateNextButton();
        }
    }

    private void updateNextButton() {
        if (canContinue()) {
            if ((tutorialData.getPosition() == 1) || (tutorialData.getPosition() == 2)) {
                fragmentDeviceCheckBinding.tvTitle.setText(R.string.device_check_step_finished);
            }
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }

    private void addIssue(IssueType issue) {
        DeviceCheckActivity activity = (DeviceCheckActivity) getActivity();
        if (activity != null) {
            activity.addIssue(issue);
        }
    }

    private final Runnable batteryCheck = new Runnable() {
        @Override
        public void run() {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, iFilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
            float batteryPct = 100 * level / (float) scale;

            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                if (batteryPct > BATTERY_MIN) {
                    setTestResult(fragmentDeviceCheckBinding.test1, R.string.ok, TestView.TestState.SUCCESS);
                } else {
                    setTestResult(fragmentDeviceCheckBinding.test1, R.string.device_check_too_low_energy, TestView.TestState.ERROR);
                    addIssue(IssueType.BATTERY_LOW);
                }
                updateNextButton();
            });
        }
    };

    private final Runnable deviceStorageCheck = new Runnable() {
        @Override
        public void run() {
            File path = Environment.getDataDirectory();
            StatFs stats = new StatFs(path.getAbsolutePath());
            long bytes = stats.getAvailableBlocksLong() * stats.getBlockSizeLong();
            int gigabytes = (int) (bytes / 1024 / 1024 / 1024);

            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                if (gigabytes > STORAGE_MIN) {
                    setTestResult(fragmentDeviceCheckBinding.test2, R.string.ok, TestView.TestState.SUCCESS);
                } else {
                    setTestResult(fragmentDeviceCheckBinding.test2, R.string.device_check_failed, TestView.TestState.ERROR);
                    addIssue(IssueType.STORAGE_LOW);
                }
                updateNextButton();
            });
        }
    };

    private final Runnable gpsCheck = new Runnable() {
        @Override
        public void run() {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                Activity activity = getActivity();
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, BaseActivity.PERMISSION_LOCATION);
                } else {
                    if (GPS.getLastKnownLocation(getContext()) != null) {
                        setTestResult(fragmentDeviceCheckBinding.test3, R.string.ok, TestView.TestState.SUCCESS);
                    } else {
                        setTestResult(fragmentDeviceCheckBinding.test3, R.string.device_check_failed, TestView.TestState.ERROR);
                        addIssue(IssueType.GPS_FAILED);
                    }
                }
                updateNextButton();
            });
        }
    };

    private final Runnable internetConnectionCheck = new Runnable() {
        @Override
        public void run() {
            if (!BuildConfig.DEBUG) {
                try {
                    SessionManager session = new SessionManager(Objects.requireNonNull(getActivity()));
                    SyncingWorkManager.provideRetrofit().create(ApiService.class).test(session.getAuthTokenWithBearer()).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<String>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onNext(@NonNull String response) {
                                    setTestResult(fragmentDeviceCheckBinding.test4, R.string.ok, TestView.TestState.SUCCESS);
                                    updateNextButton();
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    if (NetworkUtils.isExpiredToken(e.getMessage())) {
                                        setTestResult(fragmentDeviceCheckBinding.test4, R.string.ok, TestView.TestState.SUCCESS);
                                        updateNextButton();
                                    } else {
                                        String message = e.getMessage();
                                        if (message.startsWith("HTTP 2") || message.startsWith("HTTP 403")) {
                                            setTestResult(fragmentDeviceCheckBinding.test4, R.string.ok, TestView.TestState.SUCCESS);
                                        } else {
                                            setTestResult(fragmentDeviceCheckBinding.test4, R.string.device_check_failed, TestView.TestState.ERROR);
                                            addIssue(IssueType.BACKEND_ERROR);
                                        }
                                        updateNextButton();
                                    }
                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                } catch (Exception e) {
                    LogFileUtils.logException(e);
                    setTestResult(fragmentDeviceCheckBinding.test4, R.string.device_check_failed, TestView.TestState.ERROR);
                    addIssue(IssueType.BACKEND_ERROR);
                    updateNextButton();
                }
            } else {
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    setTestResult(fragmentDeviceCheckBinding.test4, R.string.device_check_failed, TestView.TestState.ERROR);
                    addIssue(IssueType.BACKEND_ERROR);
                    updateNextButton();
                });
            }
        }
    };

    private void setTestResult(TestView test, int text, TestView.TestState state) {
        try {
            test.setResult(getString(text));
            test.setState(state);
        } catch (Exception e) {
            //expected if the result is updated after leaving the screen
        }
    }
}
