package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.TutorialActivity;

public class Tutorial3Fragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private Context context;

    AppCompatCheckBox guide1;
    AppCompatCheckBox guide2;

    Button btnNext;

    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutorial3, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(view);

        guide1 = view.findViewById(R.id.guide1);
        guide2 = view.findViewById(R.id.guide2);
        btnNext = view.findViewById(R.id.btnNext);

        guide1.setOnCheckedChangeListener(this);
        guide2.setOnCheckedChangeListener(this);
        btnNext.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (guide1.isChecked() && guide2.isChecked()) {
            btnNext.setBackgroundResource(R.drawable.button_green_circular);
        } else {
            btnNext.setBackgroundResource(R.drawable.button_green_light_circular);
        }
    }

    @Override
    public void onClick(View v) {
        if (guide1.isChecked() && guide2.isChecked()) {
            ((TutorialActivity)context).gotoNext();
        }
    }
}
