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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentDeviceCheckBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity;

public class DeviceCheckFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private Context context;


    TutorialData tutorialData;
    FragmentDeviceCheckBinding fragmentDeviceCheckBinding;

    public static DeviceCheckFragment newInstance(TutorialData tutorialData) {

        Bundle args = new Bundle();
        args.putParcelable("TutorialData",tutorialData);

        DeviceCheckFragment fragment = new DeviceCheckFragment();
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
        fragmentDeviceCheckBinding.tvInstruction1.setText(tutorialData.getInstruction1());
        fragmentDeviceCheckBinding.tvInstruction2.setText(tutorialData.getInstruction2());
        if (tutorialData.getInstruction2().length() == 0) {
            fragmentDeviceCheckBinding.guide2.setVisibility(View.GONE);
        } else {
            fragmentDeviceCheckBinding.guide2.setVisibility(View.VISIBLE);
        }
        fragmentDeviceCheckBinding.guide2.setOnCheckedChangeListener(this);
        fragmentDeviceCheckBinding.btnNext.setOnClickListener(this);
        fragmentDeviceCheckBinding.stepView.go(tutorialData.getPosition(),true);
        if(tutorialData.getPosition() == 3)
        {
            fragmentDeviceCheckBinding.btnNext.setText(getString(R.string.done).toUpperCase());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (canContinue()) {
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            fragmentDeviceCheckBinding.btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }

    @Override
    public void onClick(View v) {
        if (canContinue()) {
            ((DeviceCheckActivity)context).gotoNext();
        }
    }

    private boolean canContinue() {
        return fragmentDeviceCheckBinding.guide2.isChecked() || (fragmentDeviceCheckBinding.guide2.getVisibility() == View.GONE);
    }
}
