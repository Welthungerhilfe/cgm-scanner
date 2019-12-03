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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

//import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerMeasureAdapter;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.FeedbackDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ManualDetailDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ManualMeasureDialog;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.ui.views.SwipeView;

public class MeasuresDataFragment extends Fragment implements View.OnClickListener, ManualMeasureDialog.OnManualMeasureListener, RecyclerMeasureAdapter.OnMeasureSelectListener, RecyclerMeasureAdapter.OnMeasureFeedbackListener {
    private Context context;

    private SessionManager session;
    private RemoteConfig config;

    private RecyclerView recyclerMeasure;
    private RecyclerMeasureAdapter adapterMeasure;

    private FloatingActionButton fabCreate;

    private ManualMeasureDialog measureDialog;
    private ManualDetailDialog detailDialog;
    private FeedbackDialog feedbackDialog;

    private CreateDataViewModel viewModel;

    public String qrCode;
    private Person person;

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
        config = session.getRemoteConfig();
    }

    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        viewModel = ViewModelProviders.of(getActivity()).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
        });
        viewModel.getMeasuresLiveData().observe(getViewLifecycleOwner(), measures -> {
            if (measures != null)
                adapterMeasure.resetData(measures);
        });
    }

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

        adapterMeasure = new RecyclerMeasureAdapter(context);
        adapterMeasure.setMeasureSelectListener(this);
        adapterMeasure.setMeasureFeedbackListener(this);

        recyclerMeasure = view.findViewById(R.id.recyclerMeasure);
        recyclerMeasure.setAdapter(adapterMeasure);
        recyclerMeasure.setLayoutManager(new LinearLayoutManager(context));

        SwipeView swipeController = new SwipeView(ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT, context) {

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Measure measure = adapterMeasure.getItem(position);

                if (direction == ItemTouchHelper.LEFT) {
                    if (!config.isAllow_delete()) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_delete, Snackbar.LENGTH_LONG).show();
                    } else {
                        ConfirmDialog dialog = new ConfirmDialog(context);
                        dialog.setMessage(R.string.delete_measure);
                        dialog.setConfirmListener(result -> {
                            if (result) {
                                measure.setDeleted(true);
                                measure.setDeletedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                                measure.setTimestamp(Utils.getUniversalTimestamp());
                                // ToDo: Write new code to update measure
                                //OfflineRepository.getInstance(getContext()).updateMeasure(measure);

                                adapterMeasure.removeMeasure(measure);
                            } else {
                                adapterMeasure.notifyItemChanged(position);
                            }
                        });
                        dialog.show();
                    }
                } else if (direction == ItemTouchHelper.RIGHT){
                    if (!config.isAllow_edit()) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_edit, Snackbar.LENGTH_LONG).show();
                    } else if (measure.getDate() < Utils.getUniversalTimestamp() - config.getTime_to_allow_editing() * 3600 * 1000) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_expired, Snackbar.LENGTH_LONG).show();
                    } else {
                        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
                            if (measureDialog == null)
                                measureDialog = new ManualMeasureDialog(context);
                            measureDialog.setManualMeasureListener(MeasuresDataFragment.this);
                            measureDialog.setCloseListener(result -> adapterMeasure.notifyItemChanged(position));
                            measureDialog.setMeasure(measure);
                            measureDialog.show();
                        } else {
                            Intent intent = new Intent(getContext(), ScanModeActivity.class);
                            intent.putExtra(AppConstants.EXTRA_PERSON, person);
                            intent.putExtra(AppConstants.EXTRA_MEASURE, measure);
                            startActivity(intent);
                            adapterMeasure.notifyItemChanged(position);
                        }
                    }
                }
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerMeasure);

        fabCreate = view.findViewById(R.id.fabCreate);
        fabCreate.setOnClickListener(this);

        return view;
    }

    public void createMeasure() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.title_add_measure);
            builder.setItems(R.array.selector_measure, (d, which) -> {
                if (which == 0) {
                    if (measureDialog == null)
                        measureDialog = new ManualMeasureDialog(context);
                    measureDialog.setManualMeasureListener(MeasuresDataFragment.this);
                    measureDialog.show();
                } else if (which == 1) {
                    Intent intent = new Intent(getContext(), ScanModeActivity.class);
                    intent.putExtra(AppConstants.EXTRA_PERSON, person);
                    startActivity(intent);
                }
            });
            builder.show();
        } catch (RuntimeException e) {
            // todo: Crashlytics.log(0, "measure fragment", e.getMessage());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabCreate:
                if (person == null) {
                    Snackbar.make(fabCreate, R.string.error_person_first, Snackbar.LENGTH_LONG).show();
                } else {
                    createMeasure();
                }
                break;
        }
    }

    @Override
    public void onManualMeasure(String id, double height, double weight, double muac, double headCircumference, Loc location, boolean oedema) {
        Measure measure = new Measure();
        measure.setId(id != null ? id : AppController.getInstance().getMeasureId());
        measure.setDate(System.currentTimeMillis());
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setHeight(height);
        measure.setWeight(weight);
        measure.setMuac(muac);
        measure.setHeadCircumference(headCircumference);
        measure.setArtifact("");
        measure.setLocation(location);
        measure.setOedema(oedema);
        measure.setType(AppConstants.VAL_MEASURE_MANUAL);
        measure.setPersonId(person.getId());
        measure.setTimestamp(Utils.getUniversalTimestamp());
        measure.setDate(Utils.getUniversalTimestamp());
        measure.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
        measure.setQrCode(qrCode);
        measure.setSchema_version(CgmDatabase.version);

        viewModel.insertMeasure(measure);
    }

    @Override
    public void onMeasureSelect(Measure measure) {
        if (detailDialog == null)
            detailDialog = new ManualDetailDialog(context);
        detailDialog.setMeasure(measure);
        detailDialog.show();
    }

    @Override
    public void onMeasureFeedback(Measure measure, double overallScore) {
        if (feedbackDialog == null)
            feedbackDialog = new FeedbackDialog(context);

        feedbackDialog.setMeasure(measure);
        feedbackDialog.show();
    }
}
