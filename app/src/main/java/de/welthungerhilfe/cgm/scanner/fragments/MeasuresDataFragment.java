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

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

//import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;

import com.bumptech.glide.util.Util;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;

import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerMeasureAdapter;
import de.welthungerhilfe.cgm.scanner.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.dialogs.ManualMeasureDialog;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.views.SwipeView;

public class MeasuresDataFragment extends Fragment implements View.OnClickListener, ManualMeasureDialog.OnManualMeasureListener, RecyclerMeasureAdapter.OnMeasureSelectListener {
    private Context context;


    private RecyclerView recyclerMeasure;
    private RecyclerMeasureAdapter adapterMeasure;

    private FloatingActionButton fabCreate;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        adapterMeasure = new RecyclerMeasureAdapter(context, ((CreateDataActivity)context).measures);
        adapterMeasure.setMeasureSelectListener(this);
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
        
        recyclerMeasure = view.findViewById(R.id.recyclerMeasure);
        recyclerMeasure.setAdapter(adapterMeasure);
        recyclerMeasure.setLayoutManager(new LinearLayoutManager(context));

        SwipeView swipeController = new SwipeView(ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT, context) {

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Measure measure = adapterMeasure.getItem(position);

                boolean notAdmin = !AppController.getInstance().isAdmin();

                if (direction == ItemTouchHelper.LEFT) {
                    if (notAdmin && !AppController.getInstance().firebaseConfig.getBoolean(AppConstants.CONFIG_ALLOW_DELETE)) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_delete, Snackbar.LENGTH_LONG).show();
                    } else {
                        ConfirmDialog dialog = new ConfirmDialog(context);
                        dialog.setMessage(R.string.delete_measure);
                        dialog.setConfirmListener(new ConfirmDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm(boolean result) {
                                if (result) {
                                    measure.setDeleted(true);
                                    measure.setDeletedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                                    measure.setTimestamp(Utils.getUniversalTimestamp());
                                    OfflineRepository.getInstance().updateMeasure(measure);

                                    adapterMeasure.removeMeasure(measure);
                                } else {
                                    adapterMeasure.notifyItemChanged(position);
                                }
                            }
                        });
                        dialog.show();
                    }
                } else if (direction == ItemTouchHelper.RIGHT){
                    if (notAdmin && !AppController.getInstance().firebaseConfig.getBoolean(AppConstants.CONFIG_ALLOW_EDIT)) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_edit, Snackbar.LENGTH_LONG).show();
                    } else if (notAdmin && measure.getDate() < Utils.getUniversalTimestamp() - AppController.getInstance().firebaseConfig.getLong(AppConstants.CONFIG_TIME_TO_ALLOW_EDITING) * 3600 * 1000) {
                        adapterMeasure.notifyItemChanged(position);
                        Snackbar.make(recyclerMeasure, R.string.permission_expired, Snackbar.LENGTH_LONG).show();
                    } else {
                        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
                            ManualMeasureDialog dialog = new ManualMeasureDialog(context);
                            dialog.setManualMeasureListener(MeasuresDataFragment.this);
                            dialog.setCloseListener(new ManualMeasureDialog.OnCloseListener() {
                                @Override
                                public void onClose(boolean result) {
                                    adapterMeasure.notifyItemChanged(position);
                                }
                            });
                            dialog.setMeasure(measure);
                            dialog.show();
                        } else {
                            //Intent intent = new Intent(getContext(), RecorderActivity.class);
                            Intent intent = new Intent(getContext(), ScanModeActivity.class);
                            intent.putExtra(AppConstants.EXTRA_PERSON, ((CreateDataActivity)context).person);
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

        fetchRemoteConfig();

        return view;
    }

    private void fetchRemoteConfig() {
        long cacheExpiration = 3600 * 3;
        if (AppController.getInstance().firebaseConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        AppController.getInstance().firebaseConfig.fetch(cacheExpiration)
                .addOnSuccessListener(aVoid -> {
                    AppController.getInstance().firebaseConfig.activateFetched();
                    adapterMeasure.changeManualVisibility();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    public void addMeasure(Measure measure) {
        adapterMeasure.addMeasure(measure);
    }

    public void addMeasures(List<Measure> measures) {
        if (adapterMeasure != null)
            adapterMeasure.addMeasures(measures);
    }

    public void refreshMeasures(List<Measure> measures) {
        if (adapterMeasure != null)
            adapterMeasure.resetData(measures);
    }

    public void createMeasure() {
        if (context == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.title_add_measure);
        builder.setItems(R.array.selector_measure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (which == 0) {
                    ManualMeasureDialog dialog = new ManualMeasureDialog(context);
                    dialog.setManualMeasureListener(MeasuresDataFragment.this);
                    dialog.show();
                } else if (which == 1) {
                    //Intent intent = new Intent(getContext(), RecorderActivity.class);
                    Intent intent = new Intent(getContext(), ScanModeActivity.class);
                    intent.putExtra(AppConstants.EXTRA_PERSON, ((CreateDataActivity)context).person);
                    startActivity(intent);
                }
            }
        });
        builder.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabCreate:
                Crashlytics.log("Add Measure to person");
                if (((CreateDataActivity)context).person == null) {
                    Snackbar.make(fabCreate, R.string.error_person_first, Snackbar.LENGTH_LONG).show();
                } else {
                    createMeasure();
                }
                break;
        }
    }

    @Override
    public void onManualMeasure(double height, double weight, double muac, double headCircumference, Loc location, boolean oedema) {
        ((CreateDataActivity)context).setMeasureData(height, weight, muac, headCircumference,"No Additional Info", location, oedema);
    }

    @Override
    public void onMeasureSelect(Measure measure) {
        ManualMeasureDialog dialog = new ManualMeasureDialog(context);
        dialog.setManualMeasureListener(MeasuresDataFragment.this);
        dialog.setMeasure(measure);
        dialog.setEditable(false);
        dialog.show();
    }
}
