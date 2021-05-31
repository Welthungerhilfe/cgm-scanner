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
package de.welthungerhilfe.cgm.scanner.hardware.io;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;

public class LocalPersistency {

    private static final String TAG = LocalPersistency.class.getSimpleName();
    private static final String COUNT_EXTENSION = "_COUNT";

    public static boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    public static ArrayList<Boolean> getBooleanArray(Context context, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int count = pref.getInt(key + COUNT_EXTENSION, 0);
        ArrayList<Boolean> output = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            output.add(pref.getBoolean(key + "_" + i, false));
        }
        return output;
    }

    public static long getLong(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, 0);
    }

    public static ArrayList<Long> getLongArray(Context context, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int count = pref.getInt(key + COUNT_EXTENSION, 0);
        ArrayList<Long> output = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            output.add(pref.getLong(key + "_" + i, 0));
        }
        return output;
    }

    public static String getString(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
    }

    public static ArrayList<String> getStringArray(Context context, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int count = pref.getInt(key + COUNT_EXTENSION, 0);
        ArrayList<String> output = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            output.add(pref.getString(key + "_" + i, ""));
        }
        return output;
    }

    public static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putBoolean(key, value);
        e.commit();
        LogFileUtils.logInfo(TAG, key + " set to " + value);
    }

    public static void setBooleanArray(Context context, String key, ArrayList<Boolean> value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putInt(key + COUNT_EXTENSION, value.size());
        for (int i = 0; i < value.size(); i++) {
            e.putBoolean(key + "_" + i, value.get(i));
        }
        e.commit();
    }

    public static void setLong(Context context, String key, long value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putLong(key, value);
        e.commit();
        LogFileUtils.logInfo(TAG, key + " set to " + value);
    }

    public static void setLongArray(Context context, String key, ArrayList<Long> value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putInt(key + COUNT_EXTENSION, value.size());
        for (int i = 0; i < value.size(); i++) {
            e.putLong(key + "_" + i, value.get(i));
        }
        e.commit();
    }

    public static void setString(Context context, String key, String value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putString(key, value);
        e.commit();
        LogFileUtils.logInfo(TAG, key + " set to " + value);
    }

    public static void setStringArray(Context context, String key, ArrayList<String> value) {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putInt(key + COUNT_EXTENSION, value.size());
        for (int i = 0; i < value.size(); i++) {
            e.putString(key + "_" + i, value.get(i));
        }
        e.commit();
    }
}
