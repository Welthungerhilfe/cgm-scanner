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

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.SizeF;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Objects;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.camera.ARCoreCamera;
import de.welthungerhilfe.cgm.scanner.camera.AREngineCamera;
import de.welthungerhilfe.cgm.scanner.camera.AbstractARCamera;
import de.welthungerhilfe.cgm.scanner.camera.TangoUtils;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentDeviceCheckBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity;
import de.welthungerhilfe.cgm.scanner.ui.views.TestView;

public class DeviceCheckFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, AbstractARCamera.Camera2DataListener {

    private final int CALIBRATIONS_MIN = 10;
    private final float CALIBRATION_TOLERANCE_RGB = 0.03f;
    private final float CALIBRATION_TOLERANCE_TOF = 0.015f;
    private final SizeF CALIBRATION_IMAGE_SIZE = new SizeF(0.35f, 0.35f);

    private ArrayList<SizeF> calibrationsRGB;
    private ArrayList<SizeF> calibrationsToF;

    private Context context;
    private AbstractARCamera camera;
    private TutorialData tutorialData;
    private FragmentDeviceCheckBinding fragmentDeviceCheckBinding;

    public static DeviceCheckFragment newInstance(TutorialData tutorialData) {

        Bundle args = new Bundle();
        args.putParcelable("TutorialData",tutorialData);

        DeviceCheckFragment fragment = new DeviceCheckFragment();
        fragment.calibrationsRGB = new ArrayList<>();
        fragment.calibrationsToF = new ArrayList<>();
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
        setCameraVisibility(View.GONE);
        switch (tutorialData.getPosition()) {
            case 1:
                fragmentDeviceCheckBinding.test1.setTitle(getString(R.string.device_check21));
                fragmentDeviceCheckBinding.test2.setTitle(getString(R.string.device_check22));
                fragmentDeviceCheckBinding.test1.setResult(getString(R.string.device_check_detecting_image));
                fragmentDeviceCheckBinding.test2.setResult(getString(R.string.device_check_detecting_image));
                fragmentDeviceCheckBinding.test1.setState(TestView.TestState.INITIALIZE);
                fragmentDeviceCheckBinding.test2.setState(TestView.TestState.INITIALIZE);
                fragmentDeviceCheckBinding.test1.setVisibility(View.VISIBLE);
                fragmentDeviceCheckBinding.test2.setVisibility(View.VISIBLE);

                setCameraVisibility(View.VISIBLE);
                AbstractARCamera.DepthPreviewMode depthMode = AbstractARCamera.DepthPreviewMode.CALIBRATION;
                AbstractARCamera.PreviewSize previewSize = AbstractARCamera.PreviewSize.SMALL;
                if (TangoUtils.isTangoSupported()) {
                    camera = null;
                } else if (AREngineCamera.shouldUseAREngine()) {
                    camera = new AREngineCamera(getActivity(), depthMode, previewSize);
                } else {
                    camera = new ARCoreCamera(getActivity(), depthMode, previewSize);
                }
                if (camera != null) {
                    camera.onCreate(fragmentDeviceCheckBinding.colorCameraPreview,
                                    fragmentDeviceCheckBinding.depthCameraPreview,
                                    fragmentDeviceCheckBinding.surfaceview);
                }
                break;
            case 2:
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
                break;
            case 3:
                fragmentDeviceCheckBinding.btnNext.setText(getString(R.string.done).toUpperCase());
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
        if (camera != null) {
            camera.onResume();
            camera.addListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateNextButton();
    }

    @Override
    public void onClick(View v) {
        if (canContinue()) {
            ((DeviceCheckActivity)context).gotoNext();
        }
    }

    @Override
    public void onColorDataReceived(Bitmap bitmap, int frameIndex) {
        //dummy
    }

    @Override
    public void onDepthDataReceived(Image image, float[] position, float[] rotation, int frameIndex) {

        if (frameIndex % 10 == 0) {
            SizeF calibrationRGB = camera.getCalibrationImageSize(false);
            SizeF calibrationToF = camera.getCalibrationImageSize(true);
            if (calibrationRGB != null) {
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    updateCalibration(calibrationRGB, calibrationsRGB);
                    updateCalibrationResult(calibrationsRGB, fragmentDeviceCheckBinding.test1, CALIBRATION_TOLERANCE_RGB);
                    if (calibrationToF != null) {
                        updateCalibration(calibrationToF, calibrationsToF);
                        updateCalibrationResult(calibrationsToF, fragmentDeviceCheckBinding.test2, CALIBRATION_TOLERANCE_TOF);
                    }
                });
            }
        }
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

    private void setCameraVisibility(int visibility) {
        if (!TangoUtils.isTangoSupported()) {
            fragmentDeviceCheckBinding.cameraPreviewBackground.setVisibility(visibility);
            fragmentDeviceCheckBinding.colorCameraPreview.setVisibility(visibility);
            fragmentDeviceCheckBinding.depthCameraPreview.setVisibility(visibility);
            fragmentDeviceCheckBinding.surfaceview.setVisibility(visibility);
        }
    }

    private void updateCalibration(SizeF calibration, ArrayList<SizeF> calibrations) {
        boolean valid = true;
        for (SizeF s : calibrations) {
            float dx = Math.abs(calibration.getWidth() - s.getWidth());
            float dy = Math.abs(calibration.getHeight() - s.getHeight());
            if (dx + dy < 0.00001f) {
                valid = false;
                break;
            }
        }
        if (valid && (calibrations.size() < CALIBRATIONS_MIN)) {
            calibrations.add(calibration);
        }
    }

    private void updateCalibrationResult(ArrayList<SizeF> calibrations, TestView result, float tolerance) {
        calibrations.sort((a, b) -> {
            float dx = Math.abs(a.getWidth() - b.getWidth());
            float dy = Math.abs(a.getHeight() - b.getHeight());
            return (int)(dx + dy * 1000);
        });

        SizeF median = calibrations.get(calibrations.size() / 2);
        if (calibrations.size() == 1) {
            result.setResult(getString(R.string.device_check_testing));
            result.setState(TestView.TestState.TESTING);

            Display d = getActivity().getWindowManager().getDefaultDisplay();
            fragmentDeviceCheckBinding.arHandImage.setVisibility(View.VISIBLE);
            fragmentDeviceCheckBinding.arHandImage.getAnimation().setOffset(d.getWidth() / 2, d.getHeight() / 2);
        } else if (calibrations.size() == CALIBRATIONS_MIN) {
            float dx = Math.abs(median.getWidth() - CALIBRATION_IMAGE_SIZE.getWidth());
            float dy = Math.abs(median.getHeight() - CALIBRATION_IMAGE_SIZE.getHeight());
            float avg = (dx + dy) / 2.0f;
            if (avg > tolerance) {
                result.setResult(getString(R.string.device_check_failed));
                result.setState(TestView.TestState.ERROR);
            } else {
                result.setResult(getString(R.string.ok));
                result.setState(TestView.TestState.SUCCESS);
            }
            if (canContinue()) {
                fragmentDeviceCheckBinding.arHandImage.getAnimation().setOffset(Integer.MAX_VALUE, Integer.MAX_VALUE);
                fragmentDeviceCheckBinding.tvTitle.setText(R.string.device_check23);
            }
            updateNextButton();
        }
    }

    private void updateNextButton() {
        if (canContinue()) {
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }
}
