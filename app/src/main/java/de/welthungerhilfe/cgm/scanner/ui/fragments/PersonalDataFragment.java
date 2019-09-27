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

package de.welthungerhilfe.cgm.scanner.ui.fragments;

import android.app.DialogFragment;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DateRangePickerDialog.Callback, CompoundButton.OnCheckedChangeListener, TextWatcher {

    private final int REQUEST_LOCATION = 0x1000;

    public Context context;

    private TextView txtDate;

    private AppCompatCheckBox checkAge;

    private EditText editName, editPrename, editLocation, editBirth, editGuardian;

    private AppCompatRadioButton radioFemale, radioMale;

    private Button btnNext;
    private long birthday = 0;

    private CreateDataViewModel viewModel;
    private String qrCode;
    private Person person;

    public static PersonalDataFragment getInstance(String qrCode) {
        PersonalDataFragment fragment = new PersonalDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
    }
    
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        viewModel = ViewModelProviders.of(getActivity()).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            initUI();
        });
        viewModel.getLastMeasureLiveData().observe(getViewLifecycleOwner(), measure -> {
            if (measure != null)
                showLastLocation(measure);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        view.findViewById(R.id.rytConsentDetail).setOnClickListener(this);
        view.findViewById(R.id.imgBirth).setOnClickListener(this);

        view.findViewById(R.id.txtBack).setOnClickListener(this);
        btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        checkAge = view.findViewById(R.id.checkAge);

        txtDate = view.findViewById(R.id.txtDate);
        txtDate.setText(Utils.beautifyDate(new Date()));

        editName = view.findViewById(R.id.editName);
        editName.addTextChangedListener(this);

        editPrename = view.findViewById(R.id.editPrename);
        editPrename.addTextChangedListener(this);

        editLocation = view.findViewById(R.id.editLocation);

        editBirth = view.findViewById(R.id.editBirth);
        editBirth.addTextChangedListener(this);
        editBirth.setOnClickListener(this);

        editGuardian = view.findViewById(R.id.editGuardian);
        editGuardian.addTextChangedListener(this);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioFemale.setOnCheckedChangeListener(this);

        radioMale = view.findViewById(R.id.radioMale);
        radioMale.setOnCheckedChangeListener(this);

        return view;
    }

    public void initUI() {
        if (person == null)
            return;

        txtDate.setText(Utils.beautifyDate(person.getCreated()));

        editName.setText(person.getName());
        editPrename.setText(person.getSurname());
        editBirth.setText(Utils.beautifyDate(person.getBirthday()));
        editGuardian.setText(person.getGuardian());

        if (person.getSex().equals(AppConstants.VAL_SEX_FEMALE)) {
            radioFemale.setChecked(true);
        } else if (person.getSex().equals(AppConstants.VAL_SEX_MALE)) {
            radioMale.setChecked(true);
        }

        checkAge.setChecked(person.isAgeEstimated());
    }

    private void showLastLocation(Measure measure) {
        if (measure.getLocation() != null)
            editLocation.setText(measure.getLocation().getAddress());
    }

    public boolean validate() {
        boolean valid = true;

        String name = editName.getText().toString();
        String prename = editPrename.getText().toString();
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

        if (birth.isEmpty()) {
            editBirth.setError(getResources().getString(R.string.tooltip_birthday));
            valid = false;
        } else {
            editBirth.setError(null);
        }

        if (guardian.isEmpty()) {
            editGuardian.setError(getResources().getString(R.string.tooltip_guardian));
            valid = false;
        } else {
            editGuardian.setError(null);
        }

        if (!radioFemale.isChecked() && !radioMale.isChecked()) {
            valid = false;
            Snackbar.make(radioFemale, R.string.tooltipe_sex, Snackbar.LENGTH_SHORT).show();
        }

        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
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

                    if (person == null) {
                        person = new Person();
                        person.setId(AppController.getInstance().getPersonId());
                        person.setQrCode(qrCode);
                    }

                    person.setName(editName.getText().toString());
                    person.setSurname(editPrename.getText().toString());
                    if (birthday != 0)
                        person.setBirthday(birthday);
                    person.setGuardian(editGuardian.getText().toString());
                    person.setSex(sex);
                    person.setAgeEstimated(checkAge.isChecked());
                    person.setTimestamp(Utils.getUniversalTimestamp());
                    person.setCreated(System.currentTimeMillis());
                    person.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());

                    viewModel.savePerson(person);
                }

                break;
            case R.id.rytConsentDetail:

                break;
            case R.id.txtBack:
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        birthday = selectedDate.getStartDate().getTimeInMillis();
        editBirth.setText(Utils.beautifyDate(selectedDate.getStartDate().getTimeInMillis()));
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
