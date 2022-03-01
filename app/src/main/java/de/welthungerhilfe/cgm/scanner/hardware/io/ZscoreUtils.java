package de.welthungerhilfe.cgm.scanner.hardware.io;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

public class ZscoreUtils {

    public static class ZScoreData {
        public float median;
        public float skew;
        public float coefficient;

        public float SD3neg;
        public float SD2neg;
        public float SD0;
        public float SD2;
        public float SD3;
    }

    private static final String[] BOYS = {"lhfa_boys_0_5_zscores.json", "wfa_boys_0_5_zscores.json", "wfh_boys_0_5_zscores.json", "acfa_boys_p_exp.txt"};
    private static final String[] GIRLS = {"lhfa_girls_0_5_zscores.json", "wfa_girls_0_5_zscores.json", "wfh_girls_0_5_zscores.json", "acfa_girls_p_exp.txt"};

    public enum ChartType {HEIGHT_FOR_AGE, WEIGHT_FOR_AGE, WEIGHT_FOR_HEIGHT, MUAC_FOR_AGE }

    public static double getZScore(double y, double Mt, double Lt, double St) {
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

    public static double getZScore(ZScoreData zscoreData, double height, double weight, double muac, ChartType chartType) {
        if (zscoreData != null) {
            switch (chartType) {
                case WEIGHT_FOR_AGE:
                case WEIGHT_FOR_HEIGHT:
                    return getZScore(weight, zscoreData.median, zscoreData.skew, zscoreData.coefficient);
                case HEIGHT_FOR_AGE:
                    return getZScore(height, zscoreData.median, zscoreData.skew, zscoreData.coefficient);
                case MUAC_FOR_AGE:
                    return getZScore(muac, zscoreData.median, zscoreData.skew, zscoreData.coefficient);
            }
        }
        return 100;
    }

    public static double getZScoreSlow(Context context, double height, double weight, double muac, long age, String sex, ChartType chartType) {
        HashMap<Integer, ZScoreData> data = parseData(context, sex, chartType);
        ZScoreData zscoreData = getClosestData(data, height, age, chartType);
        return getZScore(zscoreData, height, weight, muac, chartType);
    }

    public static ZScoreData getClosestData(HashMap<Integer, ZScoreData> data, double height, long age, ChartType chartType) {
        if (data != null) {
            int target;
            if (chartType == ChartType.WEIGHT_FOR_HEIGHT) {
                target = (int) (height * 1000.0f);
            } else {
                target = (int) age;
                if (age > 5 * 365 + 2) {
                    return null;
                }
            }

            int best = Integer.MAX_VALUE;
            ZScoreData zscoreData = null;
            for (Integer key : data.keySet()) {
                int diff = Math.abs(key - target);
                if (best > diff) {
                    best = diff;
                    zscoreData = data.get(key);
                }
            }
            return zscoreData;
        }
        return null;
    }

    public static HashMap<Integer, ZScoreData> parseData(Context context, String sex, ChartType chartType) {
        String filename;
        if (sex.equals("female")) {
            filename = GIRLS[chartType.ordinal()];
        } else {
            filename = BOYS[chartType.ordinal()];
        }

        //detailed data, one value per day
        if (filename.endsWith("json")) {
            return parseJson(context, filename, chartType);
        }

        //low detailed data, one value per month
        else {
            return parseTxt(context, filename, chartType);
        }
    }

    private static HashMap<Integer, ZScoreData> parseJson(Context context, String filename, ChartType chartType) {
        HashMap<Integer, ZScoreData> output = new HashMap<>();
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);

            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String jsonKey = keys.next();
                JSONObject jsonObject = obj.getJSONObject(jsonKey);
                ZScoreData value = new ZScoreData();
                value.skew = DataFormat.parseFloat(jsonObject.getString("L"));
                value.median = DataFormat.parseFloat(jsonObject.getString("M"));
                value.coefficient = DataFormat.parseFloat(jsonObject.getString("S"));
                value.SD3neg = DataFormat.parseFloat(jsonObject.getString("SD3neg"));
                value.SD2neg = DataFormat.parseFloat(jsonObject.getString("SD2neg"));
                value.SD0 = DataFormat.parseFloat(jsonObject.getString("SD0"));
                value.SD2 = DataFormat.parseFloat(jsonObject.getString("SD2"));
                value.SD3 = DataFormat.parseFloat(jsonObject.getString("SD3"));

                float key = DataFormat.parseFloat(jsonKey);
                if (chartType == ChartType.WEIGHT_FOR_HEIGHT) {
                    output.put((int) (key * 1000), value);
                } else {
                    output.put((int) key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    private static HashMap<Integer, ZScoreData> parseTxt(Context context, String filename, ChartType chartType) {
        HashMap<Integer, ZScoreData> output = new HashMap<>();
        try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] arr = line.split("\t");
                if (DataFormat.isNumber(arr[0])) {
                    ZScoreData value = new ZScoreData();
                    value.skew = DataFormat.parseFloat(arr[1]);
                    value.median = DataFormat.parseFloat(arr[2]);
                    value.coefficient = DataFormat.parseFloat(arr[3]);
                    value.SD3neg = DataFormat.parseFloat(arr[4]);
                    value.SD2neg = DataFormat.parseFloat(arr[5]);
                    value.SD0 = DataFormat.parseFloat(arr[7]);
                    value.SD2 = DataFormat.parseFloat(arr[9]);
                    value.SD3 = DataFormat.parseFloat(arr[10]);

                    float key = DataFormat.parseFloat(arr[0]);
                    if (chartType == ChartType.WEIGHT_FOR_HEIGHT) {
                        output.put((int) (key * 1000), value);
                    } else {
                        output.put((int) (key * 365 / 12), value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
