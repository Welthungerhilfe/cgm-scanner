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

import static android.app.Activity.RESULT_OK;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.location.india.IndiaLocation;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.IndiaLocationRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModelProvideFactory;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.BitmapHelper;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.QRScanActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog1;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.LocationDialogFragment;
import de.welthungerhilfe.cgm.scanner.ui.views.DateEditText;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DateRangePickerDialog1.Callback, CompoundButton.OnCheckedChangeListener, DateEditText.DateInputListener, TextWatcher, LocationDialogFragment.PassDataToPersonDataFragment {

    public Context context;
    private SessionManager session;

    private TextView txtDate;

    private AppCompatCheckBox checkAge;

    private DateEditText editBirth;
    private EditText editName, editLocation, editGuardian, editArea, editCenter;

    private AppCompatRadioButton radioFemale, radioMale;

    private Button btnNext;

    private CreateDataViewModel viewModel;
    private String qrCode;
    private Person person;
    private Loc  location;
    LinearLayout ll_retake_photo;
    ViewModelProvider.Factory factory;
    FirebaseAnalytics firebaseAnalytics;

    LinearLayout ll_village, ll_aganwadi;

    private static final int IMAGE_CAPTURED_REQUEST = 100;

    boolean ageAlertShown = false;

    String TAG = PersonalDataFragment.class.getSimpleName();

    FileLogRepository fileLogRepository;

    IndiaLocationRepository indiaLocationRepository;
    String center_location_id;

    String location_id;

    LocationDialogFragment.PassDataToPersonDataFragment passDataToPersonDataFragment;

    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;


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
        fileLogRepository = FileLogRepository.getInstance(getActivity());
        indiaLocationRepository = IndiaLocationRepository.getInstance(getActivity());
        viewModel = new ViewModelProvider(getActivity(), factory).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode,session.getEnvironment()).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            initUI();
        });
        viewModel.getLastMeasureLiveData().observe(getViewLifecycleOwner(), measure -> {
            if (measure != null)
                setLocation(measure.getLocation());
        });
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(getActivity());

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

        editArea = view.findViewById(R.id.editArea);
        editArea.setOnClickListener(this);
        editArea.addTextChangedListener(this);

        editCenter = view.findViewById(R.id.editCenter);
        editCenter.setOnClickListener(this);
        editCenter.addTextChangedListener(this);

        ll_aganwadi = view.findViewById(R.id.ll_aaganwadi);
        ll_village = view.findViewById(R.id.ll_village);

        if(session.getEnvironment() == AppConstants.ENV_DEMO_QA || session.getEnvironment() == AppConstants.ENV_IN_BMZ){
            ll_aganwadi.setVisibility(View.VISIBLE);
            ll_village.setVisibility(view.VISIBLE);
        }else {
            ll_aganwadi.setVisibility(View.GONE);
            ll_village.setVisibility(view.GONE);
        }

        editBirth = view.findViewById(R.id.editBirth);
        editBirth.setOnDateInputListener(this);

        editGuardian = view.findViewById(R.id.editGuardian);
        editGuardian.addTextChangedListener(this);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioFemale.setOnCheckedChangeListener(this);

        radioMale = view.findViewById(R.id.radioMale);
        radioMale.setOnCheckedChangeListener(this);


        scannerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), this::handleActivityResult);
        ll_retake_photo = view.findViewById(R.id.ll_retake_photo);
        View contextMenu = view.findViewById(R.id.contextMenuButton);
        contextMenu.setOnClickListener(v -> {
            if (person != null) {
                String id = person.getId();
                String qrCode = person.getQrcode();
                BaseActivity activity = (BaseActivity) getActivity();
                new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                        new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
                }, which -> {
                    ContactSupportDialog.show(activity, "data " + qrCode, "personID:" + id);
                });
            }
        });

        ll_retake_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                  requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                  return;
                }
                firebaseAnalytics.logEvent(FirebaseService.SCAN_INFORM_CONSENT_START, null);
                //startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), IMAGE_CAPTURED_REQUEST);

                GmsDocumentScannerOptions.Builder options =
                        new GmsDocumentScannerOptions.Builder()
                                .setResultFormats(
                                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                                .setGalleryImportAllowed(false);


                options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL);
                options.setPageLimit(1);



                GmsDocumentScanning.getClient(options.build())
                        .getStartScanIntent(getActivity())
                        .addOnSuccessListener(new OnSuccessListener<IntentSender>() {
                            @Override
                            public void onSuccess(IntentSender intentSender) {

                            }
                        })
                        .addOnSuccessListener(
                                intentSender ->
                                        scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                        .addOnFailureListener(
                                e -> Toast.makeText(getActivity(),"Error",Toast.LENGTH_LONG).show());
            }
            });


        return view;
    }

    private void handleActivityResult(ActivityResult activityResult) {

        int resultCode = activityResult.getResultCode();
        GmsDocumentScanningResult result =
                GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
        if (result.getPages() != null) {
            File file = new File(result.getPages().get(0).getImageUri().getPath());



            ImageSaver(file, getActivity());
        }

    }
  /*  @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_CAPTURED_REQUEST && resultCode == RESULT_OK) {
            try {

                Bitmap capturedImageBitmap = (Bitmap) data.getExtras().get("data");
                capturedImageBitmap = BitmapHelper.getAcceptableBitmap(capturedImageBitmap);
                ImageSaver(capturedImageBitmap, getActivity());
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

    public void initUI() {
        if (BuildConfig.DEBUG) {
            editArea.setText("debug");
            editCenter.setText("debug");

        }
        if (person == null) {
            firebaseAnalytics.logEvent(FirebaseService.CREATE_PERSON_START, null);
            setLocation(((CreateDataActivity) getActivity()).location);
            return;
        } else {
            setLocation(person.getLastLocation());
        }

        txtDate.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, person.getCreated()));

        editName.setText(person.getFullName());
        if(person.getCenter_location_id()!=null && !BuildConfig.DEBUG){
            IndiaLocation indiaLocation = indiaLocationRepository.getLocationFromId(person.getCenter_location_id(),session.getEnvironment());
            editArea.setText(indiaLocation.getVillage_full_name());
            editCenter.setText(indiaLocation.getAganwadi());
        }
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

            Log.i(TAG,"this is value locality "+loc.getLocality());


            setVillageDefault(loc.getLocality());
           // if ((location == null) && (address != null) && (address.length() > 0)) {
                if ((address != null) && (address.length() > 0)) {

                    if (editLocation != null) {
                    Editable oldAddress = editLocation.getText();
                    if ((oldAddress != null) && (oldAddress.toString().compareTo(address) != 0)) {
                        editLocation.setText(address);
                        if(person==null){
                            Log.i(TAG,"this is value locality "+loc.getLocality());
                        }
                        else{
                            if(person.getCenter_location_id()==null){

                            }
                        }

                    }
                }
                location = loc;
            }
        }
    }

    public void setVillageDefault(String village){
        Log.i(TAG,"this is value locality "+village);

        if(village==null){
            return;
        }
        IndiaLocation indiaLocation = indiaLocationRepository.getVillageObject(village.toUpperCase(Locale.ENGLISH),session.getEnvironment());
       if(indiaLocation!=null) {
           Log.i(TAG, "this is value locality " + indiaLocation);
       }

        if(indiaLocation != null){
            editArea.setText(indiaLocation.getVillage_full_name());

        }
    }

    public boolean validate() {
        boolean valid = true;

        String name = editName.getText().toString();
        String birth = editBirth.getText().toString();
        String guardian = editGuardian.getText().toString();
        String area = editArea.getText().toString();
        String aganwadi = editCenter.getText().toString();

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



        if(session.getEnvironment() == AppConstants.ENV_DEMO_QA || session.getEnvironment() == AppConstants.ENV_IN_BMZ){
            if (area.isEmpty()) {
                editArea.setError("Please search a village");
                valid = false;
            } else {
                editArea.setError(null);
            }


            if (aganwadi.isEmpty()) {
                editCenter.setError("Please select aganwadi");
                valid = false;
            } else {
                editCenter.setError(null);
            }


        }
        else {

        }


        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgBirth:
                long timestamp = DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, editBirth.getText().toString());

                DateRangePickerDialog1 dateRangePicker = new DateRangePickerDialog1();
                dateRangePicker.setDate(new Date(timestamp));
                dateRangePicker.setCallback(this);
               // dateRangePicker.setStyle(DI.STYLE_NO_TITLE, 0);
                dateRangePicker.show(getActivity().getSupportFragmentManager(), "DATE_RANGE_PICKER");
                break;
            case R.id.btnNext:
                if (validate()) {
                    String birth = editBirth.getText().toString();
                    if (birth != null && !birth.isEmpty() && !ageAlertShown) {
                        int months = DataFormat.monthsBetweenDates(birth);
                        if (months < 7 || months > 60) {
                            ageAlertShown = true;
                            showAgeAlertDialog();
                            return;
                        }
                    }

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
                        if(session.getSelectedMode() == AppConstants.CGM_MODE){
                            person.setBelongs_to_rst(false);
                        }else {
                            person.setBelongs_to_rst(true);
                        }
                        firebaseAnalytics.logEvent(FirebaseService.CREATE_PERSON_STOP, null);

                    }

                    person.setName(editName.getText().toString());
                    person.setBirthday(birthday);
                    person.setGuardian(editGuardian.getText().toString());
                    person.setSex(sex);
                    person.setAgeEstimated(checkAge.isChecked());
                    person.setTimestamp(AppController.getInstance().getUniversalTimestamp());
                    person.setCreatedBy(session.getUserEmail());
                    person.setSchema_version(CgmDatabase.version);
                    person.setDevice_updated_at_timestamp(System.currentTimeMillis());
                    person.setLastLocation(location);
                    person.setSynced(false);
                    person.setCenter_location_id(center_location_id);
                    person.setLocation_id(location_id);
                    if (BuildConfig.DEBUG) {
                        person.setCenter_location_id("1111");
                        person.setLocation_id("115566");
                    }
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
                    Log.i(TAG,"this is location value "+location.getLocality());
                });
                break;
            case R.id.editArea:

                LocationDialogFragment dialogFragment = LocationDialogFragment.newInstance(null,passDataToPersonDataFragment);

                dialogFragment.setTargetFragment(PersonalDataFragment.this,1000);
                dialogFragment.show(getActivity().getSupportFragmentManager(), "location_dialog");
                break;

            case R.id.editCenter:

                if(editArea.getText() == null || editArea.getText().toString().isEmpty()){
                    editArea.setError("Please search a village");
                    return;
                }

                LocationDialogFragment dialogFragment1 = LocationDialogFragment.newInstance(editArea.getText().toString(),passDataToPersonDataFragment);

                dialogFragment1.setTargetFragment(PersonalDataFragment.this,1001);
                dialogFragment1.show(getActivity().getSupportFragmentManager(), "location_dialog");
                break;
        }
    }

    @Override
    public void onDateTimeRecurrenceSet(Date selectedDate, int hourOfDay, int minute, String recurrenceOption, String recurrenceRule) {
        long birthday = selectedDate.getTime();
        editBirth.setText(DataFormat.timestamp(getContext(), DataFormat.TimestampFormat.DATE, birthday));
        ageAlertShown = false;
        PersonalDataFragment.this.onTextChanged();

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
        ageAlertShown = false;
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
        String area = editArea.getText().toString();
        String center = editCenter.getText().toString();

        if(session.getEnvironment() == AppConstants.ENV_DEMO_QA || session.getEnvironment() == AppConstants.ENV_IN_BMZ){
            if (!name.isEmpty() && !birth.isEmpty() && !guardian.isEmpty() && (radioMale.isChecked() || radioFemale.isChecked())) {
                btnNext.setTextColor(getResources().getColor(R.color.colorWhite));
                btnNext.setBackground(getResources().getDrawable(R.drawable.button_green_round));
            } else {
                btnNext.setTextColor(getResources().getColor(R.color.colorGreyDark));
                btnNext.setBackground(getResources().getDrawable(R.drawable.button_grey_round));
            }
        }
        else {
            if (!name.isEmpty() && !birth.isEmpty() && !guardian.isEmpty() && (radioMale.isChecked() || radioFemale.isChecked()) && area.isEmpty() && center.isEmpty()) {
                btnNext.setTextColor(getResources().getColor(R.color.colorWhite));
                btnNext.setBackground(getResources().getDrawable(R.drawable.button_green_round));
            } else {
                btnNext.setTextColor(getResources().getColor(R.color.colorGreyDark));
                btnNext.setBackground(getResources().getDrawable(R.drawable.button_grey_round));
            }
        }

    }

    public void showAgeAlertDialog() {
        ageAlertShown = true;
        new AlertDialog.Builder(context)
                .setMessage(R.string.age_warning)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        btnNext.performClick();
                        dialog.dismiss();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editBirth.setText("");
                onTextChanged();
                dialog.dismiss();
            }
        }).show();
    }

    void ImageSaver(File consentFile, Context context) {


        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                final long timestamp = AppController.getInstance().getUniversalTimestamp();

                Bitmap bitmap = BitmapFactory.decodeFile(consentFile.getAbsolutePath());
            /*    final String consentFileString = timestamp + "_" + qrCode + ".jpg";

                File extFileDir = AppController.getInstance().getRootDirectory(context);
                File consentFileFolder = new File(extFileDir, AppConstants.LOCAL_CONSENT_URL.replace("{qrcode}", qrCode).replace("{scantimestamp}", String.valueOf(timestamp)));
                File consentFile = new File(consentFileFolder, consentFileString);
                if (!consentFileFolder.exists()) {
                    boolean created = consentFileFolder.mkdirs();
                    if (created) {
                        Log.i(TAG, "Folder: \"" + consentFileFolder + "\" created\n");
                    } else {
                        Log.e(TAG, "Folder: \"" + consentFileFolder + "\" could not be created!\n");
                    }

                    BitmapHelper.writeBitmapToFile(data1, consentFile);


                }*/

                BitmapHelper.writeBitmapToFile(bitmap, consentFile);


                FileOutputStream output = null;
                try {
                    FileLog log = new FileLog();
                    log.setId(AppController.getInstance().getArtifactId("consent"));
                    log.setType("consent");
                    log.setPath(consentFile.getPath());
                    log.setHashValue(FileSystem.getMD5(consentFile.getPath()));
                    log.setFileSize(consentFile.length());
                    log.setUploadDate(0);
                    log.setQrCode(qrCode);
                    log.setDeleted(false);
                    log.setCreateDate(AppController.getInstance().getUniversalTimestamp());
                    log.setCreatedBy(new SessionManager(getActivity()).getUserEmail());
                    log.setEnvironment(new SessionManager(getActivity()).getEnvironment());
                    log.setSchema_version(CgmDatabase.version);
                    fileLogRepository.insertFileLog(log);
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;

            }


            @Override
            protected void onPostExecute(Boolean flag) {
                super.onPostExecute(flag);
                if (flag) {
                    firebaseAnalytics.logEvent(FirebaseService.SCAN_INFORM_CONSENT_STOP, null);
                    Toast.makeText(context,R.string.consent_upload_successfully, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }


            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


    }

    @Override
    public void onPassDate(String area, String data) {
        if(area==null){
            editArea.setText(data);
            if(data!=null){
                editArea.setError(null);
            }
            editArea.setError(null);
        }
        else{
            editCenter.setText(data);
            IndiaLocation indiaLocation = indiaLocationRepository.getCenterLocationId(area,data,session.getEnvironment());
            center_location_id = indiaLocation.getId();
            location_id = indiaLocation.getLocation_id();
            if(data!=null){
                editCenter.setError(null);
            }

        }
    }


}
