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
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.appcenter.crashes.Crashes;

import java.util.HashMap;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class BaseActivity extends AppCompatActivity {

    public static final int PERMISSION_LOCATION = 0x0001;
    public static final int PERMISSION_CAMERA = 0x0002;
    public static final int PERMISSION_STORAGE = 0x0003;

    public interface ResultListener {
        void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);

        void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
    }

    private HashMap<Integer, ResultListener> map = new HashMap<>();

    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            ex.printStackTrace();
            Crashes.trackError(ex);
        }
    }

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);

        forceSelectedLanguage(this);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (map.containsKey(requestCode)) {
            map.get(requestCode).onActivityResult(requestCode, resultCode, data);
            map.remove(requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (map.containsKey(requestCode)) {
            map.get(requestCode).onRequestPermissionsResult(requestCode, permissions, grantResults);
            map.remove(requestCode);
        }
    }

    public void addResultListener(int resultCode, ResultListener listener) {
        if (map.containsKey(resultCode)) {
            map.remove(resultCode);
        }
        map.put(resultCode, listener);
    }


    public static Context forceSelectedLanguage(Context context) {
        String lang = getPersistedData(context);
        return setLanguage(context, lang);
    }

    public static Context setLanguage(Context context, String language) {
        persist(context, language);

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }

        return updateResourcesLegacy(context, language);
    }

    public static String getPersistedData(Context context) {
        return new SessionManager(context).getLanguage();
    }

    private static void persist(Context context, String language) {
        new SessionManager(context).setLanguage(language);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {

        Locale locale = new Locale(language);

        Configuration configuration = context.getResources().getConfiguration();

        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}