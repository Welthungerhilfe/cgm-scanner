/**
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.welthungerhilfe.cgm.scanner;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;
import java.io.IOException;

import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;
import de.welthungerhilfe.cgm.scanner.utils.Utils;


public class AppController extends Application {

    private static AppController mInstance;

    private SessionManager session;

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        session = new SessionManager(this);

        mInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageHelper.onAttach(base));
    }

    public String getAzureConnection() {
        if (session.getAzureAccountName() == null || session.getAzureAccountKey() == null)
            return null;
        else
            return String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", session.getAzureAccountName(), session.getAzureAccountKey());
    }

    public boolean isAdmin() {
        return false;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public String getPersonId() {
        return String.format("%s_person_%s_%s", Utils.getAndroidID(getContentResolver()), Utils.getUniversalTimestamp(), Utils.getSaltString(16));
    }

    public String getMeasureId() {
        return String.format("%s_measure_%s_%s", Utils.getAndroidID(getContentResolver()), Utils.getUniversalTimestamp(), Utils.getSaltString(16));
    }

    public String getArtifactId(String type) {
        return String.format("%s_artifact-%s_%s_%s", Utils.getAndroidID(getContentResolver()), type, Utils.getUniversalTimestamp(), Utils.getSaltString(16));
    }

    public String getArtifactId(String type, long timestamp) {
        return String.format("%s_artifact-%s_%s_%s", Utils.getAndroidID(getContentResolver()), type, timestamp, Utils.getSaltString(16));
    }

    public String getDeviceId() {
        return String.format("%s-device-%s-%s", Utils.getAndroidID(getContentResolver()), Utils.getUniversalTimestamp(), Utils.getSaltString(16));
    }

    public File getRootDirectory() {
        File mExtFileDir;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExtFileDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name_long));
        } else {
            mExtFileDir = getApplicationContext().getFilesDir();
        }

        File nomedia = new File(mExtFileDir, ".nomedia");
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return mExtFileDir;
    }

    public boolean isUploadRunning() {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (UploadService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
