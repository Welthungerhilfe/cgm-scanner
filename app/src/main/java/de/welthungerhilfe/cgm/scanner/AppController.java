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
import android.content.Intent;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;

import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.AZURE_ACCOUNT_KEY;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.AZURE_ACCOUNT_NAME;

public class AppController extends Application {
    public static final String TAG = AppController.class.getSimpleName();

    private static AppController mInstance;

    public FirebaseAuth firebaseAuth;
    public FirebaseUser firebaseUser;

    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Crashes.trackError(ex);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        /*
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);

        Intent intent = new Intent(this, HealthInfoService.class);
        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
        */

        Thread.currentThread();
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        mInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageHelper.onAttach(base));
    }

    public String getAzureConnection() {
        return String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY);
    }

    public boolean isAdmin() {
        return false;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public void prepareFirebaseUser() {
        firebaseUser = firebaseAuth.getCurrentUser();
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

        return mExtFileDir;
    }
}
