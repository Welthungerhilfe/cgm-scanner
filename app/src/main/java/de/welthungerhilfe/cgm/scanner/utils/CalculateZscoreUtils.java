package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class CalculateZscoreUtils {
    public static double newZScore(double y, double Mt, double Lt, double St) {
        double Zind = (Math.pow(y / Mt, Lt) - 1) / (St * Lt);
        if (Zind >= -3 && Zind <= 3) {
            return Zind;
        } else if (Zind > 3) {
            double SD3pos = Mt * Math.pow(1 + Lt * St * 3, 1 / Lt);
            double SD23pos = SD3pos - Mt * Math.pow(1 + Lt * St * 2, 1 / Lt);
            return 3 + ((y - SD3pos) / SD23pos);
        } else {
            double SD3neg = Mt * Math.pow(1 + Lt * St * -3, 1 / Lt);
            double SD23neg = Mt * Math.pow(1 + Lt * St * -2, 1 / Lt) - SD3neg;
            return -3 + ((y - SD3neg) / SD23neg);
        }
    }

    public static double setData(Context context, double height, double weight, long age, String sex, int chartType) {

        double zScore = 100, median = 10, skew = 10, coefficient = 10;

        final String[] boys_0_6 = {"wfa_boys_0_5_zscores.json", "lhfa_boys_0_5_zscores.json", "wfh_boys_0_5_zscores.json", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
        final String[] girls_0_6 = {"wfa_girls_0_5_zscores.json", "lhfa_girls_0_5_zscores.json", "wfh_girls_0_5_zscores.json", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};
        Measure lastMeasure = null;

        try {
            lastMeasure = new Measure();
            lastMeasure.setHeight(height);
            lastMeasure.setAge(age);
            lastMeasure.setWeight(weight);

            String fileName = null;
            if (sex.equals("female")) {
                fileName = girls_0_6[chartType];
            } else {
                fileName = boys_0_6[chartType];
            }
            JSONObject jsonObject = null;
            if (lastMeasure != null) {
                if ((chartType == 0 || chartType == 1 || chartType == 2)) {

                    if (chartType == 2) {
                        jsonObject = loadJSONFromAsset(context, fileName, String.valueOf(lastMeasure.getHeight()));
                    } else {
                        jsonObject = loadJSONFromAsset(context, fileName, String.valueOf(lastMeasure.getAge()));
                    }
                    if (jsonObject == null) {
                        return 0.0;
                    }
                    try {
                        skew = Utils.parseDouble(jsonObject.getString("L"));
                        median = Utils.parseDouble(jsonObject.getString("M"));
                        coefficient = Utils.parseDouble(jsonObject.getString("S"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (chartType == 3) {
                    BufferedReader reader = null;
                    reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName), StandardCharsets.UTF_8));
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

                            if ((int) rule == (int) (lastMeasure.getAge() * 12 / 365.0)) {
                                skew = Utils.parseDouble(arr[1]);
                                median = Utils.parseDouble(arr[2]);
                                coefficient = Utils.parseDouble(arr[3]);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (chartType) {
            case 0:
            case 2:
                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = newZScore(lastMeasure.getWeight(), median, skew, coefficient);
                }
                break;
            case 1:
                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = newZScore(lastMeasure.getHeight(), median, skew, coefficient);
                }
                break;
            case 3:

                if (lastMeasure != null && median != 0 && coefficient != 0 && skew != 0) {
                    zScore = newZScore(lastMeasure.getMuac(), median, skew, coefficient);
                }
                break;

        }
        return zScore;
    }

    public static JSONObject loadJSONFromAsset(Context context, String fileName, String index) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            return obj.getJSONObject(index);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
