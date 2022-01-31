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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContactSupportDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ContextMenuDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.VerticalTextView;
import de.welthungerhilfe.cgm.scanner.utils.CalculateZscoreUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class GrowthDataFragment extends Fragment implements IFillFormatter {

    private enum ValueType {LINE, LINE_THIN, LINE_FILL_UP, LINE_FILL_DOWN, CIRCLE, BUBBLE, TRIANGLE}

    private final int MEASURE_COLOR = Color.rgb(158, 232, 252);
    private final int ZSCORE_COLOR_0 = Color.argb(64, 55, 129, 69);
    private final int ZSCORE_COLOR_2 = Color.argb(0, 230, 122, 58);
    private final int ZSCORE_COLOR_3 = Color.argb(0, 212, 53, 62);
    private final int BLUE_COLOR_DOT = Color.rgb(15, 24, 241);

    private LineChart mChart;
    private VerticalTextView txtYAxis;
    private TextView txtXAxis;
    private TextView txtLabel;
    private TextView txtZScore;
    private LinearLayout lytNotif;
    private TextView txtNotifTitle;
    private TextView txtNotifMessage;
    View first_dot, second_dot, third_dot;
    TextView first_dot_text, second_dot_text, third_dot_text;
    LinearLayout ll_dot_legend;

    private CalculateZscoreUtils.ChartType chartType = CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE;

    private String qrCode;

    private ArrayList<Entry> SD3neg, SD2neg;
    private ArrayList<Entry> SD0, SD2, SD3;

    public static GrowthDataFragment getInstance(String qrCode) {
        GrowthDataFragment fragment = new GrowthDataFragment();
        fragment.qrCode = qrCode;

        return fragment;
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
        ll_dot_legend = view.findViewById(R.id.ll_dot_legend);
        first_dot = view.findViewById(R.id.first_dot);
        second_dot = view.findViewById(R.id.second_dot);
        third_dot = view.findViewById(R.id.third_dot);
        first_dot_text = view.findViewById(R.id.first_dot_text);
        second_dot_text = view.findViewById(R.id.second_dot_text);
        third_dot_text = view.findViewById(R.id.third_dot_text);

        mChart = view.findViewById(R.id.chart1);
        MaterialSpinner dropChart = view.findViewById(R.id.dropChart);

        String[] filters = getResources().getStringArray(R.array.filters);
        dropChart.setItems(filters);
        dropChart.setOnItemSelectedListener((MaterialSpinner.OnItemSelectedListener<String>) (view1, position, id, item) -> {
            chartType = CalculateZscoreUtils.ChartType.values()[position];
            setData();
        });

        View contextMenu = view.findViewById(R.id.contextMenuButton);
        contextMenu.setOnClickListener(v -> {
            Person person = PersonRepository.getInstance(getContext()).findPersonByQr(qrCode);
            if (person != null) {
                String id = person.getId();
                String qrCode = person.getQrcode();
                BaseActivity activity = (BaseActivity) getActivity();
                new ContextMenuDialog(getContext(), new ContextMenuDialog.Item[]{
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
        mChart.getLegend().setEnabled(false);

        mChart.getAxisLeft().setEnabled(true);
        mChart.getAxisLeft().setDrawGridLines(true);

        mChart.getXAxis().setDrawAxisLine(true);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getXAxis().setDrawGridLines(true);
    }

    public void setData() {
        Person person = PersonRepository.getInstance(getContext()).findPersonByQr(qrCode);
        if (person == null) {
            return;
        }
        List<Measure> measures = null;
        if (chartType == CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE) {
            measures = MeasureRepository.getInstance(getContext()).getAllMeasuresByPersonId(person.getId());
        } else {
            measures = MeasureRepository.getInstance(getContext()).getManualMeasures(person.getId());
        }

        if (measures == null || measures.isEmpty()) {
            mChart.clear();
            return;
        }
        Measure lastMeasure = measures.get(0);
        txtLabel.setText(person.getSex());

        //fix age of the last measurement
        long birthday = person.getBirthday();
        long age = (lastMeasure.getDate() - birthday) / 1000 / 60 / 60 / 24;
        lastMeasure.setAge(age);

        //get data
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        HashMap<Integer, CalculateZscoreUtils.ZScoreData> data = CalculateZscoreUtils.parseData(getContext(), person.getSex(), chartType);
        CalculateZscoreUtils.ZScoreData zscoreData = CalculateZscoreUtils.getClosestData(data, lastMeasure.getHeight(), lastMeasure.getAge(), chartType);
        convertData(data);

        //show data
        showAxisLegend();
        showData(dataSets, true);
        showManualMeasures(measures, dataSets);
        showZScore(zscoreData, lastMeasure);
        mChart.setData(new LineData(dataSets));
        mChart.animateX(0);
    }

    private void convertData(HashMap<Integer, CalculateZscoreUtils.ZScoreData> data) {
        SD3neg = new ArrayList<>();
        SD2neg = new ArrayList<>();
        SD0 = new ArrayList<>();
        SD2 = new ArrayList<>();
        SD3 = new ArrayList<>();
        if (data != null) {
            List<Integer> sortedKeys = new ArrayList<>(data.keySet());
            Collections.sort(sortedKeys);
            for (Integer key : sortedKeys) {
                float x = key;
                if (chartType == CalculateZscoreUtils.ChartType.WEIGHT_FOR_HEIGHT) {
                    x /= 1000.0f;
                } else {
                    x *= 12.0f / 365.0f;
                }
                CalculateZscoreUtils.ZScoreData item = data.get(key);
                if (item != null) {
                    SD3neg.add(new Entry(x, item.SD3neg));
                    SD2neg.add(new Entry(x, item.SD2neg));
                    SD0.add(new Entry(x, item.SD0));
                    SD2.add(new Entry(x, item.SD2));
                    SD3.add(new Entry(x, item.SD3));
                }
            }
        }
    }

    private void showAxisLegend() {
        switch (chartType) {
            case HEIGHT_FOR_AGE:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_height);
                first_dot_text.setText(R.string.height_for_age_manual);
                second_dot_text.setText(R.string.height_for_age_scan);
                third_dot_text.setText(R.string.percentile_error);
                ll_dot_legend.setVisibility(View.VISIBLE);
                break;

            case WEIGHT_FOR_AGE:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_weight);
                first_dot_text.setText(R.string.weight_for_age_manual);
                second_dot_text.setText(R.string.weight_for_age_scan);
                third_dot_text.setText(R.string.percentile_error);
                ll_dot_legend.setVisibility(View.VISIBLE);
                break;

            case WEIGHT_FOR_HEIGHT:
                txtXAxis.setText(R.string.axis_height);
                txtYAxis.setText(R.string.axis_weight);
                ll_dot_legend.setVisibility(View.VISIBLE);
                first_dot_text.setText(R.string.manual_measure);
                second_dot_text.setText(R.string.scan_measure);
                third_dot_text.setText(R.string.percentile_error);
                break;

            case MUAC_FOR_AGE:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_muac);
                first_dot_text.setText(R.string.muac_for_age_manual);
                second_dot_text.setText(R.string.muac_for_age_scan);
                third_dot_text.setText(R.string.percentile_error);
                ll_dot_legend.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showData(ArrayList<ILineDataSet> dataSets, boolean filled) {
        if (filled) {
            dataSets.add(createDataSet(ValueType.LINE_FILL_UP, SD0, ZSCORE_COLOR_0));
            dataSets.add(createDataSet(ValueType.LINE_FILL_UP, SD2, ZSCORE_COLOR_2));
            dataSets.add(createDataSet(ValueType.LINE_FILL_UP, SD3, ZSCORE_COLOR_3));
            dataSets.add(createDataSet(ValueType.LINE_FILL_DOWN, SD0, ZSCORE_COLOR_0));
            dataSets.add(createDataSet(ValueType.LINE_FILL_DOWN, SD2neg, ZSCORE_COLOR_2));
            dataSets.add(createDataSet(ValueType.LINE_FILL_DOWN, SD3neg, ZSCORE_COLOR_3));
        } else {
            dataSets.add(createDataSet(ValueType.LINE, SD0, ZSCORE_COLOR_0));
            dataSets.add(createDataSet(ValueType.LINE, SD2, ZSCORE_COLOR_2));
            dataSets.add(createDataSet(ValueType.LINE, SD3, ZSCORE_COLOR_3));
            dataSets.add(createDataSet(ValueType.LINE, SD0, ZSCORE_COLOR_0));
            dataSets.add(createDataSet(ValueType.LINE, SD2neg, ZSCORE_COLOR_2));
            dataSets.add(createDataSet(ValueType.LINE, SD3neg, ZSCORE_COLOR_3));
        }
    }

    private void showDiagnose(double zScore) {

        if (zScore < -3) { // SAM
            switch (chartType) {
                case HEIGHT_FOR_AGE:
                    txtNotifTitle.setText(R.string.sam_hfa_title);
                    txtNotifMessage.setText(R.string.sam_hfa_message);
                    break;
                case WEIGHT_FOR_AGE:
                    txtNotifTitle.setText(R.string.sam_wfa_title);
                    txtNotifMessage.setText(R.string.sam_wfa_message);
                    break;
                case WEIGHT_FOR_HEIGHT:
                    txtNotifTitle.setText(R.string.sam_wfh_title);
                    txtNotifMessage.setText(R.string.sam_wfh_message);
                    break;
                case MUAC_FOR_AGE:
                    txtNotifTitle.setText(R.string.sam_acfa_title);
                    txtNotifMessage.setText(R.string.sam_acfa_message);
                    break;
            }

            lytNotif.setBackgroundResource(R.color.colorRed);
            lytNotif.setVisibility(View.VISIBLE);
        } else if (zScore < -2 && zScore >= -3) { // MAM
            switch (chartType) {
                case HEIGHT_FOR_AGE:
                    txtNotifTitle.setText(R.string.mam_hfa_title);
                    txtNotifMessage.setText(R.string.mam_hfa_message);
                    break;
                case WEIGHT_FOR_AGE:
                    txtNotifTitle.setText(R.string.mam_wfa_title);
                    txtNotifMessage.setText(R.string.mam_wfa_message);
                    break;
                case WEIGHT_FOR_HEIGHT:
                    txtNotifTitle.setText(R.string.mam_wfh_title);
                    txtNotifMessage.setText(R.string.mam_wfh_message);
                    break;
                case MUAC_FOR_AGE:
                    txtNotifTitle.setText(R.string.mam_acfa_title);
                    txtNotifMessage.setText(R.string.mam_acfa_message);
                    break;
            }

            lytNotif.setBackgroundResource(R.color.colorYellow);
            lytNotif.setVisibility(View.VISIBLE);
        } else { // Healthy
            lytNotif.setVisibility(View.GONE);
        }
    }

    private void showManualMeasures(List<Measure> measures, ArrayList<ILineDataSet> dataSets) {

        ArrayList<Entry> entries = new ArrayList<>();

        for (Measure measure : measures) {
            float x = 0, y = 0;
            switch (chartType) {
                case HEIGHT_FOR_AGE:
                    x = measure.getAge() * 12 / 365.0f;
                    y = (float) measure.getHeight();
                    break;
                case WEIGHT_FOR_AGE:
                    x = measure.getAge() * 12 / 365.0f;
                    y = (float) measure.getWeight();
                    break;
                case WEIGHT_FOR_HEIGHT:
                    DecimalFormat decimalFormat = new DecimalFormat("#.#");
                    x = Utils.parseFloat(decimalFormat.format(measure.getHeight()));
                    y = (float) measure.getWeight();
                    break;
                case MUAC_FOR_AGE:
                    x = measure.getAge() * 12 / 365.0f;
                    y = (float) measure.getMuac();
                    break;
            }

            if (x == 0 || y == 0)
                continue;

            try {
                ArrayList<Entry> entry = new ArrayList<>();

                entry.add(new Entry(x, y));
                if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
                    dataSets.add(createDataSet(ValueType.TRIANGLE, entry, ZSCORE_COLOR_0));
                    entries.add(new Entry(x, y));
                } else if (chartType == CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE && !measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL)) {
                    dataSets.add(createDataSet(ValueType.CIRCLE, entry, BLUE_COLOR_DOT));
                    if(measure.getReceived_at() > 0 && measure.getNegative_height_error() < 0 && measure.getPositive_height_error() > 0) {
                        ArrayList<Entry> errorEntry = new ArrayList<>();
                        ArrayList<Entry> posErrorEntry = new ArrayList<>();
                        ArrayList<Entry> negativeErrorEntry = new ArrayList<>();
                        posErrorEntry.add(new Entry(x, (float) (y + measure.getPositive_height_error())));
                        negativeErrorEntry.add(new Entry(x, (float) (y + measure.getNegative_height_error())));
                        errorEntry.addAll(posErrorEntry);
                        errorEntry.addAll(negativeErrorEntry);
                        dataSets.add(createDataSet(ValueType.BUBBLE, posErrorEntry, BLUE_COLOR_DOT));
                        dataSets.add(createDataSet(ValueType.BUBBLE, negativeErrorEntry, BLUE_COLOR_DOT));
                        dataSets.add(createDataSet(ValueType.LINE, errorEntry, BLUE_COLOR_DOT));
                    }
                }
            } catch (Exception e) {
                LogFileUtils.logException(e);
            }
        }
        if (chartType != CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE) {
            if (entries.size() > 1) {
                entries.sort((o1, o2) -> Float.compare(o1.getX(), o2.getX()));
            }
            dataSets.add(0, createDataSet(ValueType.LINE_THIN, entries, MEASURE_COLOR));
        }
    }

    private void showZScore(CalculateZscoreUtils.ZScoreData zscoreData, Measure lastMeasure) {
        if (zscoreData != null) {
            double zScore = CalculateZscoreUtils.getZScore(zscoreData, lastMeasure.getHeight(), lastMeasure.getWeight(), lastMeasure.getMuac(), chartType);
            txtZScore.setText(String.format(Locale.getDefault(), "( z-score : %.2f )", zScore));
            txtZScore.setVisibility(View.VISIBLE);
            showDiagnose(zScore);
        } else {
            txtZScore.setVisibility(View.GONE);
            lytNotif.setVisibility(View.GONE);
        }
    }

    protected LineDataSet createDataSet(ValueType type, ArrayList<Entry> values, int color) {
        String label = "";
        if (type == ValueType.LINE_FILL_UP) {
            label = "pos";
        } else if (type == ValueType.LINE_FILL_DOWN) {
            label = "neg";
        }

        int rgb = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
        LineDataSet dataSet = new LineDataSet(values, label);
        Path path = new Path();
        float shapeSize = 10;
        switch (type) {
            case LINE:
            case LINE_THIN:
                dataSet.setDrawShapes(false);
                dataSet.setColor(rgb);
                break;
            case LINE_FILL_UP:
            case LINE_FILL_DOWN:
                dataSet.setDrawShapes(false);
                dataSet.setDrawFilled(true);
                dataSet.setFillAlpha(Color.alpha(color));
                dataSet.setFillFormatter(this);
                dataSet.setFillColor(rgb);
                dataSet.setColor(Color.TRANSPARENT);
                break;
            case CIRCLE:
                dataSet.setDrawShapes(true);
                dataSet.setDrawShapesShadow(true);
                path.addCircle(0, 0, shapeSize, Path.Direction.CW);
                break;
            case BUBBLE:
                dataSet.setDrawShapes(true);
                dataSet.setDrawShapesShadow(true);
                path.addCircle(0, 0, shapeSize, Path.Direction.CW); //fill
                path.addCircle(0, 0, shapeSize / 2, Path.Direction.CCW); //hole
                break;
            case TRIANGLE:
                dataSet.setDrawShapes(true);
                dataSet.setDrawShapesShadow(true);
                path.moveTo(0, -shapeSize);
                path.lineTo(-shapeSize, shapeSize);
                path.lineTo(shapeSize, shapeSize);
                path.close();
                break;
        }
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setLineWidth(type == ValueType.LINE_THIN ? 1.0f : 1.5f);
        dataSet.setShapeColor(rgb);
        dataSet.setShapePath(path);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(rgb);
        return dataSet;
    }

    @Override
    public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
        return dataSet.getLabel().compareTo("pos") == 0 ? mChart.getYChartMax() : mChart.getYChartMin();
    }
}
