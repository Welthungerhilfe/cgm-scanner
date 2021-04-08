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
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ManualDetailDialog extends Dialog {

    @BindView(R.id.imgType)
    ImageView imgType;
    @BindView(R.id.txtTitle)
    TextView txtTitle;
    @BindView(R.id.txtManualDate)
    TextView txtManualDate;
    @BindView(R.id.txtManualHeight)
    TextView txtManualHeight;
    @BindView(R.id.txtManualWeight)
    TextView txtManualWeight;
    @BindView(R.id.txtManualMuac)
    TextView txtManualMuac;
    @BindView(R.id.txtManualLocation)
    TextView txtManualLocation;
    @BindView(R.id.checkManualOedema)
    AppCompatCheckBox checkManualOedema;


    @OnClick(R.id.btnOK)
    void OnConfirm(Button btnOK) {
        dismiss();
    }

    private Measure measure;

    public ManualDetailDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_manual_detail);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;

        updateUI();
    }

    private void updateUI() {
        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            imgType.setImageResource(R.drawable.manual);
            txtTitle.setText(R.string.manual_measure);
        } else {
            imgType.setImageResource(R.drawable.machine);
            txtTitle.setText(R.string.machine_measure);
        }

        txtManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, measure.getDate()));
        if (measure.getLocation() != null)
            txtManualLocation.setText(measure.getLocation().getAddress());
        else
            txtManualLocation.setText(R.string.last_location_error);

        boolean manual = measure.getType().compareTo(AppConstants.VAL_MEASURE_MANUAL) == 0;
        boolean stdtest = Utils.isStdTestQRCode(measure.getQrCode());
        if (!stdtest || manual) {
            txtManualHeight.setText(String.valueOf(measure.getHeight()));
            txtManualWeight.setText(String.valueOf(measure.getWeight()));
            txtManualMuac.setText(String.valueOf(measure.getMuac()));
        } else {
            txtManualHeight.setText(R.string.field_concealed);
            txtManualWeight.setText(R.string.field_concealed);
            txtManualMuac.setText(R.string.field_concealed);
        }
        checkManualOedema.setChecked(!measure.isOedema());
        checkManualOedema.setEnabled(false);
    }
}
