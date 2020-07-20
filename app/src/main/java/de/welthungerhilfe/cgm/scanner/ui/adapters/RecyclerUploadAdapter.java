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

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerUploadAdapter extends RecyclerView.Adapter<RecyclerUploadAdapter.ViewHolder> {
    private Context context;
    private List<Measure> measureList;

    private FileLogRepository artifactRepository;

    public RecyclerUploadAdapter(Context ctx) {
        context = ctx;

        artifactRepository = FileLogRepository.getInstance(context);

        measureList = new ArrayList<>();
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
    public RecyclerUploadAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_upload, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerUploadAdapter.ViewHolder holder, int position) {
        Measure measure = measureList.get(position);

        final String measureId = measure.getId();
        Observer<UploadStatus> statusObserver = status -> {
            double progress = status.getUploaded() / status.getTotal() * 100;
            if (progress >= 100)
                artifactRepository.getMeasureUploadProgress(measureId).removeObservers((LifecycleOwner) context);
            else
                holder.progressUpload.setProgress((int) progress);
        };
        artifactRepository.getMeasureUploadProgress(measureId).observeForever(statusObserver);

        holder.txtDate.setText(DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE_AND_TIME, measure.getDate()));
        holder.txtAuthor.setText(Utils.getNameFromEmail(measure.getCreatedBy()));
        holder.txtQr.setText(measure.getQrCode());
    }

    @Override
    public int getItemCount() {
        return measureList.size();
    }

    public void setData(List<Measure> measures) {
        this.measureList = measures;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rytItem;

        TextView txtDate;
        TextView txtAuthor;
        TextView txtQr;
        ProgressBar progressUpload;

        ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtQr = itemView.findViewById(R.id.txtQr);
            progressUpload = itemView.findViewById(R.id.progressUpload);
        }
    }
}
