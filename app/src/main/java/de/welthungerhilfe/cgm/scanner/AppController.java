/*
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

import android.content.Context;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;
import java.io.IOException;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import de.welthungerhilfe.cgm.scanner.network.module.DaggerAppComponent;
import de.welthungerhilfe.cgm.scanner.utils.IO;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AppController extends DaggerApplication {

    private static AppController mInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        mInstance = this;
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

    public File getRootDirectory(Context c) {
        //crashed on lenovo so added try-catch
        File mExtFileDir = new File(c.getApplicationInfo().dataDir);
        try {
            File oldDir = new File(Environment.getExternalStorageDirectory(), "Child Growth Monitor Scanner App");
            IO.move(oldDir, mExtFileDir);
        } catch (Exception e) {

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

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        DaggerAppComponent.builder().bindInstance(AppController.this).build().inject(this);
        return DaggerAppComponent.builder().bindInstance(AppController.this).build();
    }
}
