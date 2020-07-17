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
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerMeasureAdapter extends RecyclerView.Adapter<RecyclerMeasureAdapter.ViewHolder> {
    private Context context;
    private OnMeasureSelectListener listener;
    private OnMeasureFeedbackListener feedbackListener;
    private List<Measure> measureList;
    private int lastPosition = -1;

    private FileLogRepository artifactRepository;
    private ArtifactResultRepository artifactResultRepository;

    private SessionManager session;
    private RemoteConfig config;

    public interface OnMeasureSelectListener {
        void onMeasureSelect(Measure measure);
    }

    public interface OnMeasureFeedbackListener {
        void onMeasureFeedback(Measure measure, double overallScore);
    }

    public RecyclerMeasureAdapter(Context ctx) {
        context = ctx;

        artifactRepository = FileLogRepository.getInstance(context);
        artifactResultRepository = ArtifactResultRepository.getInstance(context);

        measureList = new ArrayList<>();

        this.session = new SessionManager(context);
        config = session.getRemoteConfig();
    }

    public void setMeasureSelectListener(OnMeasureSelectListener listener) {
        this.listener = listener;
    }

    public void setMeasureFeedbackListener(OnMeasureFeedbackListener listener) {
        this.feedbackListener = listener;
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

            holder.rateOverallScore.setVisibility(View.GONE);
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

            new AsyncTask<Void, Void, Boolean>() {
                private double averagePointCountFront = 0;
                private int pointCloudCountFront = 0;

                private double averagePointCountSide = 0;
                private int pointCloudCountSide = 0;

                private double averagePointCountBack = 0;
                private int pointCloudCountBack = 0;

                @Override
                protected Boolean doInBackground(Void... voids) {
                    averagePointCountFront = artifactResultRepository.getAveragePointCountForFront(measure.getId());
                    pointCloudCountFront = artifactResultRepository.getPointCloudCountForFront(measure.getId());

                    averagePointCountSide = artifactResultRepository.getAveragePointCountForSide(measure.getId());
                    pointCloudCountSide = artifactResultRepository.getPointCloudCountForSide(measure.getId());

                    averagePointCountBack = artifactResultRepository.getAveragePointCountForBack(measure.getId());
                    pointCloudCountBack = artifactResultRepository.getPointCloudCountForBack(measure.getId());
                    return true;
                }

                @SuppressLint("DefaultLocale")
                public void onPostExecute(Boolean result) {
                    double lightScoreFront = (Math.abs(averagePointCountFront / 38000 - 1.0) * 3);
                    double durationScoreFront = Math.abs(1- Math.abs((double) pointCloudCountFront / 8 - 1));
                    if (lightScoreFront > 1) lightScoreFront -= 1;
                    if (durationScoreFront > 1) durationScoreFront -= 1;

                    double lightScoreSide = (Math.abs(averagePointCountSide / 38000 - 1.0) * 3);
                    double durationScoreSide = Math.abs(1- Math.abs((double) pointCloudCountSide / 24 - 1));
                    if (lightScoreSide > 1) lightScoreSide -= 1;
                    if (durationScoreSide > 1) durationScoreSide -= 1;

                    double lightScoreBack = (Math.abs(averagePointCountBack / 38000 - 1.0) * 3);
                    double durationScoreBack = Math.abs(1- Math.abs((double) pointCloudCountBack / 8 - 1));
                    if (lightScoreBack > 1) lightScoreBack -= 1;
                    if (durationScoreBack > 1) durationScoreBack -=  1;

                    Log.e("front-light : ", String.valueOf(lightScoreFront));
                    Log.e("side-light : ", String.valueOf(lightScoreSide));
                    Log.e("back-light : ", String.valueOf(lightScoreBack));

                    Log.e("front-duration : ", String.valueOf(durationScoreFront));
                    Log.e("side-duration : ", String.valueOf(durationScoreSide));
                    Log.e("back-duration : ", String.valueOf(durationScoreBack));

                    double scoreFront = lightScoreFront * durationScoreFront;
                    double scoreSide = lightScoreSide * durationScoreSide;
                    double scoreBack = lightScoreBack * durationScoreBack;

                    double overallScore = 0;
                    if (scoreFront > overallScore) overallScore = scoreFront;
                    if (scoreSide > overallScore) overallScore = scoreSide;
                    if (scoreBack > overallScore) overallScore = scoreBack;

                    holder.rateOverallScore.setRating(5 * (float)overallScore);

                    if (feedbackListener != null) holder.bindScanFeedbackListener(measureList.get(holder.getAdapterPosition()), overallScore);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (measure.isOedema()) {
            holder.rytItem.setBackgroundResource(R.color.colorPink);
        } else {
            holder.rytItem.setBackgroundResource(R.color.colorWhite);
        }

        holder.txtDate.setText(DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE_AND_TIME, measure.getDate()));
        holder.txtAuthor.setText(Utils.getNameFromEmail(measure.getCreatedBy()));

        if (config.isMeasure_visibility()) {
            holder.txtHeight.setText(String.format("%.2f%s", measure.getHeight(), context.getString(R.string.unit_cm)));
            holder.txtWeight.setText(String.format("%.3f%s", measure.getWeight(), context.getString(R.string.unit_kg)));
        } else {
            holder.txtHeight.setText(R.string.field_concealed);
            holder.txtWeight.setText(R.string.field_concealed);
        }

        if (listener != null) {
            holder.bindSelectListener(measureList.get(holder.getAdapterPosition()));
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
        RelativeLayout rytItem;

        ImageView imgType;

        TextView txtDate;
        TextView txtAuthor;
        TextView txtHeight;
        TextView txtWeight;
        ProgressBar progressUpload;
        AppCompatRatingBar rateOverallScore;

        public ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);
            imgType = itemView.findViewById(R.id.imgType);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtHeight = itemView.findViewById(R.id.txtHeight);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            rateOverallScore = itemView.findViewById(R.id.rateOverallScore);
            progressUpload = itemView.findViewById(R.id.progressUpload);
        }

        public void bindSelectListener(Measure measure) {
            rytItem.setOnClickListener(v -> listener.onMeasureSelect(measure));
        }

        public void bindScanFeedbackListener(Measure measure, double overallScore) {
            rateOverallScore.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    feedbackListener.onMeasureFeedback(measure, overallScore);
                }
                return true;
            });
        }
    }
}
