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

import androidx.annotation.Nullable;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.common.collect.Iterables;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.VerticalTextView;
import de.welthungerhilfe.cgm.scanner.utils.CalculateZscoreUtils;
import de.welthungerhilfe.cgm.scanner.utils.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class GrowthDataFragment extends Fragment {

    String TAG = GrowthDataFragment.class.getSimpleName();

    private final int ZSCORE_COLOR_0 = Color.rgb(55, 129, 69);
    private final int ZSCORE_COLOR_2 = Color.rgb(230, 122, 58);
    private final int ZSCORE_COLOR_3 = Color.rgb(212, 53, 62);

    private Context context;

    private final String[] boys_0_6 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
    private final String[] girls_0_6 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

    private final String[] boys_0_2 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfl_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
    private final String[] girls_0_2 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfl_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};


    private LineChart mChart;

    private VerticalTextView txtYAxis;
    private TextView txtXAxis;

    private TextView txtLabel;
    private TextView txtZScore;

    private int chartType = 0;

    public String qrCode;

    LinearLayout lytNotif;
    TextView txtNotifTitle;
    TextView txtNotifMessage;
    View contextMenu;

    public static GrowthDataFragment getInstance(String qrCode) {
        GrowthDataFragment fragment = new GrowthDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            setData();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_growth, container, false);

        txtLabel = view.findViewById(R.id.txtLabel);
        txtZScore = view.findViewById(R.id.txtZScore);
        txtYAxis = view.findViewById(R.id.txtYAxis);
        txtXAxis = view.findViewById(R.id.txtXAxis);

        lytNotif = view.findViewById(R.id.lytNotif);
        txtNotifTitle = view.findViewById(R.id.txtNotifTitle);
        txtNotifMessage = view.findViewById(R.id.txtNotifMessage);

        mChart = view.findViewById(R.id.chart1);
        MaterialSpinner dropChart = view.findViewById(R.id.dropChart);

        String[] filters = getResources().getStringArray(R.array.filters);
        dropChart.setItems(filters);
        dropChart.setOnItemSelectedListener((MaterialSpinner.OnItemSelectedListener<String>) (view1, position, id, item) -> {
            chartType = position;
            setData();
        });

        contextMenu = view.findViewById(R.id.contextMenuButton);
        contextMenu.setOnClickListener(v -> {
            Person person = PersonRepository.getInstance(context).findPersonByQr(qrCode);
            if (person != null) {
                String id = person.getId();
                String qrCode = person.getQrcode();
                BaseActivity activity = (BaseActivity) getActivity();
                new ContextMenuDialog(context, new ContextMenuDialog.Item[]{
                        new ContextMenuDialog.Item(R.string.contact_support, R.drawable.ic_contact_support),
                }, which -> {
                    ContactSupportDialog.show(activity, "growth " + qrCode, "personID:" + id);
                });
            }
        });

        initChart();
        return view;
    }

    private void initChart() {
        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        mChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        // get the legend (only possible after setting data)
        mChart.getLegend().setEnabled(true);

        mChart.getAxisLeft().setEnabled(true);
        mChart.getAxisLeft().setDrawGridLines(true);

        mChart.getXAxis().setDrawAxisLine(true);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getXAxis().setDrawGridLines(true);
        mChart.getXAxis().setValueFormatter((value, axis) -> {
            axis.setLabelCount(6, true);
            return value + "";
        });
    }

    public void setData() {
        Person person = PersonRepository.getInstance(context).findPersonByQr(qrCode);
        List<Measure> measures = MeasureRepository.getInstance(context).getManualMeasures(person.getId());
        if (person == null || measures == null || measures.size() == 0) {
            return;
        }
        Measure lastMeasure = null;
        if (measures.size() > 0)
            lastMeasure = measures.get(0);
        Log.i(TAG,"this is value of lastmeasure H-> "+lastMeasure.getHeight()+" W-> "+lastMeasure.getWeight());
        txtLabel.setText(person.getSex());


        long birthday = person.getBirthday();

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        // ------------------------- Line for ruler values ---------------------------------- //
        ArrayList<Entry> SD3neg = new ArrayList<>();
        ArrayList<Entry> SD2neg = new ArrayList<>();
        ArrayList<Entry> SD0 = new ArrayList<>();
        ArrayList<Entry> SD2 = new ArrayList<>();
        ArrayList<Entry> SD3 = new ArrayList<>();

        double zScore = 100, median = 10, skew = 10, coefficient = 10;

        try {
            BufferedReader reader;

            String[] boys, girls;
            if (lastMeasure != null && lastMeasure.getAge() < 365 * 2 && lastMeasure.getHeight() <= 110) {
                boys = boys_0_2;
                girls = girls_0_2;
            } else {
                boys = boys_0_6;
                girls = girls_0_6;
            }

            if (person.getSex().equals("female"))
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(girls[chartType]), StandardCharsets.UTF_8));
            else
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(boys[chartType]), StandardCharsets.UTF_8));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] arr = mLine.split("\t");
                float rule;
                try {
                    rule = Float.parseFloat(arr[0]);
                } catch (Exception e) {
                    continue;
                }
                if (lastMeasure != null) {
                    if ((chartType == 0 || chartType == 1 || chartType==3)) {
                        if ((int) rule == (int) (lastMeasure.getAge() * 12 / 365)) {
                            skew = Utils.parseDouble(arr[1]);
                            median = Utils.parseDouble(arr[2]);
                            coefficient = Utils.parseDouble(arr[3]);
                        }
                    } else if (chartType == 2) {
                        if ((int) (rule * 2) == (int) (lastMeasure.getHeight() * 2)) {
                            skew = Utils.parseDouble(arr[1]);
                            median = Utils.parseDouble(arr[2]);
                            coefficient = Utils.parseDouble(arr[3]);
                        }
                    }
                }

                SD3neg.add(new Entry(rule, Utils.parseFloat(arr[4])));
                SD2neg.add(new Entry(rule, Utils.parseFloat(arr[5])));
                SD0.add(new Entry(rule, Utils.parseFloat(arr[7])));
                SD2.add(new Entry(rule, Utils.parseFloat(arr[9])));
                SD3.add(new Entry(rule, Utils.parseFloat(arr[10])));
            }
            reader.close();

            dataSets.add(createDataSet(SD3neg, "-3", ZSCORE_COLOR_3, 1.5f, false));
            dataSets.add(createDataSet(SD2neg, "-2", ZSCORE_COLOR_2, 1.5f, false));
            dataSets.add(createDataSet(SD0, "0", ZSCORE_COLOR_0, 1.5f, false));
            dataSets.add(createDataSet(SD2, "+2", ZSCORE_COLOR_2, 1.5f, false));
            dataSets.add(createDataSet(SD3, "+3", ZSCORE_COLOR_3, 1.5f, false));


        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (chartType) {
            case 0:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_weight);

                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = CalculateZscoreUtils.newZScore(lastMeasure.getWeight(), median, skew, coefficient);
                }
                break;
            case 1:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_height);

                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = CalculateZscoreUtils.newZScore(lastMeasure.getHeight(), median, skew, coefficient);
                }
                break;
            case 2:
                txtXAxis.setText(R.string.axis_height);
                txtYAxis.setText(R.string.axis_weight);

                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = CalculateZscoreUtils.newZScore(lastMeasure.getWeight(), median, skew, coefficient);
                }
                break;
            case 3:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_muac);

                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = CalculateZscoreUtils.newZScore(lastMeasure.getMuac(), median, skew, coefficient);
                }
                break;
        }

        txtZScore.setText(String.format(Locale.getDefault(), "( z-score : %.2f )", zScore));

        if (zScore < -3) { // SAM
            switch (chartType) {
                case 0: // weight for age
                    txtNotifTitle.setText(R.string.sam_wfa_title);
                    txtNotifMessage.setText(R.string.sam_wfa_message);
                    break;
                case 1: // height for age
                    txtNotifTitle.setText(R.string.sam_hfa_title);
                    txtNotifMessage.setText(R.string.sam_hfa_message);
                    break;
                case 2: // weight for height
                    txtNotifTitle.setText(R.string.sam_wfh_title);
                    txtNotifMessage.setText(R.string.sam_wfh_message);
                    break;
                case 3: // muac for age
                    txtNotifTitle.setText(R.string.sam_acfa_title);
                    txtNotifMessage.setText(R.string.sam_acfa_message);
                    break;
            }

            lytNotif.setBackgroundResource(R.color.colorRed);
            lytNotif.setVisibility(View.VISIBLE);
        } else if (zScore < -2 && zScore >= -3) { // MAM
            switch (chartType) {
                case 0: // weight for age
                    txtNotifTitle.setText(R.string.mam_wfa_title);
                    txtNotifMessage.setText(R.string.mam_wfa_message);
                    break;
                case 1: // height for age
                    txtNotifTitle.setText(R.string.mam_hfa_title);
                    txtNotifMessage.setText(R.string.mam_hfa_message);
                    break;
                case 2: // weight for height
                    txtNotifTitle.setText(R.string.mam_wfh_title);
                    txtNotifMessage.setText(R.string.mam_wfh_message);
                    break;
                case 3: // muac for age
                    txtNotifTitle.setText(R.string.mam_acfa_title);
                    txtNotifMessage.setText(R.string.mam_acfa_message);
                    break;
            }

            lytNotif.setBackgroundResource(R.color.colorYellow);
            lytNotif.setVisibility(View.VISIBLE);
        } else { // Healthy
            lytNotif.setVisibility(View.GONE);
        }

        // ----------------------- Line for manual measures -------------------------- //
        ArrayList<Entry> entries = new ArrayList<>();

        for (Measure measure : measures) {
            float x = 0, y = 0;
            switch (chartType) {
                case 0:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24 / 30;
                    y = (float) measure.getWeight();
                    break;
                case 1:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24 / 30;
                    y = (float) measure.getHeight();
                    break;
                case 2:
                    DecimalFormat decimalFormat = new DecimalFormat("#.#");
                    x = Utils.parseFloat(decimalFormat.format(measure.getHeight()));
                    y = (float) measure.getWeight();
                    break;
                case 3:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24 / 30;
                    y = (float) measure.getMuac();
                    break;
            }

            if (x == 0 || y == 0)
                continue;

            final int fX = (int)x;
            try {
                ArrayList<Entry> entry = new ArrayList<>();
                entry.add(new Entry(x, y));

                if (y <= Iterables.find(SD3neg, input -> (int)input.getX() == fX).getY()) {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_3, 5f, true));
                } else if (y <= Iterables.find(SD2neg, input -> (int)input.getX() == fX).getY()) {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_2, 5f, true));
                } else if (y <= Iterables.find(SD0, input -> (int)input.getX() == fX).getY()) {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_0, 5f, true));
                } else if (y <= Iterables.find(SD2, input -> (int)input.getX() == fX).getY()) {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_0, 5f, true));
                } else if (y <= Iterables.find(SD3, input -> (int)input.getX() == fX).getY()) {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_2, 5f, true));
                } else {
                    dataSets.add(createDataSet(entry, "", ZSCORE_COLOR_3, 5f, true));
                }
                entries.add(new Entry(x, y));
            } catch (Exception e) {
                LogFileUtils.logException(e);
            }
        }

        if (entries.size() > 1) {
            Collections.sort(entries, (o1, o2) -> Float.compare(o1.getX(), o2.getX()));
            dataSets.add(createDataSet(entries, "measure", Color.rgb(158, 232, 252), 1.5f, false));
        }


        mChart.setData(new LineData(dataSets));
        mChart.animateX(3000);
    }

    protected LineDataSet createDataSet(ArrayList<Entry> values, String label, int color, float width, boolean circle) {
        LineDataSet dataSet = new LineDataSet(values, label);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(color);
        dataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(width);
        dataSet.setDrawCircles(circle);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);

        return dataSet;
    }
}
