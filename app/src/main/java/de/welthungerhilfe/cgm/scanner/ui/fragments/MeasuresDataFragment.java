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

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;

import de.welthungerhilfe.cgm.scanner.databinding.DialogMeasureMenuBinding;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModelProvideFactory;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerMeasureAdapter;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ManualMeasureDialog;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.MeasureMenuDialog;

public class MeasuresDataFragment extends Fragment implements View.OnClickListener, ManualMeasureDialog.ManualMeasureListener {
    private Context context;
    private SessionManager session;

    private RecyclerMeasureAdapter adapterMeasure;
    private FloatingActionButton fabCreate;

    private CreateDataViewModel viewModel;

    public String qrCode;
    private Person person;

    ViewModelProvider.Factory factory;

    FirebaseAnalytics firebaseAnalytics;


    public static MeasuresDataFragment getInstance(String qrCode) {
        MeasuresDataFragment fragment = new MeasuresDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        this.session = new SessionManager(context);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        factory = new CreateDataViewModelProvideFactory(getActivity());
        viewModel = new ViewModelProvider(getActivity(), factory).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            if(adapterMeasure!=null){
                adapterMeasure.setPerson(person);
            }
        });
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(getActivity());
        viewModel.getMeasuresLiveData().observe(getViewLifecycleOwner(), measures -> {
            if (measures != null)
                adapterMeasure.resetData(measures);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: when coming from automatic scan show ManualMeasureDialog before displaying auto
        View view = inflater.inflate(R.layout.fragment_measure, container, false);

        RecyclerView recyclerMeasure = view.findViewById(R.id.recyclerMeasure);
        recyclerMeasure.setLayoutManager(new LinearLayoutManager(context));
        adapterMeasure = new RecyclerMeasureAdapter((BaseActivity) getActivity(), recyclerMeasure, this);
        recyclerMeasure.setAdapter(adapterMeasure);

        fabCreate = view.findViewById(R.id.fabCreate);
        fabCreate.setOnClickListener(this);

        return view;
    }

    public void createMeasure() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
// ...Irrelevant code for customizing the buttons and title
        LayoutInflater inflater = this.getLayoutInflater();
        DialogMeasureMenuBinding dialogMeasureMenuBinding = DataBindingUtil.inflate(inflater, R.layout.dialog_measure_menu,null,false);

      //  View dialogView = inflater.inflate(R.layout.dialog_measure_menu, null);
        dialogBuilder.setView(dialogMeasureMenuBinding.getRoot());
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
        dialogMeasureMenuBinding.btStartManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ManualMeasureDialog measureDialog = new ManualMeasureDialog(context);
                measureDialog.setManualMeasureListener(MeasuresDataFragment.this);
                measureDialog.setPerson(person);
                measureDialog.show();
                firebaseAnalytics.logEvent(FirebaseService.MANUAL_MEASURE_START,null);
                alertDialog.dismiss();

            }
        });

        dialogMeasureMenuBinding.btStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ScanModeActivity.class);
                intent.putExtra(AppConstants.EXTRA_PERSON, person);
                startActivity(intent);
                alertDialog.dismiss();
            }
        });



       /* MeasureMenuDialog measureMenuDialog = new MeasureMenuDialog();
        measureMenuDialog.show(getActivity().getSupportFragmentManager(),"MeasureMenuDialog");*/
       /* new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                new ContextMenuDialog.Item(R.string.selector_manual, R.drawable.ic_manual),
                new ContextMenuDialog.Item(R.string.selector_scan, R.drawable.ic_machine)
        }, which -> {
            switch (which) {
                case 0:
                    ManualMeasureDialog measureDialog = new ManualMeasureDialog(context);
                    measureDialog.setManualMeasureListener(MeasuresDataFragment.this);
                    measureDialog.setPerson(person);
                    measureDialog.show();
                    firebaseAnalytics.logEvent(FirebaseService.MANUAL_MEASURE_START,null);
                    break;
                case 1:
                    Intent intent = new Intent(getContext(), ScanModeActivity.class);
                    intent.putExtra(AppConstants.EXTRA_PERSON, person);
                    startActivity(intent);
                    break;
            }
        });*/
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fabCreate) {
            if (person == null) {
                Snackbar.make(fabCreate, R.string.error_person_first, Snackbar.LENGTH_LONG).show();
            } else {
                if (person.getCreatedBy().equals(session.getUserEmail()) && person.getEnvironment() == session.getEnvironment()) {
                    createMeasure();
                } else {
                    Snackbar.make(fabCreate, R.string.no_authorazation, Snackbar.LENGTH_LONG).show();
                    String selectedBackend = null;
                    switch (person.getEnvironment()){
                        case AppConstants.ENV_IN_BMZ:
                            selectedBackend = "in_bmz";
                            break;
                        case AppConstants.ENV_NAMIBIA:
                            selectedBackend = "namibia";
                            break;
                        case AppConstants.ENV_NEPAL:
                            selectedBackend = "nepal";
                            break;
                        case AppConstants.ENV_UGANDA:
                            selectedBackend = "uganda";
                            break;
                        case AppConstants.ENV_BAN:
                            selectedBackend = "bangladesh";
                            break;
                        case AppConstants.ENV_DEMO_QA:
                            selectedBackend = "demo_qa";
                            break;
                    }

                    showAlert("this QR code is created by "+person.getCreatedBy()+" in "+selectedBackend+", so only "+person.getCreatedBy()+" can add measure for this QR code.");
                }
            }
        }
    }

    public void showAlert(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onManualMeasure(String id, double height, double weight, double muac, double headCircumference, Loc location, boolean oedema, String measureServerKey) {
        Measure measure = new Measure();
        measure.setId(id != null ? id : AppController.getInstance().getMeasureId());
        measure.setDate(System.currentTimeMillis());
        if(measureServerKey!=null) {
            measure.setMeasureServerKey(measureServerKey);
        }
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setHeight(height);
        measure.setWeight(weight);
        measure.setMuac(muac);
        measure.setHeadCircumference(headCircumference);
        measure.setArtifact("");
        measure.setLocation(location);
        measure.setScannedBy(null);
        measure.setOedema(oedema);
        measure.setType(AppConstants.VAL_MEASURE_MANUAL);
        measure.setPersonId(person.getId());
        measure.setTimestamp(AppController.getInstance().getUniversalTimestamp());
        measure.setDate(AppController.getInstance().getUniversalTimestamp());
        measure.setCreatedBy(session.getUserEmail());
        measure.setQrCode(qrCode);
        measure.setSchema_version(CgmDatabase.version);
        measure.setArtifact_synced(true);
        measure.setEnvironment(person.getEnvironment());
        measure.setSynced(false);
        measure.setStd_test_qr_code(session.getStdTestQrCode());
        person.setLast_updated(System.currentTimeMillis());
        viewModel.savePerson(person);
        viewModel.insertMeasure(measure);
        firebaseAnalytics.logEvent(FirebaseService.MANUAL_MEASURE_STOP,null);
    }

}
