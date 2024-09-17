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
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentTutorialBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.TutorialActivity;

public class TutorialFragment {/*extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private Context context;
    private TutorialData tutorialData;
    private FragmentTutorialBinding fragmentTutorialBinding;

    public static TutorialFragment newInstance(TutorialData tutorialData) {

        Bundle args = new Bundle();
        args.putParcelable("TutorialData",tutorialData);

        TutorialFragment fragment = new TutorialFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentTutorialBinding = DataBindingUtil.inflate(inflater,R.layout.fragment_tutorial, container, false);
        tutorialData = getArguments().getParcelable("TutorialData");
        return fragmentTutorialBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        fragmentTutorialBinding.tvTitle.setText(tutorialData.getTitle());
        fragmentTutorialBinding.ivTitle.setImageResource(tutorialData.getImage());
                fragmentTutorialBinding.guide1.setText(tutorialData.getInstruction1());
        fragmentTutorialBinding.guide2.setText(tutorialData.getInstruction2());
        fragmentTutorialBinding.guide1.setOnCheckedChangeListener(this);
        fragmentTutorialBinding.guide2.setOnCheckedChangeListener(this);
        fragmentTutorialBinding.btnNext.setOnClickListener(this);
        fragmentTutorialBinding.stepView.go(tutorialData.getPosition(),true);
        if(tutorialData.getPosition() == 3) {
            fragmentTutorialBinding.btnNext.setText(getString(R.string.done).toUpperCase());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (fragmentTutorialBinding.guide1.isChecked() && fragmentTutorialBinding.guide2.isChecked()) {
            fragmentTutorialBinding.btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            fragmentTutorialBinding.btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }

    @Override
    public void onClick(View v) {
        if (fragmentTutorialBinding.guide1.isChecked() && fragmentTutorialBinding.guide2.isChecked()) {
            ((TutorialActivity)context).gotoNext();
        }
    }*/
}