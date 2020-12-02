package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentTutorialBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.activities.TutorialActivity;

public class TutorialFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private Context context;


    TutorialData tutorialData;
    FragmentTutorialBinding fragmentTutorialBinding;

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
        fragmentTutorialBinding.tvInstruction1.setText(tutorialData.getInstruction1());
        fragmentTutorialBinding.tvInstruction2.setText(tutorialData.getInstruction2());
        fragmentTutorialBinding.guide1.setOnCheckedChangeListener(this);
        fragmentTutorialBinding.guide2.setOnCheckedChangeListener(this);
        fragmentTutorialBinding.btnNext.setOnClickListener(this);
        fragmentTutorialBinding.stepView.go(tutorialData.getPosition(),true);
        if(tutorialData.getPosition() == 3)
        {
            fragmentTutorialBinding.btnNext.setText("DONE");
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
    }
}
