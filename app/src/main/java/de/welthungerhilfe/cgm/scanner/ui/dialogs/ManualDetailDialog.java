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

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.databinding.DialogManualDetailBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

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
        } else {
            dialogManualDetailBinding.imgType.setImageResource(R.drawable.machine);
            dialogManualDetailBinding.txtTitle.setText(R.string.machine_measure);
        }

        dialogManualDetailBinding.txtManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, measure.getDate()));
        if (measure.getLocation() != null)
            dialogManualDetailBinding.txtManualLocation.setText(measure.getLocation().getAddress());
        else
            dialogManualDetailBinding.txtManualLocation.setText(R.string.last_location_error);

        SessionManager sessionManager = new SessionManager(getContext());
        boolean stdtest = sessionManager.getStdTestQrCode() != null;
        if (!stdtest) {
            dialogManualDetailBinding.txtManualHeight.setText(String.valueOf(measure.getHeight()));
            dialogManualDetailBinding.txtManualWeight.setText(String.valueOf(measure.getWeight()));
            dialogManualDetailBinding.txtManualMuac.setText(String.valueOf(measure.getMuac()));
        } else {
            dialogManualDetailBinding.txtManualHeight.setText(R.string.field_concealed);
            dialogManualDetailBinding.txtManualWeight.setText(R.string.field_concealed);
            dialogManualDetailBinding.txtManualMuac.setText(R.string.field_concealed);
        }
        dialogManualDetailBinding.checkManualOedema.setChecked(!measure.isOedema());
        dialogManualDetailBinding.checkManualOedema.setEnabled(false);
    }
}
