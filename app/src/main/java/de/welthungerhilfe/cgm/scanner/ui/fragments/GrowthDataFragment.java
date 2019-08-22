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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.CreateDataViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.ui.views.VerticalTextView;

public class GrowthDataFragment extends Fragment {
    private Context context;

    private final String[] boys = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "hcfa_boys_p_exp.txt", "acfa_boys_p_exp.txt"};
    private final String[] girls = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "hcfa_girls_p_exp.txt", "acfa_girls_p_exp.txt"};

    private LineChart mChart;

    private VerticalTextView txtYAxis;
    private TextView txtXAxis;

    private TextView txtLabel;

    private int chartType = 0;

    public String qrCode;


    private CreateDataViewModel viewModel;
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

        viewModel = ViewModelProviders.of(getActivity()).get(CreateDataViewModel.class);
        viewModel.getPersonLiveData(qrCode).observe(getViewLifecycleOwner(), person -> {
            this.person = person;
            setData();
        });
        viewModel.getMeasuresLiveData().observe(getViewLifecycleOwner(), measures -> {
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
        txtYAxis = view.findViewById(R.id.txtYAxis);
        txtXAxis = view.findViewById(R.id.txtXAxis);

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

    public void setData() {
        if (person == null || measures == null)
            return;

        txtLabel.setText(person.getSex());

        switch (chartType) {
            case 0:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_weight);
                break;
            case 1:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_height);
                break;
            case 2:
                txtXAxis.setText(R.string.axis_height);
                txtYAxis.setText(R.string.axis_weight);
                break;
            case 3:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_muac);
                break;
        }

        long birthday = person.getBirthday();

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        // ----------------------- Line for manual measures -------------------------- //
        ArrayList<Entry> entries = new ArrayList<>();

        double maxHeight = 0;

        for (Measure measure : measures) {
            if (!measure.getType().equals("manual"))
                continue;

            if (measure.getHeight() > maxHeight)
                maxHeight = measure.getHeight();

            long day = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24;

            switch (chartType) {
                case 0:
                    entries.add(new Entry(day, (float) measure.getWeight()));
                    break;
                case 1:
                    entries.add(new Entry(day, (float) measure.getHeight()));
                    break;
                case 2:
                    entries.add(new Entry((float) measure.getHeight(), (float) measure.getWeight()));
                    Collections.sort(entries, (o1, o2) -> Float.compare(o1.getX(), o2.getX()));
                    break;
                case 3:
                    entries.add(new Entry(day, (float) measure.getMuac()));
                    break;
            }
        }

        if (entries.size() > 1)
            dataSets.add(createDataSet(entries, "measure", Color.rgb(0, 0, 0), 3f, false));
        else {
            dataSets.add(createDataSet(entries, "measure", Color.rgb(0, 0, 0), 3f, true));
        }

        // ------------------------- Line for ruler values ---------------------------------- //
        long days = (System.currentTimeMillis() - birthday) / 1000 / 60 / 60 / 24 + 100;

        ArrayList<Entry> p3 = new ArrayList<>();
        ArrayList<Entry> p15 = new ArrayList<>();
        ArrayList<Entry> p50 = new ArrayList<>();
        ArrayList<Entry> p85 = new ArrayList<>();
        ArrayList<Entry> p97 = new ArrayList<>();

        try {
            BufferedReader reader = null;

            if (person.getSex().equals("female"))
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(girls[chartType]), "UTF-8"));
            else
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(boys[chartType]), "UTF-8"));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] arr = mLine.split("\t");
                float rule = 0;
                try {
                    rule = Float.parseFloat(arr[0]);
                } catch (Exception e) {
                    continue;
                }

                p3.add(new Entry(rule, Float.parseFloat(arr[6])));
                p15.add(new Entry(rule, Float.parseFloat(arr[9])));
                p50.add(new Entry(rule, Float.parseFloat(arr[11])));
                p85.add(new Entry(rule, Float.parseFloat(arr[13])));
                p97.add(new Entry(rule, Float.parseFloat(arr[16])));

                if ((chartType == 0 || chartType == 1 || chartType == 3 || chartType == 4) && rule > days)
                    break;
                /*
                if (chartType == 1 && rule > maxHeight)
                    break;
                */
                if (chartType == 2 && rule > maxHeight)
                    break;
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
