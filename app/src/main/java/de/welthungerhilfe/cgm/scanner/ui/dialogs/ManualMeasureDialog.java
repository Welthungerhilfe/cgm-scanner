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
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.databinding.DataBindingUtil;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogManualMeasureBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.ui.views.UnitEditText;
import de.welthungerhilfe.cgm.scanner.utils.CalculateZscoreUtils;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import okhttp3.internal.Util;

public class ManualMeasureDialog extends Dialog implements View.OnClickListener {



    private boolean oedema = false;

  //  @OnClick(R.id.imgLocation)
    public void onLocation() {
        LocationDetectActivity.navigate((AppCompatActivity) mContext, dialogManualMeasureBinding.editManualLocation, location, location -> {
            ManualMeasureDialog.this.location = location;
            dialogManualMeasureBinding.editManualLocation.setText(location.getAddress());
        });
    }

   // @OnClick(R.id.txtCancel)
    public void onCancel() {
        dismiss();
    }

    //@OnClick(R.id.btnOK)
    public void OnConfirm() {
        if(!validate()){
            return;
        }

        boolean wfa = validZscore(CalculateZscoreUtils.ChartType.WEIGHT_FOR_AGE);
        boolean hfa = validZscore(CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE);
        boolean mfa = validZscore(CalculateZscoreUtils.ChartType.MUAC_FOR_AGE);

        if (wfa && hfa && mfa) {
            updateManualMeasurements();
        } else {
            if (!wfa) dialogManualMeasureBinding.editManualWeight.setError(mContext.getString(R.string.invalid_zscore));
            if (!hfa) dialogManualMeasureBinding.editManualHeight.setError(mContext.getString(R.string.invalid_zscore));
            if (!mfa) dialogManualMeasureBinding.editManualMuac.setError(mContext.getString(R.string.invalid_zscore));

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.invalid_zscore);
            builder.setPositiveButton(R.string.selector_yes, (dialogInterface, i) -> {
                updateManualMeasurements();
                dismiss();
            });
            builder.setNegativeButton(R.string.selector_no, (dialogInterface, i) -> show());
            builder.show();
        }
    }

    public void updateManualMeasurements(){
        String measureServerKey = null;
        if(measure!=null)
        {
            measureServerKey = measure.getMeasureServerKey();
        }
        if (measureListener != null) {
            if (!oedema) {
                final TextView message = new TextView(mContext);
                final SpannableString s = new SpannableString(mContext.getText(R.string.edema_link));
                Linkify.addLinks(s, Linkify.WEB_URLS);
                int p = (int) (25 * ((float) getContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
                message.setPadding(p, p, p, p);
                message.setText(s);
                message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                message.setMovementMethod(LinkMovementMethod.getInstance());
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.edema_check);
                builder.setView(message);
                String finalMeasureServerKey = measureServerKey;
                builder.setPositiveButton(R.string.selector_yes, (dialogInterface, i) -> {
                    oedema = true;
                    measureListener.onManualMeasure(
                            measure != null ? measure.getId() : null,
                            Utils.parseDouble(dialogManualMeasureBinding.editManualHeight.getText().toString()),
                            Utils.parseDouble(dialogManualMeasureBinding.editManualWeight.getText().toString()),
                            Utils.parseDouble(dialogManualMeasureBinding.editManualMuac.getText().toString()),
                            0f,
                            location,
                            oedema,
                            finalMeasureServerKey
                    );
                    dismiss();
                });
                builder.setNegativeButton(R.string.selector_no, (dialogInterface, i) -> show());
                builder.show();
            } else {
                oedema = false;
                measureListener.onManualMeasure(
                        measure != null ? measure.getId() : null,
                        Utils.parseDouble(dialogManualMeasureBinding.editManualHeight.getText().toString()),
                        Utils.parseDouble(dialogManualMeasureBinding.editManualWeight.getText().toString()),
                        Utils.parseDouble(dialogManualMeasureBinding.editManualMuac.getText().toString()),
                        0f,
                        location,
                        oedema,
                        measureServerKey
                );
                dismiss();
            }
        }
    }



    private Context mContext;
    private Measure measure;
    private Person person;
    private Loc location;

    private ManualMeasureListener measureListener;
    private CloseListener closeListener;

    DialogManualMeasureBinding dialogManualMeasureBinding;

    public ManualMeasureDialog(@NonNull Context context) {
        super(context);

        mContext = context;
        location = ((CreateDataActivity) mContext).location;

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogManualMeasureBinding = DataBindingUtil.inflate(LayoutInflater.from(context),R.layout.dialog_manual_measure,null,false);
        this.setContentView(dialogManualMeasureBinding.getRoot());
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);

        dialogManualMeasureBinding.editManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, System.currentTimeMillis()));
        if (location != null)
            dialogManualMeasureBinding.editManualLocation.setText(location.getAddress());
        dialogManualMeasureBinding.editManualLocation.setOnClickListener(this);
        dialogManualMeasureBinding.imgLocation.setOnClickListener(this);
        dialogManualMeasureBinding.txtCancel.setOnClickListener(this);
        dialogManualMeasureBinding.btnOK.setOnClickListener(this);
        dialogManualMeasureBinding.checkManualOedema.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    buttonView.setBackgroundResource(R.color.colorPink);
                    buttonView.setTextColor(getContext().getColor(R.color.colorWhite));
                } else {
                    buttonView.setBackgroundResource(R.color.colorWhite);
                    buttonView.setTextColor(getContext().getColor(R.color.colorBlack));
                }
                oedema = isChecked;
            }
        });

    }

    public void show() {
        super.show();

        if (closeListener != null)
            closeListener.onClose(true);
    }

    public void dismiss() {
        dialogManualMeasureBinding.editManualHeight.setText("");
        dialogManualMeasureBinding.editManualWeight.setText("");
        dialogManualMeasureBinding.editManualMuac.setText("");
        dialogManualMeasureBinding.checkManualOedema.setChecked(false);

        super.dismiss();

        if (closeListener != null)
            closeListener.onClose(false);
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;

        updateUI();
    }

    public void setPerson(Person person){
        this.person = person;
    }

    private void updateUI() {
        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            dialogManualMeasureBinding.imgType.setImageResource(R.drawable.manual);
            dialogManualMeasureBinding.txtTitle.setText(R.string.manual_measure);
        } else {
            dialogManualMeasureBinding.imgType.setImageResource(R.drawable.machine);
            dialogManualMeasureBinding.txtTitle.setText(R.string.machine_measure);
        }

        dialogManualMeasureBinding.editManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, measure.getDate()));
        if (measure.getLocation() != null)
            dialogManualMeasureBinding.editManualLocation.setText(measure.getLocation().getAddress());
            dialogManualMeasureBinding.editManualHeight.setText(String.valueOf(measure.getHeight()));
            dialogManualMeasureBinding.editManualWeight.setText(String.valueOf(measure.getWeight()));
            dialogManualMeasureBinding.editManualMuac.setText(String.valueOf(measure.getMuac()));
            dialogManualMeasureBinding.checkManualOedema.setChecked(!measure.isOedema());

        location = measure.getLocation();
    }

    public void setManualMeasureListener(ManualMeasureListener listener) {
        measureListener = listener;
    }

    public void setCloseListener(CloseListener listener) {
        closeListener = listener;
    }

    private boolean validate() {
        boolean valid = true;

        String height = dialogManualMeasureBinding.editManualHeight.getText().toString();
        String weight = dialogManualMeasureBinding.editManualWeight.getText().toString();
        String muac = dialogManualMeasureBinding.editManualMuac.getText().toString();

        if (height.isEmpty()) {
            dialogManualMeasureBinding.editManualHeight.setError(getString(R.string.tooltip_cm));
            valid = false;
        } else if (Utils.checkDoubleDecimals(height) != 1) {
            dialogManualMeasureBinding.editManualHeight.setError(getString(R.string.tooltip_decimal));
            valid = false;
        } else if (Utils.parseDouble(height) <= 45) {
            dialogManualMeasureBinding.editManualHeight.setError(getString(R.string.tooltipe_height_min));
            valid = false;
        } else if (Utils.parseDouble(height) >= 130) {
            dialogManualMeasureBinding.editManualHeight.setError(getString(R.string.tooltipe_height_max));
            valid = false;
        } else {
            dialogManualMeasureBinding.editManualHeight.setError(null);
        }

        if (weight.isEmpty()) {
            dialogManualMeasureBinding.editManualWeight.setError(getString(R.string.tooltip_kg));
            valid = false;
        } else if (Utils.checkDoubleDecimals(weight) != 3) {
            dialogManualMeasureBinding.editManualWeight.setError(getString(R.string.tooltip_kg_precision));
            valid = false;
        } else if (Utils.parseDouble(weight) < 2) {
            dialogManualMeasureBinding.editManualWeight.setError(getString(R.string.tooltipe_weight_min));
            valid = false;
        } else if (Utils.parseDouble(weight) > 30) {
            dialogManualMeasureBinding.editManualWeight.setError(getString(R.string.tooltipe_weight_max));
            valid = false;
        } else {
            dialogManualMeasureBinding.editManualWeight.setError(null);
        }

        if (muac.isEmpty()) {
            dialogManualMeasureBinding.editManualMuac.setError(getString(R.string.tooltip_cm));
            valid = false;
        } else if (Utils.checkDoubleDecimals(muac) != 1) {
            dialogManualMeasureBinding.editManualMuac.setError(getString(R.string.tooltip_decimal));
            valid = false;
        } else if (Utils.parseDouble(muac) < 7) {
            dialogManualMeasureBinding.editManualMuac.setError(getString(R.string.tooltipe_muac_min));
            valid = false;
        } else if (Utils.parseDouble(muac) > 22) {
            dialogManualMeasureBinding.editManualMuac.setError(getString(R.string.tooltipe_muac_max));
            valid = false;
        } else {
            dialogManualMeasureBinding.editManualMuac.setError(null);
        }

        return valid;
    }

    private String getString(int string){
        return mContext.getResources().getString(string);
    }

    private boolean validZscore(CalculateZscoreUtils.ChartType chartType) {
        String height = dialogManualMeasureBinding.editManualHeight.getText().toString();
        String weight = dialogManualMeasureBinding.editManualWeight.getText().toString();
        String muac = dialogManualMeasureBinding.editManualMuac.getText().toString();
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        double zScore = CalculateZscoreUtils.setData(mContext,Utils.parseDouble(height),Utils.parseDouble(weight), Utils.parseDouble(muac),age,person.getSex(), chartType);
        return Math.abs(zScore) < 3.0;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.editManualLocation) {
            LocationDetectActivity.navigate((AppCompatActivity) mContext, dialogManualMeasureBinding.editManualLocation, location, location -> {
                ManualMeasureDialog.this.location = location;
                dialogManualMeasureBinding.editManualLocation.setText(location.getAddress());
            });
        } else if(v.getId() == R.id.imgLocation){
            onLocation();
        }else if(v.getId()== R.id.txtCancel){
            onCancel();
        }else if(v.getId() == R.id.btnOK){
            OnConfirm();
        }

    }

    public interface ManualMeasureListener {
        void onManualMeasure(String id, double height, double weight, double muac, double headCircumference, Loc location, boolean oedema, String measureServerKey);
    }

    public interface CloseListener {
        void onClose(boolean result);
    }

    public interface LocationListener {
        void onConfirm(Loc location);
    }
}
