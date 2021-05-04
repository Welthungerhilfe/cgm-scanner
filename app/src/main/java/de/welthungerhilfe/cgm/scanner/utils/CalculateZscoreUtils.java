package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.widget.Toast;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

class CalculateZscoreUtils {
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

    public static void setData(Context context, double height, double weight, Person person) {

        long birthday = person.getBirthday();
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        double zScore = 100, median = 10, skew = 10, coefficient = 10;

        final String[] boys_0_2 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfl_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
        final String[] girls_0_2 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfl_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

        final String[] boys_0_6 = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "acfa_boys_p_exp.txt", "hcfa_boys_p_exp.txt"};
        final String[] girls_0_6 = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "acfa_girls_p_exp.txt", "hcfa_girls_p_exp.txt"};

        int chartType=0;
        Measure lastMeasure = null;


        try {
            BufferedReader reader;

            String[] boys, girls;
            lastMeasure =  new Measure();
            lastMeasure.setHeight(height);
            //change this
            long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
            lastMeasure.setAge(age);
            lastMeasure.setWeight(weight);


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
                    if ((chartType == 0 || chartType == 1)) {
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
                if (lastMeasure.getMuac() < 11.5) { // SAM (red)
                    zScore = -3;
                } else if (lastMeasure.getMuac() < 12.5) { // MAM (yellow)
                    zScore = -2;
                } else {
                    zScore = 0;
                }
                break;
        }

        Toast.makeText(context,""+zScore, Toast.LENGTH_SHORT).show();
        //  txtZScore.setText(String.format(Locale.getDefault(), "( z-score : %.2f )", zScore));


    }
}
