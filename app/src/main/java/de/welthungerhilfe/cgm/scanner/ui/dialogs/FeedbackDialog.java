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
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/23/2018.
 */

public class FeedbackDialog extends Dialog {
    private final int REQUEST_LOCATION = 0x1000;

    @BindView(R.id.txtOverallScore)
    TextView txtOverallScore;
    @BindView(R.id.txtFrontFeedback)
    TextView txtFrontFeedback;
    @BindView(R.id.txtSideFeedback)
    TextView txtSideFeedback;
    @BindView(R.id.txtBackFeedback)
    TextView txtBackFeedback;


    @OnClick(R.id.btnOK)
    void OnConfirm() {
        dismiss();
    }

    private Measure measure;
    private ArtifactResultRepository artifactResultRepository;

    public FeedbackDialog(@NonNull Context context) {
        super(context);

        artifactResultRepository = ArtifactResultRepository.getInstance(context);

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

                double lightScoreSide = (Math.abs(averagePointCountSide / 38000 - 1.0) * 3);
                double durationScoreSide = Math.abs(1- Math.abs((double) pointCloudCountSide / 24 - 1));

                double lightScoreBack = (Math.abs(averagePointCountBack / 38000 - 1.0) * 3);
                double durationScoreBack = Math.abs(1- Math.abs((double) pointCloudCountBack / 8 - 1));

                Log.e("front-light : ", String.valueOf(lightScoreFront));
                Log.e("side-light : ", String.valueOf(lightScoreSide));
                Log.e("back-light : ", String.valueOf(lightScoreBack));

                Log.e("front-duration : ", String.valueOf(durationScoreFront));
                Log.e("side-duration : ", String.valueOf(durationScoreSide));
                Log.e("back-duration : ", String.valueOf(durationScoreBack));

                String issuesFront = String.format(" - Light Score : %d%%", Math.round(lightScoreFront * 100));
                issuesFront = String.format("%s\n - Duration score : %d%%", issuesFront, Math.round(durationScoreFront * 100));
                if (pointCloudCountFront < 8) issuesFront = String.format("%s\n - Duration was too short", issuesFront);
                else if (pointCloudCountFront > 9) issuesFront = String.format("%s\n - Duration was too long", issuesFront);
                txtFrontFeedback.setText(issuesFront);

                String issuesSide = String.format(" - Light Score : %d%%", Math.round(lightScoreSide * 100));
                issuesSide = String.format("%s\n - Duration score : %d%%", issuesSide, Math.round(durationScoreSide * 100));
                if (pointCloudCountSide < 12) issuesSide = String.format("%s\n - Duration was too short", issuesSide);
                else if (pointCloudCountSide > 27) issuesSide = String.format("%s\n - Duration was too long", issuesSide);
                txtSideFeedback.setText(issuesSide);

                String issuesBack = String.format(" - Light Score : %d%%", Math.round(lightScoreBack * 100));
                issuesBack = String.format("%s\n - Duration score : %d%%", issuesBack, Math.round(durationScoreBack * 100));

                if (pointCloudCountBack < 8) issuesBack = String.format("%s\n - Duration was too short", issuesBack);
                else if (pointCloudCountBack > 9) issuesBack = String.format("%s\n - Duration was too long", issuesBack);
                txtBackFeedback.setText(issuesBack);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("DefaultLocale")
    public void setOverallScore(double overallScore) {
        txtOverallScore.setText(String.format("%d%%", Math.round(overallScore * 100)));
    }
}
