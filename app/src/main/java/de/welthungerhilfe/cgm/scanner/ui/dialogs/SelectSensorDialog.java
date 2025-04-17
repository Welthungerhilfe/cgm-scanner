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

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogSelectModeBinding;
import de.welthungerhilfe.cgm.scanner.databinding.DialogSelsectSensorModeBinding;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

public class SelectSensorDialog extends DialogFragment {

    DialogSelsectSensorModeBinding dialogSelectModeBinding;
    int selectedSensor = AppConstants.NO_SENSOR_MODE_SELECTED;
    SessionManager sessionManager;
    SensorSelectionListener sensorSelectionListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            sensorSelectionListener = (SensorSelectionListener) getActivity();
        } catch (ClassCastException e) {
            Log.e("", "onAttach: ClassCastException: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialogSelectModeBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_selsect_sensor_mode, container, false);

        String noSensor = "<b>Phone Camera Scan:</b> Use only your phone's camera to scan the child";
        dialogSelectModeBinding.textNoSensor.setText(Html.fromHtml(noSensor, Html.FROM_HTML_MODE_LEGACY));

        String ultrasonicSensorDesc = "<b>External Sensor Scan:</b> Use an external sensor attached to your phone for child scanning";
        dialogSelectModeBinding.textExternalSensor.setText(Html.fromHtml(ultrasonicSensorDesc, Html.FROM_HTML_MODE_LEGACY));
        sessionManager = new SessionManager(getActivity());
        Log.i("SensorDialog","this is select sensor "+sessionManager.getSensorMode());
        if (sessionManager.getSensorMode() == AppConstants.NO_SENSOR_MODE_SELECTED) {
            getDialog().setCancelable(false);
            Log.i("SensorDialog","this is select sensor "+sessionManager.getSensorMode());
        }

        if (sessionManager.getSensorMode() == AppConstants.NO_SENSOR_SELECTED) {
            selectNOSensor();
        }
        if (sessionManager.getSensorMode() == AppConstants.SENSOR_SELECTED) {
            selectSensor();
        }




        dialogSelectModeBinding.btExternalSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //   Toast.makeText(getActivity(), "Please select a external", Toast.LENGTH_LONG).show();

                selectSensor();
            }
        });


        dialogSelectModeBinding.btNoSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //    Toast.makeText(getActivity(), "Please select a no", Toast.LENGTH_LONG).show();

                selectNOSensor();
            }
        });
       // dialogSelectModeBinding.btNoSensor.setOnClickListener(view -> selectNOSensor());

        dialogSelectModeBinding.btNext.setOnClickListener(view -> {
            if (selectedSensor == AppConstants.NO_SENSOR_MODE_SELECTED) {
                Toast.makeText(getActivity(), "Please select a scan device", Toast.LENGTH_LONG).show();
                return;
            }
            sessionManager.setSensorMode(selectedSensor);
            sensorSelectionListener.changeSelectedSensor();
            dismiss();
        });

        dialogSelectModeBinding.tvCancel.setOnClickListener(view -> dismiss());

        if (sessionManager.getSensorMode() == AppConstants.NO_SENSOR_MODE_SELECTED) {
            dialogSelectModeBinding.tvCancel.setVisibility(View.GONE);
        }

        getDialog().setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (sessionManager.getSensorMode() == AppConstants.NO_SENSOR_MODE_SELECTED) {
                    getActivity().finish();
                } else {
                    dismiss();
                }
                return true;
            }
            return false;
        });

        return dialogSelectModeBinding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        return super.onCreateDialog(savedInstanceState);
    }

    public interface SensorSelectionListener {
        void changeSelectedSensor();
    }

    public void selectSensor() {
        dialogSelectModeBinding.btExternalSensor.setTextColor(getResources().getColor(R.color.colorWhite, null));
        dialogSelectModeBinding.btExternalSensor.setBackground(getResources().getDrawable(R.drawable.button_green_round, null));
        dialogSelectModeBinding.btNoSensor.setTextColor(getResources().getColor(R.color.colorPrimary, null));
        dialogSelectModeBinding.btNoSensor.setBackground(getResources().getDrawable(R.drawable.rounded_greycolor_greenborder, null));
        dialogSelectModeBinding.btNext.setBackground(getResources().getDrawable(R.drawable.button_green_round, null));
        selectedSensor = AppConstants.SENSOR_SELECTED;
    }

    public void selectNOSensor() {
        dialogSelectModeBinding.btNoSensor.setTextColor(getResources().getColor(R.color.colorWhite, null));
        dialogSelectModeBinding.btNoSensor.setBackground(getResources().getDrawable(R.drawable.button_green_round, null));
        dialogSelectModeBinding.btExternalSensor.setTextColor(getResources().getColor(R.color.colorPrimary, null));
        dialogSelectModeBinding.btExternalSensor.setBackground(getResources().getDrawable(R.drawable.rounded_greycolor_greenborder, null));
        dialogSelectModeBinding.btNext.setBackground(getResources().getDrawable(R.drawable.button_green_round, null));
        selectedSensor = AppConstants.NO_SENSOR_SELECTED;
    }
}
