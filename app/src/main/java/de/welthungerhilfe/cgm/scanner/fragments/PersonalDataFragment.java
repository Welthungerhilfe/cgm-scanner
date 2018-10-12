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

package de.welthungerhilfe.cgm.scanner.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.activities.ImageDetailActivity;
import de.welthungerhilfe.cgm.scanner.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DateRangePickerDialog.Callback, CompoundButton.OnCheckedChangeListener, TextWatcher {

    private String TAG = this.getClass().getSimpleName();

    private final int REQUEST_LOCATION = 0x1000;

    public Context context;

    private LinearLayout lytCreate;

    private ImageView imgConsent, imgBirth, imgLocation;

    private TextView txtDate;

    private AppCompatCheckBox checkAge;

    private EditText editName, editPrename, editLocation, editBirth, editGuardian;

    private AppCompatRadioButton radioFemale, radioMale/*, radioFluid*/;

    private Button btnNext;

    private Loc location = null;
    private long birthday = 0;
    
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        view.findViewById(R.id.rytConsentDetail).setOnClickListener(this);
        view.findViewById(R.id.imgBirth).setOnClickListener(this);

        /*
        TODO fix
        imgLocation = view.findViewById(R.id.imgLocation);
        imgLocation.setOnClickListener(this);
        */

        view.findViewById(R.id.txtBack).setOnClickListener(this);
        btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        lytCreate = view.findViewById(R.id.lytCreate);

        imgConsent = view.findViewById(R.id.imgConsent);

        checkAge = view.findViewById(R.id.checkAge);

        txtDate = view.findViewById(R.id.txtDate);

        editName = view.findViewById(R.id.editName);
        editName.addTextChangedListener(this);

        editPrename = view.findViewById(R.id.editPrename);
        editPrename.addTextChangedListener(this);

        editLocation = view.findViewById(R.id.editLocation);
        //editLocation.addTextChangedListener(this);

        editBirth = view.findViewById(R.id.editBirth);
        editBirth.addTextChangedListener(this);
        editBirth.setOnClickListener(this);

        editGuardian = view.findViewById(R.id.editGuardian);
        editGuardian.addTextChangedListener(this);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioFemale.setOnCheckedChangeListener(this);

        radioMale = view.findViewById(R.id.radioMale);
        radioMale.setOnCheckedChangeListener(this);
        //radioFluid = view.findViewById(R.id.radioFluid);

        initUI();
        showConsent();

        return view;
    }

    public void initUI() {
        if (context == null || txtDate == null)
            return;

        if (((CreateDataActivity)context).person != null) {
            txtDate.setText(Utils.beautifyDate(((CreateDataActivity)context).person.getCreated()));

            editName.setText(((CreateDataActivity)context).person.getName());
            editPrename.setText(((CreateDataActivity)context).person.getSurname());
            editBirth.setText(Utils.beautifyDate(((CreateDataActivity)context).person.getBirthday()));
            editGuardian.setText(((CreateDataActivity)context).person.getGuardian());

            if (((CreateDataActivity)context).person.getLastLocation() != null) {
                editLocation.setText(((CreateDataActivity)context).person.getLastLocation().getAddress());
                //imgLocation.setVisibility(View.VISIBLE);
            }

            if (((CreateDataActivity)context).person.getSex().equals(AppConstants.VAL_SEX_FEMALE)) {
                radioFemale.setChecked(true);
            } else if (((CreateDataActivity)context).person.getSex().equals(AppConstants.VAL_SEX_MALE)) {
                radioMale.setChecked(true);
            } /*else if (((CreateDataActivity)context).person.getSex().equals(AppConstants.VAL_SEX_OTHER)) {
                radioFluid.setChecked(true);
            }*/

            checkAge.setChecked(((CreateDataActivity)context).person.isAgeEstimated());
        } else {
            txtDate.setText(Utils.beautifyDate(new Date()));

            byte[] data = ((CreateDataActivity)context).qrBitmapByteArray;
            if (data != null) {
                imgConsent.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
            }
        }
    }

    public boolean validate() {
        boolean valid = true;

        String name = editName.getText().toString();
        String prename = editPrename.getText().toString();
        // TODO fix String location = editLocation.getText().toString();
        String birth = editBirth.getText().toString();
        String guardian = editGuardian.getText().toString();

        if (name.isEmpty()) {
            editName.setError(getResources().getString(R.string.tooltip_name));
            valid = false;
        } else {
            editName.setError(null);
        }

        if (prename.isEmpty()) {
            editPrename.setError(getResources().getString(R.string.tooltip_prename));
            valid = false;
        } else {
            editPrename.setError(null);
        }

        /*
        if (location.isEmpty()) {
            editLocation.setError(getResources().getString(R.string.tooltip_location);
            valid = false;
        } else {
            editLocation.setError(null);
        }
        */

        if (birth.isEmpty()) {
            editBirth.setError(getResources().getString(R.string.tooltip_birthday));
            valid = false;
        } else {
            editBirth.setError(null);
        }

        /*
        if (age.isEmpty()) {
            editAge.setError(getResources().getString(R.string.tooltip_age));
            valid = false;
        } else {
            editName.setError(null);
        }
        */

        if (guardian.isEmpty()) {
            editGuardian.setError(getResources().getString(R.string.tooltip_guardian));
            valid = false;
        } else {
            editGuardian.setError(null);
        }

        if (radioFemale.isChecked() || radioMale.isChecked() /* || radioFluid.isChecked() */) {

        } else {
            valid = false;
            Snackbar.make(radioFemale, R.string.tooltipe_sex, Snackbar.LENGTH_SHORT).show();
        }

        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            /* TODO fix
            case R.id.imgLocation:
                startActivityForResult(new Intent(context, LocationDetectActivity.class), REQUEST_LOCATION);
                break;
                */
            case R.id.editBirth:
                DateRangePickerDialog pickerDialog = new DateRangePickerDialog();
                pickerDialog.setCallback(this);
                pickerDialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                pickerDialog.show(getActivity().getFragmentManager(), "DATE_RANGE_PICKER");

                break;
            case R.id.imgBirth:
                DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
                dateRangePicker.setCallback(this);
                dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                dateRangePicker.show(getActivity().getFragmentManager(), "DATE_RANGE_PICKER");

                break;
            case R.id.btnNext:
                if (validate()) {

                    String sex = "";
                    if (radioMale.isChecked())
                        sex = radioMale.getText().toString();
                    else if (radioFemale.isChecked())
                        sex = radioFemale.getText().toString();
                    /*
                    else if (radioFluid.isChecked())
                        sex = radioFluid.getText().toString();
                    */

                    ((CreateDataActivity)context).setPersonalData(
                            editName.getText().toString(),
                            editPrename.getText().toString(),
                            birthday,
                            checkAge.isChecked(),
                            sex, location, editGuardian.getText().toString());
                }

                break;
            case R.id.rytConsentDetail:
                Intent intent = new Intent(context, ImageDetailActivity.class);
                if (((CreateDataActivity)context).qrCode != null) {
                    intent.putExtra(AppConstants.EXTRA_QR_BITMAP, ((CreateDataActivity)context).qrBitmapByteArray);
                } else if (((CreateDataActivity)context).consents.size() > 0) {
                    intent.putExtra(AppConstants.EXTRA_QR_URL, ((CreateDataActivity)context).consents.get(0).getConsent());
                }

                startActivity(intent);
                break;
            case R.id.txtBack:
                getActivity().finish();
                break;
        }
    }

    public void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            location = (Loc)data.getSerializableExtra(AppConstants.EXTRA_LOCATION);
            // TODO fix editLocation.setText(location.getAddress());
        }
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        birthday = selectedDate.getStartDate().getTimeInMillis();
        editBirth.setText(Utils.beautifyDate(selectedDate.getStartDate().getTimeInMillis()));
    }

    public void showConsent() {
        if (context == null)
            return;

        if (((CreateDataActivity)context).qrCode != null) {
            imgConsent.setImageBitmap(BitmapFactory.decodeByteArray(((CreateDataActivity)context).qrBitmapByteArray, 0, ((CreateDataActivity)context).qrBitmapByteArray.length));
        } else if (((CreateDataActivity)context).consents.size() > 0) {
            final String qrUrl = ((CreateDataActivity)context).consents.get(0).getConsent();

            long created = ((CreateDataActivity)context).consents.get(0).getCreated();
            String qrcode = ((CreateDataActivity)context).consents.get(0).getQrcode();
            String thumbUrl = "/data/person/"+qrcode+"/"+created+"_"+qrcode+".png_thumb.png";
            Log.v(TAG,"thumbUrl: "+thumbUrl);
            StorageReference qrThumbRef = AppController.getInstance().firebaseStorage.getReference(thumbUrl);

            qrThumbRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
            {
                @Override
                public void onSuccess(Uri downloadUrl)
                {
                    Log.v(TAG,"found thumbnail at: " + downloadUrl.toString());
                    if (context != null)
                        Glide.with(context).load(downloadUrl.toString()).into(imgConsent);
                    //do something with downloadurl
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG,"error getting thumbnail");
                    e.printStackTrace();
                    if (context != null)
                        Glide.with(context).load(qrUrl).into(imgConsent);
                }
            });
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (validate()) {
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_green_round));
            btnNext.setTextColor(getResources().getColor(R.color.colorWhite));
        } else {
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_grey_round));
            btnNext.setTextColor(getResources().getColor(R.color.colorGreyDark));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String name = editName.getText().toString();
        String prename = editPrename.getText().toString();
        String birth = editBirth.getText().toString();
        String guardian = editGuardian.getText().toString();

        if (!name.isEmpty() && !prename.isEmpty() && !birth.isEmpty() && !guardian.isEmpty() && (radioMale.isChecked() || radioFemale.isChecked())) {
            btnNext.setTextColor(getResources().getColor(R.color.colorWhite));
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_green_round));
        } else {
            btnNext.setTextColor(getResources().getColor(R.color.colorGreyDark));
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_grey_round));
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
