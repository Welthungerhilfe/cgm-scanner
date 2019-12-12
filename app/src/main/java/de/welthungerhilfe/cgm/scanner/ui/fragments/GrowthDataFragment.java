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

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.ui.views.VerticalTextView;

public class GrowthDataFragment extends Fragment {
    private Context context;

    private final String[] boys = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
    private final String[] girls = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

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

    private Person person;
    private List<Measure> measures = new ArrayList<>();

    public static GrowthDataFragment getInstance(String qrCode) {
        GrowthDataFragment fragment = new GrowthDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
    }

    public void onActivityCreated(Bundle instance) {
        super.onActivityCreated(instance);

        CreateDataViewModel viewModel = ViewModelProviders.of(getActivity()).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            setData();
        });
        viewModel.getManualMeasuresLiveData().observe(getViewLifecycleOwner(), measures -> {
            this.measures = measures;
            setData();
        });
    }

    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
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

        //chartGrowth = view.findViewById(R.id.chartGrowth);
        mChart = view.findViewById(R.id.chart1);
        MaterialSpinner dropChart = view.findViewById(R.id.dropChart);

        @SuppressLint("ResourceType") String[] filters = getResources().getStringArray(R.array.filters);
        dropChart.setItems(filters);
        dropChart.setOnItemSelectedListener((MaterialSpinner.OnItemSelectedListener<String>) (view1, position, id, item) -> {
            chartType = position;
            setData();
        });

        initChart();
        //setChartData();

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
        /*
        mChart.getXAxis().setGranularity(1.0f);
        mChart.getXAxis().setGranularityEnabled(true);
        */

        //setData();
    }

    @SuppressLint("DefaultLocale")
    public void setData() {
        if (person == null || measures == null)
            return;

        Measure lastMeasure = null;
        if (measures.size() > 0)
            lastMeasure = measures.get(measures.size() - 1);

        txtLabel.setText(person.getSex());



        long birthday = person.getBirthday();

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        // ------------------------- Line for ruler values ---------------------------------- //
        ArrayList<Entry> p3 = new ArrayList<>();
        ArrayList<Entry> p15 = new ArrayList<>();
        ArrayList<Entry> p50 = new ArrayList<>();
        ArrayList<Entry> p85 = new ArrayList<>();
        ArrayList<Entry> p97 = new ArrayList<>();

        double zScore = 100, median = 100, standard = 100, coefficient = 100;

        try {
            BufferedReader reader;

            if (person.getSex().equals("female"))
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(girls[chartType]), "UTF-8"));
            else
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(boys[chartType]), "UTF-8"));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] arr = mLine.split("\t");
                float rule;
                try {
                    rule = Float.parseFloat(arr[0]);
                } catch (Exception e) {
                    continue;
                }

                if ((chartType == 0 || chartType == 1 ) && lastMeasure != null && rule == lastMeasure.getAge()) {
                    median = Double.parseDouble(arr[2]);
                    coefficient = Double.parseDouble(arr[3]);
                    standard = median * coefficient;

                    Log.e("Median", String.valueOf(median));
                    Log.e("Coefficient", String.valueOf(coefficient));
                    Log.e("Standard Deviation", String.valueOf(standard));
                } else if (chartType == 2 && lastMeasure != null && rule == lastMeasure.getHeight()) {
                    median = Double.parseDouble(arr[2]);
                    coefficient = Double.parseDouble(arr[3]);
                    standard = median * coefficient;

                    Log.e("Median", String.valueOf(median));
                    Log.e("Coefficient", String.valueOf(coefficient));
                    Log.e("Standard Deviation", String.valueOf(standard));
                }

                p3.add(new Entry(rule, Float.parseFloat(arr[6])));
                p15.add(new Entry(rule, Float.parseFloat(arr[9])));
                p50.add(new Entry(rule, Float.parseFloat(arr[11])));
                p85.add(new Entry(rule, Float.parseFloat(arr[13])));
                p97.add(new Entry(rule, Float.parseFloat(arr[16])));
            }
            reader.close();

            dataSets.add(createDataSet(p3, "3rd", Color.rgb(212, 53, 62), 1.5f, false));
            dataSets.add(createDataSet(p15, "15th", Color.rgb(230, 122, 58), 1.5f, false));
            dataSets.add(createDataSet(p50, "50th", Color.rgb(55, 129, 69), 1.5f, false));
            dataSets.add(createDataSet(p85, "85th", Color.rgb(230, 122, 58), 1.5f, false));
            dataSets.add(createDataSet(p97, "97th", Color.rgb(212, 53, 62), 1.5f, false));


        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (chartType) {
            case 0:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_weight);

                if (lastMeasure != null && median != 0 && standard != 0) {
                    Log.e("Current Value", String.valueOf(lastMeasure.getWeight()));
                    zScore = (lastMeasure.getWeight() - median) / standard;
                }
                break;
            case 1:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_height);

                if (lastMeasure != null && median != 0 && standard != 0) {
                    Log.e("Current Value", String.valueOf(lastMeasure.getHeight()));
                    zScore = (lastMeasure.getHeight() - median) / standard;
                }
                break;
            case 2:
                txtXAxis.setText(R.string.axis_height);
                txtYAxis.setText(R.string.axis_weight);

                if (lastMeasure != null && median != 0 && standard != 0) {
                    Log.e("Current Value", String.valueOf(lastMeasure.getWeight()));
                    zScore = (lastMeasure.getWeight() - median) / standard;
                }
                break;
            case 3:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_muac);

                Log.e("Current Value", String.valueOf(lastMeasure.getMuac()));

                if (lastMeasure.getMuac() < 11.5) { // SAM (red)
                    zScore = -3;
                } else if (lastMeasure.getMuac() < 12.5) { // MAM (yellow)
                    zScore = -2;
                } else {
                    zScore = 0;
                }
                break;
        }

        Log.e("Z Score", String.valueOf(zScore));
        txtZScore.setText(String.format("( z-score : %.2f )", zScore));

        if (zScore <= -3) { // SAM
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
        } else if (zScore <= -2) { // MAM
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
            if (!measure.getType().equals("manual"))
                continue;

            float x = 0, y = 0;
            switch (chartType) {
                case 0:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24;
                    y = (float) measure.getWeight();
                    break;
                case 1:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24;
                    y = (float) measure.getHeight();
                    break;
                case 2:
                    x = (float) measure.getHeight();
                    y = (float) measure.getWeight();
                    break;
                case 3:
                    x = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24;
                    y = (float) measure.getMuac();
                    break;
            }

            final float fX = x;
            Entry v3 = Iterables.tryFind(p3, input -> input.getX() == fX).orNull();
            try {
                ArrayList<Entry> entry = new ArrayList<>();
                entry.add(new Entry(x, y));

                if (y <= v3.getY()) {
                    dataSets.add(createDataSet(entry, "", Color.rgb(212, 53, 62), 5f, true));
                } else {
                    Entry v15 = Iterables.tryFind(p15, input -> input.getX() == fX).orNull();
                    if (y <= v15.getY()) {
                        dataSets.add(createDataSet(entry, "", Color.rgb(230, 122, 58), 5f, true));
                    } else {
                        Entry v50 = Iterables.tryFind(p50, input -> input.getX() == fX).orNull();
                        if (y <= v50.getY()) {
                            dataSets.add(createDataSet(entry, "", Color.rgb(55, 129, 69), 5f, true));
                        } else {
                            Entry v85 = Iterables.tryFind(p85, input -> input.getX() == fX).orNull();
                            if (y <= v85.getY()) {
                                dataSets.add(createDataSet(entry, "", Color.rgb(230, 122, 58), 5f, true));
                            } else {
                                Entry v97 = Iterables.tryFind(p97, input -> input.getX() == fX).orNull();
                                if (y <= v97.getY()) {
                                    dataSets.add(createDataSet(entry, "", Color.rgb(212, 53, 62), 5f, true));
                                } else {
                                    dataSets.add(createDataSet(entry, "", Color.rgb(0, 0, 0), 5f, true));
                                }
                            }
                        }
                    }
                }
                entries.add(new Entry(x, y));
            } catch (NullPointerException ex) {
                ex.printStackTrace();

                Toast.makeText(getContext(), "Child needs to be older than 3 month to compare arm circumference", Toast.LENGTH_LONG).show();
            }
        }

        if (entries.size() > 1) {
            Collections.sort(entries, (o1, o2) -> Float.compare(o1.getX(), o2.getX()));
            dataSets.add(createDataSet(entries, "measure", Color.rgb(158, 232, 252), 1.5f, false));
        }


        mChart.setData(new LineData(dataSets));
        //mChart.invalidate();
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
