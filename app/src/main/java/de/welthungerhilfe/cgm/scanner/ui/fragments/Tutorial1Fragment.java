package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.content.Context;
import android.database.DatabaseUtils;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.FragmentTutorial1Binding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.activities.TutorialActivity;

public class Tutorial1Fragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private Context context;


    TutorialData tutorialData;
    FragmentTutorial1Binding fragmentTutorial1Binding;

    public static Tutorial1Fragment newInstance(TutorialData tutorialData) {

        Bundle args = new Bundle();
        args.putParcelable("TutorialData",tutorialData);

        Tutorial1Fragment fragment = new Tutorial1Fragment();
        fragment.setArguments(args);
        return fragment;
    }


    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentTutorial1Binding = DataBindingUtil.inflate(inflater,R.layout.fragment_tutorial1, container, false);
        tutorialData = getArguments().getParcelable("TutorialData");
        return fragmentTutorial1Binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        fragmentTutorial1Binding.tvTitle.setText(tutorialData.getTitle());
        fragmentTutorial1Binding.ivTitle.setImageResource(tutorialData.getImage());
        fragmentTutorial1Binding.tvInstruction1.setText(tutorialData.getInstruction1());
        fragmentTutorial1Binding.tvInstruction2.setText(tutorialData.getInstruction2());
        fragmentTutorial1Binding.guide1.setOnCheckedChangeListener(this);
        fragmentTutorial1Binding.guide2.setOnCheckedChangeListener(this);
        fragmentTutorial1Binding.btnNext.setOnClickListener(this);
        fragmentTutorial1Binding.stepView.go(tutorialData.getPosition(),true);
        if(tutorialData.getPosition() == 3)
        {
            fragmentTutorial1Binding.btnNext.setText("DONE");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (fragmentTutorial1Binding.guide1.isChecked() && fragmentTutorial1Binding.guide2.isChecked()) {
            fragmentTutorial1Binding.btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            fragmentTutorial1Binding.btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }

    @Override
    public void onClick(View v) {
        if (fragmentTutorial1Binding.guide1.isChecked() && fragmentTutorial1Binding.guide2.isChecked()) {
            ((TutorialActivity)context).gotoNext();
        }
    }
}
