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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.appcenter.crashes.Crashes;

import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.utils.LanguageHelper;
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

    private SessionManager session;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);

        LanguageHelper.onAttach(this);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
    }

    @Override
    public void onStart() {
        super.onStart();
        session = new SessionManager(this);
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
}