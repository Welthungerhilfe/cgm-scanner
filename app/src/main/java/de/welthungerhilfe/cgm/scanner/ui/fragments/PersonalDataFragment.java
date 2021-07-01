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

import android.app.Activity;
import android.app.DialogFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModelProvideFactory;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.DateEditText;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DateRangePickerDialog.Callback, CompoundButton.OnCheckedChangeListener, DateEditText.DateInputListener, TextWatcher {

    public Context context;
    private SessionManager session;

    private TextView txtDate;

    private AppCompatCheckBox checkAge;

    private DateEditText editBirth;
    private EditText editName, editLocation, editGuardian;

    private AppCompatRadioButton radioFemale, radioMale;

    private Button btnNext;

    private CreateDataViewModel viewModel;
    private String qrCode;
    private Person person;
    private Loc location;

    ViewModelProvider.Factory factory;

    public static PersonalDataFragment getInstance(String qrCode) {
        PersonalDataFragment fragment = new PersonalDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        session = new SessionManager(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        factory = new CreateDataViewModelProvideFactory(getActivity());
        viewModel = new ViewModelProvider(getActivity(), factory).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            initUI();
        });
        viewModel.getLastMeasureLiveData().observe(getViewLifecycleOwner(), measure -> {
            if (measure != null)
                setLocation(measure.getLocation());
        });

        view.findViewById(R.id.rytConsentDetail).setOnClickListener(this);
        view.findViewById(R.id.imgBirth).setOnClickListener(this);

        view.findViewById(R.id.txtBack).setOnClickListener(this);
        btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        checkAge = view.findViewById(R.id.checkAge);

        txtDate = view.findViewById(R.id.txtDate);
        txtDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, System.currentTimeMillis()));

        editName = view.findViewById(R.id.editName);
        editName.addTextChangedListener(this);

        editLocation = view.findViewById(R.id.editLocation);
        editLocation.setOnClickListener(this);

        editBirth = view.findViewById(R.id.editBirth);
        editBirth.setOnDateInputListener(this);

        editGuardian = view.findViewById(R.id.editGuardian);
        editGuardian.addTextChangedListener(this);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioFemale.setOnCheckedChangeListener(this);

        radioMale = view.findViewById(R.id.radioMale);
        radioMale.setOnCheckedChangeListener(this);

        View contextMenu = view.findViewById(R.id.contextMenuButton);
        contextMenu.setOnClickListener(v -> {
            if (person != null) {
                String id = person.getId();
                String qrCode = person.getQrcode();
                BaseActivity activity = (BaseActivity) getActivity();
                new ContextMenuDialog(context, new ContextMenuDialog.Item[] {
                        new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
                }, which -> {
                    ContactSupportDialog.show(activity, "data " + qrCode, "personID:" + id);
                });
            }
        });
        return view;
    }

    public void initUI() {
        if (person == null) {
            setLocation(((CreateDataActivity) getActivity()).location);
            return;
        } else {
            setLocation(person.getLastLocation());
        }

        txtDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, person.getCreated()));

        editName.setText(person.getFullName());
        editBirth.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, person.getBirthday()));
        editGuardian.setText(person.getGuardian());

        String sex = person.getSex();
        if (sex != null) {
            if (sex.equals(AppConstants.VAL_SEX_FEMALE)) {
                radioFemale.setChecked(true);
            } else if (sex.equals(AppConstants.VAL_SEX_MALE)) {
                radioMale.setChecked(true);
            }
        }

        checkAge.setChecked(person.isAgeEstimated());
    }

    public void setLocation(Loc loc) {
        if (loc != null) {
            String address = loc.getAddress();
            if ((location == null) && (address != null) && (address.length() > 0)) {
                if (editLocation != null) {
                    Editable oldAddress = editLocation.getText();
                    if ((oldAddress != null) && (oldAddress.toString().compareTo(address) != 0)) {
                        editLocation.setText(address);
                    }
                }
                location = loc;
            }
        }
    }

    public boolean validate() {
        boolean valid = true;

        String name = editName.getText().toString();
        String birth = editBirth.getText().toString();
        String guardian = editGuardian.getText().toString();

        if (name.isEmpty()) {
            editName.setError(getResources().getString(R.string.tooltip_input, getResources().getString(R.string.name_tooltip)));
            valid = false;
        } else {
            editName.setError(null);
        }

        if (birth.isEmpty()) {
            editBirth.setError(getResources().getString(R.string.tooltip_input, getResources().getString(R.string.birthday_tooltip)));
            valid = false;
        } else if (birth.equals("DD-MM-YYYY")) {
            editBirth.setError(getResources().getString(R.string.tooltip_input, getResources().getString(R.string.birthday_tooltip)));
            valid = false;
        } else {
            editBirth.setError(null);
        }

        if (guardian.isEmpty()) {
            editGuardian.setError(getResources().getString(R.string.tooltip_input, getResources().getString(R.string.guardian_tooltip)));
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
            case R.id.imgBirth:
                long timestamp = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, editBirth.getText().toString());

                DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
                dateRangePicker.setDate(new Date(timestamp));
                dateRangePicker.setCallback(this);
                dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                dateRangePicker.show(getActivity().getFragmentManager(), "DATE_RANGE_PICKER");
                break;
            case R.id.btnNext:
                if (validate()) {
                    long birthday = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, editBirth.getText().toString());

                    String sex = "";
                    if (radioMale.isChecked())
                        sex = "male";
                    else if (radioFemale.isChecked())
                        sex = "female";
                    if (person == null) {
                        person = new Person();
                        person.setId(AppController.getInstance().getPersonId());
                        person.setQrcode(qrCode);
                        person.setEnvironment(session.getEnvironment());
                        person.setCreated(System.currentTimeMillis());
                    }

                    person.setName(editName.getText().toString());
                    person.setBirthday(birthday);
                    person.setGuardian(editGuardian.getText().toString());
                    person.setSex(sex);
                    person.setAgeEstimated(checkAge.isChecked());
                    person.setTimestamp(Utils.getUniversalTimestamp());
                    person.setCreatedBy(session.getUserEmail());
                    person.setSchema_version(CgmDatabase.version);
                    person.setDevice_updated_at_timestamp(System.currentTimeMillis());
                    person.setLastLocation(location);
                    person.setSynced(false);
                    viewModel.savePerson(person);
                }

                break;
            case R.id.rytConsentDetail:

                break;
            case R.id.txtBack:
                getActivity().finish();
                break;
            case R.id.editLocation:
                LocationDetectActivity.navigate((AppCompatActivity) getActivity(), editLocation, location, location -> {
                    setLocation(location);
                });
                break;
        }
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        long birthday = selectedDate.getStartDate().getTimeInMillis();
        editBirth.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, birthday));
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
    public void onDateEntered(String value) {
        InputMethodManager ime = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        ime.hideSoftInputFromWindow(editBirth.getWindowToken(), 0);

        long birthday = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, value);
        editBirth.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, birthday));
        editBirth.clearFocus();
        PersonalDataFragment.this.onTextChanged();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        PersonalDataFragment.this.onTextChanged();
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    private void onTextChanged() {
        String name = editName.getText().toString();
        String birth = editBirth.getText().toString();
        String guardian = editGuardian.getText().toString();

        if (!name.isEmpty() && !birth.isEmpty() && !guardian.isEmpty() && (radioMale.isChecked() || radioFemale.isChecked())) {
            btnNext.setTextColor(getResources().getColor(R.color.colorWhite));
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_green_round));
        } else {
            btnNext.setTextColor(getResources().getColor(R.color.colorGreyDark));
            btnNext.setBackground(getResources().getDrawable(R.drawable.button_grey_round));
        }
    }
}
