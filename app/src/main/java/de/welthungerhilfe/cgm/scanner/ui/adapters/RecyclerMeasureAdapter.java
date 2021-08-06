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
package de.welthungerhilfe.cgm.scanner.ui.adapters;

import android.annotation.SuppressLint;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ManualDetailDialog;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ManualMeasureDialog;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerMeasureAdapter extends RecyclerView.Adapter<RecyclerMeasureAdapter.ViewHolder> {
    private BaseActivity context;
    private ManualMeasureDialog.ManualMeasureListener manualMeasureListener;
    private List<Measure> measureList;

    private FileLogRepository artifactRepository;

    private SessionManager session;
    private RecyclerView recyclerMeasure;
    private RemoteConfig config;
    private Person person;

    public RecyclerMeasureAdapter(BaseActivity ctx, RecyclerView recycler, ManualMeasureDialog.ManualMeasureListener listener) {
        context = ctx;
        manualMeasureListener = listener;
        recyclerMeasure = recycler;

        artifactRepository = FileLogRepository.getInstance(context);

        measureList = new ArrayList<>();

        session = new SessionManager(context);
        config = session.getRemoteConfig();
    }

    @Override
    public int getItemViewType(int position) {
        Measure measure = measureList.get(position);
        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL))
            return 0;
        else
            return 1;
    }

    @Override
    public RecyclerMeasureAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_measure, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onBindViewHolder(@NonNull RecyclerMeasureAdapter.ViewHolder holder, int position) {
        Measure measure = measureList.get(position);

        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            holder.imgType.setImageResource(R.drawable.manual);
        } else {
            holder.imgType.setImageResource(R.drawable.machine);

            final String measureId = measure.getId();
            Observer<UploadStatus> statusObserver = status -> {
                double progress = status.getUploaded() / status.getTotal() * 100;
                if (progress >= 100) {
                    holder.progressUpload.setVisibility(View.GONE);
                    artifactRepository.getMeasureUploadProgress(measureId).removeObservers((LifecycleOwner) context);
                } else {
                    holder.progressUpload.setVisibility(View.VISIBLE);
                    holder.progressUpload.setProgress((int) progress);
                }
            };
            artifactRepository.getMeasureUploadProgress(measureId).observe((LifecycleOwner) context, statusObserver);
        }

        if (measure.isOedema()) {
            holder.rytItem.setBackgroundResource(R.color.colorLightPink);
        } else {
            holder.rytItem.setBackgroundResource(R.color.colorWhite);
        }

        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            if (measure.isSynced()) {
                holder.iv_is_synced_corner.setVisibility(View.VISIBLE);
            } else {
                holder.iv_is_synced_corner.setVisibility(View.GONE);
            }
        } else {
            if (measure.isSynced()) {
                holder.iv_is_synced_centre.setVisibility(View.VISIBLE);
            } else {
                holder.iv_is_synced_centre.setVisibility(View.GONE);
            }
        }


        holder.txtDate.setText(DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE_AND_TIME, measure.getDate()));
        holder.txtAuthor.setText(Utils.getNameFromEmail(measure.getCreatedBy()));

        SessionManager sessionManager = new SessionManager(context);
        boolean stdtest = sessionManager.getStdTestQrCode() != null;
        if (config.isMeasure_visibility() && !stdtest) {

            holder.txtHeight.setText(String.format(Locale.getDefault(), "%.2f", measure.getHeight()) + context.getString(R.string.unit_cm));
            holder.txtWeight.setText(String.format(Locale.getDefault(), "%.3f", measure.getWeight()) + context.getString(R.string.unit_kg));

            if (measure.getType().compareTo(AppConstants.VAL_MEASURE_MANUAL) == 0) {
                holder.txtHeightConfidence.setVisibility(View.GONE);
                holder.txtWeightConfidence.setVisibility(View.GONE);
            } else {
                int heightConfidence = (int) (measure.getHeightConfidence() * 100);
                int weightConfidence = (int) (measure.getWeightConfidence() * 100);
                setConfidence(holder.txtHeight, holder.txtHeightConfidence, heightConfidence);
                setConfidence(holder.txtWeight, holder.txtWeightConfidence, weightConfidence);
            }
        } else {
            holder.txtHeight.setText(R.string.field_concealed);
            holder.txtWeight.setText(R.string.field_concealed);
            holder.txtHeightConfidence.setVisibility(View.GONE);
            holder.txtWeightConfidence.setVisibility(View.GONE);
        }
        holder.bindSelectListener(measure);
    }

    @Override
    public int getItemCount() {
        return measureList.size();
    }

    public void resetData(List<Measure> measureList) {
        this.measureList = measureList;
        notifyDataSetChanged();
    }

    public void removeMeasure(Measure measure) {
        int index = measureList.indexOf(measure);
        measureList.remove(measure);
        notifyItemRemoved(index);
    }

    private void setConfidence(TextView text, TextView percentage, int value) {
        percentage.setBackgroundResource(R.drawable.tags_rounded_corners);
        GradientDrawable drawable = (GradientDrawable) percentage.getBackground();

        if (value >= AppConstants.MIN_CONFIDENCE) {
            drawable.setColor(text.getResources().getColor(R.color.colorPrimary));
            text.setPaintFlags(Paint.ANTI_ALIAS_FLAG);
        } else {
            drawable.setColor(Color.RED);
            text.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        String label = value + "%";
        percentage.setText(label);
        percentage.setVisibility(View.VISIBLE);
    }

    private void deleteMeasure(Measure measure) {
        if (!config.isAllow_delete()) {
            notifyDataSetChanged();
            Snackbar.make(recyclerMeasure, R.string.permission_delete, Snackbar.LENGTH_LONG).show();
        } else {
            ConfirmDialog dialog = new ConfirmDialog(context);
            dialog.setMessage(R.string.delete_measure);
            dialog.setConfirmListener(result -> {
                if (result) {
                    measure.setDeleted(true);
                    measure.setDeletedBy(session.getUserEmail());
                    measure.setTimestamp(Utils.getUniversalTimestamp());

                    removeMeasure(measure);
                } else {
                    notifyDataSetChanged();
                }
            });
            dialog.show();
        }
    }

    private void editMeasure(Measure measure) {
        if (!config.isAllow_edit()) {
            notifyDataSetChanged();
            Snackbar.make(recyclerMeasure, R.string.permission_edit, Snackbar.LENGTH_LONG).show();
        } else if (measure.getDate() < Utils.getUniversalTimestamp() - config.getTime_to_allow_editing() * 3600 * 1000) {
            notifyDataSetChanged();
            Snackbar.make(recyclerMeasure, R.string.permission_expired, Snackbar.LENGTH_LONG).show();
        } else if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            ManualMeasureDialog measureDialog = new ManualMeasureDialog(context);
            measureDialog.setManualMeasureListener(manualMeasureListener);
            measureDialog.setCloseListener(result -> notifyDataSetChanged());
            measureDialog.setMeasure(measure);
            measureDialog.setPerson(person);
            measureDialog.show();
        }
    }

    private void showMeasure(Measure measure) {
        ManualDetailDialog detailDialog = new ManualDetailDialog(context);
        detailDialog.setMeasure(measure);
        detailDialog.show();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rytItem;

        ImageView imgType;
        ImageView iv_is_synced_corner, iv_is_synced_centre;
        TextView txtDate;
        TextView txtAuthor;
        TextView txtHeight;
        TextView txtHeightConfidence;
        TextView txtWeight;
        TextView txtWeightConfidence;
        ProgressBar progressUpload;
        View contextMenu;

        public ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);
            imgType = itemView.findViewById(R.id.imgType);
            iv_is_synced_corner = itemView.findViewById(R.id.iv_is_synced_corner);
            iv_is_synced_centre = itemView.findViewById(R.id.iv_is_synced_centre);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            txtHeightConfidence = itemView.findViewById(R.id.txtHeightConfidence);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtWeightConfidence = itemView.findViewById(R.id.txtWeightConfidence);
            progressUpload = itemView.findViewById(R.id.progressUpload);
            contextMenu = itemView.findViewById(R.id.contextMenuButton);
        }

        public void bindSelectListener(Measure measure) {
            rytItem.setOnClickListener(v -> showMeasure(measure));
            rytItem.setOnLongClickListener(view -> {
                showContextMenu(measure);
                return true;
            });
            contextMenu.setOnClickListener(view -> showContextMenu(measure));
        }
    }

    private void showContextMenu(Measure measure) {
        String date = DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE_AND_TIME, measure.getDate());

        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
            new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                    new ContextMenuDialog.Item(R.string.show_details, R.drawable.ic_details),
                    new ContextMenuDialog.Item(R.string.edit_data, R.drawable.ic_edit),
                    new ContextMenuDialog.Item(R.string.delete_data, R.drawable.ic_delete),
                    new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
            }, which -> {
                switch (which) {
                    case 0:
                        showMeasure(measure);
                        break;
                    case 1:
                        editMeasure(measure);
                        break;
                    case 2:
                        deleteMeasure(measure);
                        break;
                    case 3:
                        ContactSupportDialog.show(context, "measure " + date, "measureID:" + measure.getId());
                        break;
                }
            });
        } else {
            new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                    new ContextMenuDialog.Item(R.string.show_details, R.drawable.ic_details),
                    new ContextMenuDialog.Item(R.string.delete_data, R.drawable.ic_delete),
                    new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
            }, which -> {
                switch (which) {
                    case 0:
                        showMeasure(measure);
                        break;
                    case 1:
                        deleteMeasure(measure);
                        break;
                    case 2:
                        ContactSupportDialog.show(context, "measure " + date, "measureID:" + measure.getId());
                        break;
                }
            });
        }
    }
    public void setPerson(Person person){
        this.person = person;
    }
}
