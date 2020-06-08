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

package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRatingBar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureResultRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.views.PrecisionView;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/23/2018.
 */

public class FeedbackDialog extends Dialog {
    private final int REQUEST_LOCATION = 0x1000;

    @BindView(R.id.ratingOverall)
    AppCompatRatingBar ratingOverall;

    @BindView(R.id.ratingStep1)
    AppCompatRatingBar ratingStep1;
    @BindView(R.id.ratingStep2)
    AppCompatRatingBar ratingStep2;
    @BindView(R.id.ratingStep3)
    AppCompatRatingBar ratingStep3;

    @BindView(R.id.ratingLightStep1)
    AppCompatRatingBar ratingLightStep1;
    @BindView(R.id.ratingLightStep2)
    AppCompatRatingBar ratingLightStep2;
    @BindView(R.id.ratingLightStep3)
    AppCompatRatingBar ratingLightStep3;

    @BindView(R.id.ratingDurationStep1)
    AppCompatRatingBar ratingDurationStep1;
    @BindView(R.id.ratingDurationStep2)
    AppCompatRatingBar ratingDurationStep2;
    @BindView(R.id.ratingDurationStep3)
    AppCompatRatingBar ratingDurationStep3;

    @BindView(R.id.ratingPrecisionStep1)
    AppCompatRatingBar ratingPrecisionStep1;
    @BindView(R.id.ratingPrecisionStep2)
    AppCompatRatingBar ratingPrecisionStep2;
    @BindView(R.id.ratingPrecisionStep3)
    AppCompatRatingBar ratingPrecisionStep3;

    @BindView(R.id.ratingVisibilityStep1)
    AppCompatRatingBar ratingVisibilityStep1;
    @BindView(R.id.ratingVisibilityStep2)
    AppCompatRatingBar ratingVisibilityStep2;
    @BindView(R.id.ratingVisibilityStep3)
    AppCompatRatingBar ratingVisibilityStep3;

    @BindView(R.id.ratingChildNoStep1)
    AppCompatRatingBar ratingChildNoStep1;
    @BindView(R.id.ratingChildNoStep2)
    AppCompatRatingBar ratingChildNoStep2;
    @BindView(R.id.ratingChildNoStep3)
    AppCompatRatingBar ratingChildNoStep3;

    @BindView(R.id.imgDurationStep1)
    ImageView imgDurationStep1;
    @BindView(R.id.imgDurationStep2)
    ImageView imgDurationStep2;
    @BindView(R.id.imgDurationStep3)
    ImageView imgDurationStep3;

    @BindView(R.id.lytPrecisionStep1)
    LinearLayout lytPrecisionStep1;
    @BindView(R.id.lytPrecisionStep2)
    LinearLayout lytPrecisionStep2;
    @BindView(R.id.lytPrecisionStep3)
    LinearLayout lytPrecisionStep3;

    @OnClick(R.id.btnOK)
    void OnConfirm() {
        dismiss();
    }

    private Measure measure;
    private ArtifactResultRepository artifactResultRepository;
    private MeasureResultRepository measureResultRepository;

    public FeedbackDialog(@NonNull Context context) {
        super(context);

        artifactResultRepository = ArtifactResultRepository.getInstance(context);
        measureResultRepository = MeasureResultRepository.getInstance(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_measure_feedback);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;

        updateUI();
    }

    @SuppressLint("StaticFieldLeak")
    private void updateUI() {
        new AsyncTask<Void, Void, Boolean>() {
            private double averagePointCountFront = 0;
            private int pointCloudCountFront = 0;

            private double averagePointCountSide = 0;
            private int pointCloudCountSide = 0;

            private double averagePointCountBack = 0;
            private int pointCloudCountBack = 0;

            private float frontConfidence, sideConfidence, backConfidence;

            @Override
            protected Boolean doInBackground(Void... voids) {
                averagePointCountFront = artifactResultRepository.getAveragePointCountForFront(measure.getId());
                pointCloudCountFront = artifactResultRepository.getPointCloudCountForFront(measure.getId());

                averagePointCountSide = artifactResultRepository.getAveragePointCountForSide(measure.getId());
                pointCloudCountSide = artifactResultRepository.getPointCloudCountForSide(measure.getId());

                averagePointCountBack = artifactResultRepository.getAveragePointCountForBack(measure.getId());
                pointCloudCountBack = artifactResultRepository.getPointCloudCountForBack(measure.getId());

                frontConfidence = measureResultRepository.getConfidence(measure.getId(), "%_front");
                sideConfidence = measureResultRepository.getConfidence(measure.getId(), "%_360");
                backConfidence = measureResultRepository.getConfidence(measure.getId(), "%_back");

                return true;
            }

            @SuppressLint("DefaultLocale")
            public void onPostExecute(Boolean result) {

                double lightScoreFront = (Math.abs(averagePointCountFront / 38000 - 1.0) * 3);
                double durationScoreFront = Math.abs(1- Math.abs((double) pointCloudCountFront / 8 - 1));
                if (lightScoreFront > 1) lightScoreFront -= 1;
                if (durationScoreFront > 1) durationScoreFront -= 1;

                double scoreFront = lightScoreFront * durationScoreFront;
                ratingStep1.setRating(5 * (float)scoreFront);

                if (pointCloudCountFront < 8) {
                    imgDurationStep1.setImageResource(R.drawable.ic_arrow_down);
                    imgDurationStep1.setVisibility(View.VISIBLE);
                } else if (pointCloudCountFront > 9) {
                    imgDurationStep1.setImageResource(R.drawable.ic_arrow_up);
                    imgDurationStep1.setVisibility(View.VISIBLE);
                } else {
                    imgDurationStep1.setVisibility(View.GONE);
                }


                double lightScoreSide = (Math.abs(averagePointCountSide / 38000 - 1.0) * 3);
                double durationScoreSide = Math.abs(1- Math.abs((double) pointCloudCountSide / 24 - 1));
                if (lightScoreSide > 1) lightScoreSide -= 1;
                if (durationScoreSide > 1) durationScoreSide -= 1;

                double scoreSide = lightScoreSide * durationScoreSide;
                ratingStep2.setRating(5 * (float)scoreSide);

                if (pointCloudCountSide < 12) {
                    imgDurationStep2.setImageResource(R.drawable.ic_arrow_down);
                    imgDurationStep2.setVisibility(View.VISIBLE);
                } else if (pointCloudCountSide > 27) {
                    imgDurationStep2.setImageResource(R.drawable.ic_arrow_up);
                    imgDurationStep2.setVisibility(View.VISIBLE);
                } else {
                    imgDurationStep2.setVisibility(View.GONE);
                }

                double lightScoreBack = (Math.abs(averagePointCountBack / 38000 - 1.0) * 3);
                double durationScoreBack = Math.abs(1- Math.abs((double) pointCloudCountBack / 8 - 1));
                if (lightScoreBack > 1) lightScoreBack -= 1;
                if (durationScoreBack > 1) durationScoreBack -= 1;

                double scoreBack = lightScoreBack * durationScoreBack;
                ratingStep3.setRating(5 * (float)scoreBack);

                if (pointCloudCountBack < 8) {
                    imgDurationStep3.setImageResource(R.drawable.ic_arrow_down);
                    imgDurationStep3.setVisibility(View.VISIBLE);
                } else if (pointCloudCountBack > 9) {
                    imgDurationStep3.setImageResource(R.drawable.ic_arrow_up);
                    imgDurationStep3.setVisibility(View.VISIBLE);
                } else {
                    imgDurationStep3.setVisibility(View.GONE);
                }

                double overallScore = 0;
                if (scoreFront > overallScore) overallScore = scoreFront;
                if (scoreSide > overallScore) overallScore = scoreSide;
                if (scoreBack > overallScore) overallScore = scoreBack;
                ratingOverall.setRating(5 * (float)overallScore);

                Log.e("front-light : ", String.valueOf(lightScoreFront));
                Log.e("side-light : ", String.valueOf(lightScoreSide));
                Log.e("back-light : ", String.valueOf(lightScoreBack));

                Log.e("front-duration : ", String.valueOf(durationScoreFront));
                Log.e("side-duration : ", String.valueOf(durationScoreSide));
                Log.e("back-duration : ", String.valueOf(durationScoreBack));

                ratingLightStep1.setRating(5 * (float)lightScoreFront);
                ratingDurationStep1.setRating(5 * (float)durationScoreFront);

                if (frontConfidence > 0) {
                    lytPrecisionStep1.setVisibility(View.VISIBLE);
                    ratingPrecisionStep1.setRating(5 * frontConfidence);
                }

                ratingLightStep2.setRating(5 * (float)lightScoreSide);
                ratingDurationStep2.setRating(5 * (float)durationScoreSide);

                if (sideConfidence > 0) {
                    lytPrecisionStep2.setVisibility(View.VISIBLE);
                    ratingPrecisionStep2.setRating(5 * sideConfidence);
                }

                ratingLightStep3.setRating(5 * (float)lightScoreBack);
                ratingDurationStep3.setRating(5 * (float)durationScoreBack);

                if (backConfidence > 0) {
                    lytPrecisionStep3.setVisibility(View.VISIBLE);
                    ratingPrecisionStep3.setRating(5 * backConfidence);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
