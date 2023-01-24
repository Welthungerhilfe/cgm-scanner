package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.test.espresso.remote.EspressoRemoteMessage;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogSelectModeBinding;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class SelectModeDialog extends DialogFragment {

    DialogSelectModeBinding dialogSelectModeBinding;
    String selectedMode = "No Mode selected";
    SessionManager sessionManager;
    SetupmodeListner setupmodeListner;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            setupmodeListner = (SetupmodeListner) getActivity();

        }
        catch (ClassCastException e) {
            Log.e("", "onAttach: ClassCastException: "
                    + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialogSelectModeBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_select_mode,container,false);

        String rapidBuilder = "<b>Rapid Screening :</b> Screening at child population level to know the malnutrition prevalence";
        dialogSelectModeBinding.textRapid.setText(Html.fromHtml(rapidBuilder,Html.FROM_HTML_MODE_LEGACY));
        String growthBuilder = "<b>Growth Monitiring :</b> To measure a child to know his/her malnutrition status";
        dialogSelectModeBinding.textGrowth.setText(Html.fromHtml(growthBuilder,Html.FROM_HTML_MODE_LEGACY));
        sessionManager = new SessionManager(getActivity());

        if(sessionManager.getSelectedMode() == AppConstants.NO_MODE_SELECTED){
            getDialog().setCancelable(false);
        }

        if(sessionManager.getSelectedMode() == AppConstants.CGM_MODE){
            selectCGM();
        }
        if(sessionManager.getSelectedMode() == AppConstants.RST_MODE){
            selectRST();
        }
        dialogSelectModeBinding.btGrowthMonitiring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectCGM();
            }
        });

        dialogSelectModeBinding.btRapidScreening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectRST();
            }
        });
        dialogSelectModeBinding.btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(),""+selectedMode,Toast.LENGTH_LONG).show();
                setupmodeListner.changeSetupMode();
                dismiss();
            }
        });

        dialogSelectModeBinding.tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();
            }
        });

        if(sessionManager.getSelectedMode() == AppConstants.NO_MODE_SELECTED){
            dialogSelectModeBinding.tvCancel.setVisibility(View.GONE);
        }
        return dialogSelectModeBinding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        return super.onCreateDialog(savedInstanceState);
    }

    public interface SetupmodeListner{
        void changeSetupMode();
    }

    public void selectCGM(){
        dialogSelectModeBinding.btGrowthMonitiring.setTextColor(getResources().getColor(R.color.colorWhite,null));
        dialogSelectModeBinding.btGrowthMonitiring.setBackground(getResources().getDrawable(R.drawable.button_green_round,null));
        dialogSelectModeBinding.btRapidScreening.setTextColor(getResources().getColor(R.color.colorPrimary,null));
        dialogSelectModeBinding.btRapidScreening.setBackground(getResources().getDrawable(R.drawable.rounded_greycolor_greenborder,null));
        dialogSelectModeBinding.btNext.setBackground(getResources().getDrawable(R.drawable.button_green_round,null));
        selectedMode = "Growth Monitiring";
        sessionManager.setSelectedMode(AppConstants.CGM_MODE);
    }
    public void selectRST(){
        dialogSelectModeBinding.btRapidScreening.setTextColor(getResources().getColor(R.color.colorWhite,null));
        dialogSelectModeBinding.btRapidScreening.setBackground(getResources().getDrawable(R.drawable.button_green_round,null));
        dialogSelectModeBinding.btGrowthMonitiring.setTextColor(getResources().getColor(R.color.colorPrimary,null));
        dialogSelectModeBinding.btGrowthMonitiring.setBackground(getResources().getDrawable(R.drawable.rounded_greycolor_greenborder,null));
        dialogSelectModeBinding.btNext.setBackground(getResources().getDrawable(R.drawable.button_green_round,null));
        selectedMode = "Rapid Screening";
        sessionManager.setSelectedMode(AppConstants.RST_MODE);
    }
}
