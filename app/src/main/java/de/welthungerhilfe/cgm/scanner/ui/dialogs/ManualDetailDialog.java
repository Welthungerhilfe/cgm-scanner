/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.databinding.DialogManualDetailBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class ManualDetailDialog extends Dialog {

    private Measure measure;

    DialogManualDetailBinding dialogManualDetailBinding;

    public ManualDetailDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogManualDetailBinding = DataBindingUtil.inflate(LayoutInflater.from(context),R.layout.dialog_manual_detail,null,false);
        this.setContentView(dialogManualDetailBinding.getRoot());
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);
        dialogManualDetailBinding.btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    public void setMeasure(Measure measure) {
        this.measure = measure;

        updateUI();
    }

    private void updateUI() {
        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            dialogManualDetailBinding.imgType.setImageResource(R.drawable.manual);
            dialogManualDetailBinding.txtTitle.setText(R.string.manual_measure);
            dialogManualDetailBinding.rltNegError.setVisibility(View.GONE);
            dialogManualDetailBinding.rltPosError.setVisibility(View.GONE);
        } else {
            dialogManualDetailBinding.imgType.setImageResource(R.drawable.machine);
            dialogManualDetailBinding.txtTitle.setText(R.string.machine_measure);
            if(measure.getPositive_height_error()==0) {
                dialogManualDetailBinding.txtPositivePe.setText("--");
            } else {
                dialogManualDetailBinding.txtPositivePe.setText(String.format(Locale.getDefault(), "%.1f", measure.getPositive_height_error()));
            }
            if(measure.getNegative_height_error()==0){
                dialogManualDetailBinding.txtNegativePe.setText("--");
            } else {
                dialogManualDetailBinding.txtNegativePe.setText(String.format(Locale.getDefault(), "%.1f", measure.getNegative_height_error()));
            }
        }

        dialogManualDetailBinding.txtManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, measure.getDate()));
        if (measure.getLocation() != null)
            dialogManualDetailBinding.txtManualLocation.setText(measure.getLocation().getAddress());
        else
            dialogManualDetailBinding.txtManualLocation.setText(R.string.last_location_error);

        SessionManager sessionManager = new SessionManager(getContext());
        boolean stdtest = sessionManager.getStdTestQrCode() != null;
        if (!stdtest) {
            dialogManualDetailBinding.txtManualHeight.setText(String.format(Locale.getDefault(), "%.1f", measure.getHeight()));
            dialogManualDetailBinding.txtManualWeight.setText(String.format(Locale.getDefault(), "%.3f", measure.getWeight()));
            dialogManualDetailBinding.txtManualMuac.setText(String.format(Locale.getDefault(), "%.1f", measure.getMuac()));
        } else {
            dialogManualDetailBinding.txtManualHeight.setText(R.string.field_concealed);
            dialogManualDetailBinding.txtManualWeight.setText(R.string.field_concealed);
            dialogManualDetailBinding.txtManualMuac.setText(R.string.field_concealed);
        }
        dialogManualDetailBinding.checkManualOedema.setChecked(!measure.isOedema());
        dialogManualDetailBinding.checkManualOedema.setEnabled(false);
    }
}
