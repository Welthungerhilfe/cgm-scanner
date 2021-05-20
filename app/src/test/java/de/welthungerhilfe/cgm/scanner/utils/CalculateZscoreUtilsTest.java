package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;


import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CalculateZscoreUtilsTest {

    double[] zScore = new double[3];
    private Context context;
    double error = 0.064;
    double zScoreWFA, zScoreHFA, zScoreWFH;

    @Before
    public void setUpContext() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void ZscoreTest_1() {
        zScoreWFA = -0.57;
        zScoreHFA = -0.07;
        zScoreWFH = -0.81;

        zScore = calculateAllZscore(context, 84.8, 10.399, 683, "female");
        System.out.println("ZscoreTest_1=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_2() {
        zScoreWFA = 0.53;
        zScoreHFA = -0.19;
        zScoreWFH = 0.83;

        zScore = calculateAllZscore(context, 83.5, 11.699, 653, "female");
        System.out.println("ZscoreTest_2=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_3() {
        zScoreWFA = 0.74;
        zScoreHFA = 0.32;
        zScoreWFH = 0.82;

        zScore = calculateAllZscore(context, 80.5, 11.0, 518, "female");
        System.out.println("ZscoreTest_3=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_4() {
        zScoreWFA = 1.48;
        zScoreHFA = 0.41;
        zScoreWFH = 1.76;

        zScore = calculateAllZscore(context, 110.1, 21.799, 1737, "male");
        System.out.println("ZscoreTest_5=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_5() {
        zScoreWFA = -1.14;
        zScoreHFA = -1.36;
        zScoreWFH = -0.72;

        zScore = calculateAllZscore(context, 75.0, 8.600, 496, "female");
        System.out.println("ZscoreTest_5=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_6() {
        zScoreWFA = -0.38;
        zScoreHFA = 0.07;
        zScoreWFH = -0.67;

        zScore = calculateAllZscore(context, 106.0, 16.100, 1616, "female");
        System.out.println("ZscoreTest_6=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_7() {
        zScoreWFA = 0.22;
        zScoreHFA = 0.85;
        zScoreWFH = -0.82;

        zScore = calculateAllZscore(context, 55.2, 4.300, 30, "female");
        System.out.println("ZscoreTest_7=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_8() {
        zScoreWFA = -0.62;
        zScoreHFA = -1.51;
        zScoreWFH = 0.51;

        zScore = calculateAllZscore(context, 63.5, 7.199, 166, "male");
        System.out.println("ZscoreTest_8=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_9() {
        zScoreWFA = -0.36;
        zScoreHFA = -1.06;
        zScoreWFH = 0.27;

        zScore = calculateAllZscore(context, 82.5, 11.199, 657, "male");
        System.out.println("ZscoreTest_9=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_10() {
        zScoreWFA = -0.18;
        zScoreHFA = -0.52;
        zScoreWFH = 0.08;

        zScore = calculateAllZscore(context, 84.1, 11.100, 712, "female");
        System.out.println("ZscoreTest_10=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    @Test
    public void ZscoreTest_11() {
        zScoreWFA = 0.76;
        zScoreHFA = 0.15;
        zScoreWFH = 0.95;

        zScore = calculateAllZscore(context, 92.3, 14.5, 907, "male");
        System.out.println("ZscoreTest_11=> " + zScore[0] + " " + zScore[1] + " " + zScore[2]);

        assertThat(zScore[0], closeTo(zScoreWFA, error));
        assertThat(zScore[1], closeTo(zScoreHFA, error));
        assertThat(zScore[2], closeTo(zScoreWFH, error));
    }

    public double[] calculateAllZscore(Context context, double height, double weight, long age, String sex) {
        zScore[0] = CalculateZscoreUtils.setData(context, height, weight, age, sex, CalculateZscoreUtils.ChartType.WEIGHT_FOR_AGE);
        zScore[1] = CalculateZscoreUtils.setData(context, height, weight, age, sex, CalculateZscoreUtils.ChartType.HEIGHT_FOR_AGE);
        zScore[2] = CalculateZscoreUtils.setData(context, height, weight, age, sex, CalculateZscoreUtils.ChartType.WEIGHT_FOR_HEIGHT);
        return zScore;
    }


}
