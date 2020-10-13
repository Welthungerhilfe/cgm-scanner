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
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class BaseActivity extends AppCompatActivity implements Connectable, Disconnectable {

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

    private Merlin merlin;
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

        merlin = new Merlin.Builder().withConnectableCallbacks().withDisconnectableCallbacks().build(this);
        merlin.registerConnectable(this);
        merlin.registerDisconnectable(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        merlin.bind();
    }

    @Override
    protected void onPause() {
        merlin.unbind();
        super.onPause();
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

    @Override
    public void onConnect() {
        // todo: Crashlytics.log(0, "network state: ", "connected at " + Utils.beautifyDateTime(new Date()));

        long timestamp = session.getConnectionTimestamp();
        if (timestamp != 0) {
            long seconds = (System.currentTimeMillis() - timestamp) / 1000;
            // todo: Crashlytics.log(0, "network state duration: ", String.format("last connection lasts for %d seconds", seconds));
        }
        session.setConnectionTimestamp(System.currentTimeMillis());
    }

    @Override
    public void onDisconnect() {
        // todo: Crashlytics.log(0, "network state: ", "disconnected at " + Utils.beautifyDateTime(new Date()));

        long timestamp = session.getConnectionTimestamp();
        if (timestamp != 0) {
            long seconds = (System.currentTimeMillis() - timestamp) / 1000;
            // todo: Crashlytics.log(0, "network state duration: ", String.format("last connection lasts for %d seconds", seconds));
        }
        session.setConnectionTimestamp(System.currentTimeMillis());
    }

    public void addResultListener(int resultCode, ResultListener listener) {
        if (map.containsKey(resultCode)) {
            map.remove(resultCode);
        }
        map.put(resultCode, listener);
    }
}