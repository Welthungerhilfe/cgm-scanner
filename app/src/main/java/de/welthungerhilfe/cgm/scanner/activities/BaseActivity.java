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

package de.welthungerhilfe.cgm.scanner.activities;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class BaseActivity extends AppCompatActivity implements Connectable, Disconnectable {
    private boolean running = false;
    private Merlin merlin;
    private SessionManager session;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        running = true;

        session = new SessionManager(this);

        merlin = new Merlin.Builder().withConnectableCallbacks().withDisconnectableCallbacks().build(this);
        merlin.registerConnectable(this);
        merlin.registerDisconnectable(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;

        hideProgressDialog();
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

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        if (running)
            mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void openKeyboard(EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(editText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        editText.requestFocus();
    }

    public void closeKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public void onConnect() {
        Log.e("Network State", "Connected");
        Crashlytics.setString("Network State", "Connected");

        long timestamp = session.getConnectionTimestamp();
        if (timestamp != 0) {
            long seconds = (System.currentTimeMillis() - timestamp) / 1000;
            Log.e("Network Last Duration", String.valueOf(seconds));
            Crashlytics.setLong("Network Last Duration", seconds);
        }
        session.setConnectionTimestamp(System.currentTimeMillis());
    }

    @Override
    public void onDisconnect() {
        Log.e("Network State", "Disconnected");
        Crashlytics.setString("Network State", "Disconnected");

        long timestamp = session.getConnectionTimestamp();
        if (timestamp != 0) {
            long seconds = (System.currentTimeMillis() - timestamp) / 1000;
            Log.e("Network Last Duration", String.valueOf(seconds));
            Crashlytics.setLong("Network Last Duration", seconds);
        }
        session.setConnectionTimestamp(System.currentTimeMillis());
    }
}