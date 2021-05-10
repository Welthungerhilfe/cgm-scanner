package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.IOException;
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

    public static double setData(Context context, double height, double weight,long age, String sex, int chartType) {


        double zScore = 100, median = 10, skew = 10, coefficient = 10;

        final String[] boys_0_2 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfl_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
        final String[] girls_0_2 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfl_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

        final String[] boys_0_6 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
        final String[] girls_0_6 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

        Measure lastMeasure = null;


        try {
            BufferedReader reader;

            String[] boys, girls;
            lastMeasure =  new Measure();
            lastMeasure.setHeight(height);
            //change this
            lastMeasure.setAge(age);
            lastMeasure.setWeight(weight);


            if (lastMeasure != null && lastMeasure.getAge() < 365 * 2 && lastMeasure.getHeight() <= 110) {
                boys = boys_0_2;
                girls = girls_0_2;
            } else {
                boys = boys_0_6;
                girls = girls_0_6;
            }


            if (sex.equals("female"))
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
            }
            reader.close();



        } catch (IOException e) {
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
}
