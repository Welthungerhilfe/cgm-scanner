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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerMeasureAdapter extends RecyclerView.Adapter<RecyclerMeasureAdapter.ViewHolder> {
    private Context context;
    private OnMeasureSelectListener listener;
    private List<Measure> measureList;
    private int lastPosition = -1;

    public interface OnMeasureSelectListener {
        void onMeasureSelect(Measure measure);
    }

    public RecyclerMeasureAdapter(Context ctx) {
        context = ctx;

        measureList = new ArrayList<>();
    }

    public void setMeasureSelectListener(OnMeasureSelectListener listener) {
        this.listener = listener;
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

    @Override
    public void onBindViewHolder(RecyclerMeasureAdapter.ViewHolder holder, int position) {
        Measure measure = measureList.get(position);

        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL))
            holder.imgType.setImageResource(R.drawable.manual);
        else
            holder.imgType.setImageResource(R.drawable.machine);

        if (measure.isOedema()) {
            holder.rytItem.setBackgroundResource(R.color.colorPink);
        } else {
            holder.rytItem.setBackgroundResource(R.color.colorWhite);
        }

        holder.txtDate.setText(Utils.beautifyHourMinute(measure.getDate()));
        holder.txtAuthor.setText(Utils.getNameFromEmail(measure.getCreatedBy()));

        if (AppController.getInstance().firebaseConfig.getBoolean(AppConstants.CONFIG_MEASURE_VISIBILITY)) {
            holder.txtHeight.setText(Double.toString(measure.getHeight()) + context.getString(R.string.unit_cm));
            holder.txtWeight.setText(Double.toString(measure.getWeight()) + context.getString(R.string.unit_kg));
            holder.txtArm.setText(Double.toString(measure.getMuac()) + context.getString(R.string.unit_cm));
            holder.score.setText(Double.toString(measure.getHeadCircumference()));
        } else {
            holder.txtHeight.setText(R.string.field_concealed);
            holder.txtWeight.setText(R.string.field_concealed);
            holder.txtArm.setText(R.string.field_concealed);
            holder.score.setText("100");
        }

        if (listener != null) {
            holder.bindSelectListener(measureList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return measureList.size();
    }

    public Measure getItem(int position) {
        return measureList.get(position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void changeManualVisibility() {
        notifyDataSetChanged();
    }

    public void resetData(List<Measure> measureList) {
        this.measureList = measureList;
        notifyDataSetChanged();
    }

    public void addMeasure(Measure measure) {
        measureList.add(0, measure);
        notifyItemInserted(0);
    }

    public void addMeasures(List<Measure> measures) {
        measureList.addAll(0, measures);
        notifyDataSetChanged();
    }

    public void removeMeasure(Measure measure) {
        int index = measureList.indexOf(measure);
        measureList.remove(measure);
        notifyItemRemoved(index);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout rytItem;

        public ImageView imgType;

        public TextView txtDate;
        public TextView txtAuthor;
        public TextView txtHeight;
        public TextView txtWeight;
        public TextView score;
        public TextView txtArm;

        public ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);
            imgType = itemView.findViewById(R.id.imgType);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            score = itemView.findViewById(R.id.score);
            txtArm = itemView.findViewById(R.id.txtArm);
        }

        public void bindSelectListener(Measure measure) {
            rytItem.setOnClickListener(v -> listener.onMeasureSelect(measure));
        }
    }
}
