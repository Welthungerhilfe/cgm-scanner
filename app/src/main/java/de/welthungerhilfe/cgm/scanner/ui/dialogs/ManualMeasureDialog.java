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

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.ui.views.UnitEditText;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ManualMeasureDialog extends Dialog implements View.OnClickListener {

    @BindView(R.id.imgType)
    ImageView imgType;
    @BindView(R.id.txtTitle)
    TextView txtTitle;
    @BindView(R.id.editManualDate)
    EditText editManualDate;
    @BindView(R.id.editManualHeight)
    UnitEditText editManualHeight;
    @BindView(R.id.editManualWeight)
    UnitEditText editManualWeight;
    @BindView(R.id.editManualMuac)
    UnitEditText editManualMuac;
    @BindView(R.id.editManualLocation)
    EditText editManualLocation;
    @BindView(R.id.btnOK)
    Button btnOK;
    @BindView(R.id.checkManualOedema)
    AppCompatCheckBox checkManualOedema;

    private boolean oedema = false;

    @OnCheckedChanged(R.id.checkManualOedema)
    void onAlert(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
            buttonView.setBackgroundResource(R.color.colorPink);
            buttonView.setTextColor(getContext().getColor(R.color.colorWhite));
        } else {
            buttonView.setBackgroundResource(R.color.colorWhite);
            buttonView.setTextColor(getContext().getColor(R.color.colorBlack));
        }
        oedema = isChecked;
    }

    @OnClick(R.id.imgLocation)
    void onLocation(ImageView imgLocation) {
        LocationDetectActivity.navigate((AppCompatActivity) mContext, editManualLocation, location, location -> {
            ManualMeasureDialog.this.location = location;
            editManualLocation.setText(location.getAddress());
        });
    }

    @OnClick(R.id.txtCancel)
    void onCancel(TextView txtCancel) {
        dismiss();
    }

    @OnClick(R.id.btnOK)
    void OnConfirm(Button btnOK) {
        String measureServerKey = null;
        if(measure!=null)
        {
            measureServerKey = measure.getMeasureServerKey();
        }
        if (validate() && measureListener != null) {
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
                            Utils.parseDouble(editManualHeight.getText().toString()),
                            Utils.parseDouble(editManualWeight.getText().toString()),
                            Utils.parseDouble(editManualMuac.getText().toString()),
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
                        Utils.parseDouble(editManualHeight.getText().toString()),
                        Utils.parseDouble(editManualWeight.getText().toString()),
                        Utils.parseDouble(editManualMuac.getText().toString()),
                        0f,
                        location,
                        oedema,
                       measureServerKey
                );
                dismiss();
            }
        }
    }


    @BindString(R.string.tooltip_kg)
    String tooltip_kg;
    @BindString(R.string.tooltip_kg_precision)
    String tooltip_kg_precision;
    @BindString(R.string.tooltip_decimal)
    String tooltip_decimal;
    @BindString(R.string.tooltip_weight_ex)
    String tooltip_weight_ex;
    @BindString(R.string.tooltip_cm)
    String tooltip_cm;
    @BindString(R.string.tooltip_height_ex)
    String tooltip_height_ex;
    @BindString(R.string.tooltipe_height_min)
    String tooltipe_height_min;
    @BindString(R.string.tooltipe_height_max)
    String tooltipe_height_max;
    @BindString(R.string.tooltipe_weight_min)
    String tooltipe_weight_min;
    @BindString(R.string.tooltipe_weight_max)
    String tooltipe_weight_max;
    @BindString(R.string.tooltipe_muac_min)
    String tooltipe_muac_min;
    @BindString(R.string.tooltipe_muac_max)
    String tooltipe_muac_max;


    private Context mContext;
    private Measure measure;
    private Loc location;

    private ManualMeasureListener measureListener;
    private CloseListener closeListener;

    public ManualMeasureDialog(@NonNull Context context) {
        super(context);

        mContext = context;
        location = ((CreateDataActivity) mContext).location;

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_manual_measure);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);

        editManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, System.currentTimeMillis()));
        if (location != null)
            editManualLocation.setText(location.getAddress());
        editManualLocation.setOnClickListener(this);
    }

    public void show() {
        super.show();

        if (closeListener != null)
            closeListener.onClose(true);
    }

    public void dismiss() {
        editManualHeight.setText("");
        editManualWeight.setText("");
        editManualMuac.setText("");
        checkManualOedema.setChecked(false);

        super.dismiss();

        if (closeListener != null)
            closeListener.onClose(false);
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

        editManualDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, measure.getDate()));
        if (measure.getLocation() != null)
            editManualLocation.setText(measure.getLocation().getAddress());
        editManualHeight.setText(String.valueOf(measure.getHeight()));
        editManualWeight.setText(String.valueOf(measure.getWeight()));
        editManualMuac.setText(String.valueOf(measure.getMuac()));
        checkManualOedema.setChecked(!measure.isOedema());

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

        String height = editManualHeight.getText().toString();
        String weight = editManualWeight.getText().toString();
        String muac = editManualMuac.getText().toString();

        if (height.isEmpty()) {
            editManualHeight.setError(tooltip_cm);
            valid = false;
        } else if (Utils.checkDoubleDecimals(height) != 1) {
            editManualHeight.setError(tooltip_decimal);
            valid = false;
        } else if (Utils.parseDouble(height) <= 45) {
            editManualHeight.setError(tooltipe_height_min);
            valid = false;
        } else if (Utils.parseDouble(height) >= 130) {
            editManualHeight.setError(tooltipe_height_max);
            valid = false;
        } else {
            editManualHeight.setError(null);
        }

        if (weight.isEmpty()) {
            editManualWeight.setError(tooltip_kg);
            valid = false;
        } else if (Utils.checkDoubleDecimals(weight) != 3) {
            editManualWeight.setError(tooltip_kg_precision);
            valid = false;
        } else if (Utils.parseDouble(weight) < 2) {
            editManualWeight.setError(tooltipe_weight_min);
            valid = false;
        } else if (Utils.parseDouble(weight) > 30) {
            editManualWeight.setError(tooltipe_weight_max);
            valid = false;
        } else {
            editManualWeight.setError(null);
        }

        if (muac.isEmpty()) {
            editManualMuac.setError(tooltip_cm);
            valid = false;
        } else if (Utils.checkDoubleDecimals(muac) != 1) {
            editManualMuac.setError(tooltip_decimal);
            valid = false;
        } else if (Utils.parseDouble(muac) < 7) {
            editManualMuac.setError(tooltipe_muac_min);
            valid = false;
        } else if (Utils.parseDouble(muac) > 22) {
            editManualMuac.setError(tooltipe_muac_max);
            valid = false;
        } else {
            editManualMuac.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.editManualLocation) {
            LocationDetectActivity.navigate((AppCompatActivity) mContext, editManualLocation, location, location -> {
                ManualMeasureDialog.this.location = location;
                editManualLocation.setText(location.getAddress());
            });
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
