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
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.appcenter.crashes.Crashes;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class BaseActivity extends AppCompatActivity implements Connectable, Disconnectable {
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

        Thread.currentThread().setDefaultUncaughtExceptionHandler(new ExceptionHandler());
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

    public void sleep(long miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}