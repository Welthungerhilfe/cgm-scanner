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
import android.os.Build;

import com.google.gson.Gson;

import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.DataFormat;

public class SessionManager {
    private final String PREF_KEY_USER = "pref_key_user";

    private final String KEY_USER_SIGNED = "key_user_signed";
    private final String KEY_USER_EMAIL = "key_user_email";
    private final String KEY_USER_TOKEN = "key_user_token";
    private final String KEY_USER_LOCATION_LATITUDE = "key_user_location_latitude";
    private final String KEY_USER_LOCATION_LONGITUDE = "key_user_location_longitude";
    private final String KEY_USER_LOCATION_ADDRESS = "key_user_location_address";
    private final String KEY_SYNC_TIMESTAMP = "sync_timestamp";
    private final String KEY_BACKUP_TIMESTAMP = "backup_timestamp";
    private final String KEY_LANGUAGE = "key_language";
    private final String KEY_CONNECTION_TIMESTAMP = "key_connection_timestamp";
    private final String KEY_TUTORIAL = "key_tutorial";
    private final String KEY_REMOTE_CONFIG = "key_remote_config";
    private final String SELECTED_ENVIRONMENT = "selected_environment";
    private final String LOG_FILE = "log_file";
    private final String KEY_STD_TEST_QR_CODE = "key_std_test_qr_code";
    private final String KEY_PERSON_SYNC_TIMESTAMP = "person_sync_timestamp";
    private final String KEY_SESSION_ERROR = "key_session_error";
    private final String KEY_LAST_SYNC_DAILY_REPORT= "key_last_sync_daily_report";
    private final String KEY_ENVIRONMENT_MODE= "key_environment_mode";
    private final String KEY_SELECTED_MODE= "key_selected_mode";
    private final String KEY_AR_CORE_CALI= "key_ar_core_cali";

    private final String KEY_LOCATION_INDIA_VERSION= "key_location_india_version";

    private final String KEY_LOCATION_DEMO_QA_VERSION= "key_location_demo_qa_version";

    private final String KEY_LOCATLITY= "key_locality";

    private final String BACKGROUND_TASK_COUNT = "background_task_count";

    private final String RGB_SENSOR= "RGB_SENSOR";

    private final String TOF_SENSOR= "TOF_SENSOR";













    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context ctx) {
        pref = ctx.getSharedPreferences(PREF_KEY_USER, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setSigned(boolean signed) {
        editor.putBoolean(KEY_USER_SIGNED, signed);

        editor.commit();
    }

    public boolean isSigned() {
        if (getEnvironment() == AppConstants.ENV_UNKNOWN) {
            return false;
        } else if (BuildConfig.DEBUG) {
            return pref.getBoolean(KEY_USER_SIGNED, false);
        } else {
            return pref.getBoolean(KEY_USER_SIGNED, false) && (getAuthToken() != null);
        }
    }

    public void setUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);

        editor.commit();
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public void setLanguage(String code) {
        editor.putString(KEY_LANGUAGE, code);

        editor.commit();
    }

    public String getLanguage() {
        String current = getCurrentLanguage();
        String setting = pref.getString(KEY_LANGUAGE, null);
        if (setting == null) {
            setLanguage(current);
            return current;
        }
        return setting;
    }

    private String getCurrentLanguage() {
        boolean supported = false;
        String output = Locale.getDefault().getLanguage();
        if (output.length() > 2) {
            output = output.substring(0, 2);
        }
        for (String lang : AppConstants.SUPPORTED_LANGUAGES) {
            if (lang.compareTo(output) == 0) {
                supported = true;
            }
        }
        if (!supported) {
            output = AppConstants.LANG_ENGLISH;
        }
        return output;
    }

    public String getDevice() {
        return Build.BRAND + " " + Build.MODEL;
    }

    public void setLocation(Loc location) {
        editor.putString(KEY_USER_LOCATION_LATITUDE, Double.toString(location.getLatitude()));
        editor.putString(KEY_USER_LOCATION_LONGITUDE, Double.toString(location.getLongitude()));
        editor.putString(KEY_USER_LOCATION_ADDRESS, location.getAddress());

        editor.commit();
    }

    public Loc getLocation() {
        Loc location = new Loc();
        location.setLatitude(DataFormat.parseDouble(pref.getString(KEY_USER_LOCATION_LATITUDE, "0")));
        location.setLongitude(DataFormat.parseDouble(pref.getString(KEY_USER_LOCATION_LONGITUDE, "0")));
        location.setAddress(pref.getString(KEY_USER_LOCATION_ADDRESS, ""));

        return location;
    }

    public void setBackupTimestamp(long timestamp) {
        editor.putLong(KEY_BACKUP_TIMESTAMP, timestamp);

        editor.commit();
    }

    public long getBackupTimestamp() {
        return pref.getLong(KEY_BACKUP_TIMESTAMP, 0);
    }

    public void setSyncTimestamp(long timestamp) {
        editor.putLong(KEY_SYNC_TIMESTAMP, timestamp);

        editor.commit();
    }

    public long getSyncTimestamp() {
        return pref.getLong(KEY_SYNC_TIMESTAMP, 0);
    }

    public void setTutorial(boolean tutorial) {
        editor.putBoolean(KEY_TUTORIAL, tutorial);

        editor.commit();
    }

    public boolean getTutorial() {
        return pref.getBoolean(KEY_TUTORIAL, false);
    }

    public void saveRemoteConfig(RemoteConfig config) {
        Gson gson = new Gson();
        editor.putString(KEY_REMOTE_CONFIG, gson.toJson(config));

        editor.commit();
    }

    public RemoteConfig getRemoteConfig() {
        String jsonStr = pref.getString(KEY_REMOTE_CONFIG, null);

        if (jsonStr == null) {
            return null;
        } else {
            Gson gson = new Gson();
            return gson.fromJson(jsonStr, RemoteConfig.class);
        }
    }

    public void setAuthToken(String idToken) {
        editor.putString(KEY_USER_TOKEN, idToken);

        editor.commit();
    }

    public String getAuthToken() {
        if (BuildConfig.DEBUG) {
            return null;

        } else {
        return pref.getString(KEY_USER_TOKEN, null);
         }
    }

    public String getAuthTokenWithBearer() {
       if (BuildConfig.DEBUG) {
            return null;


        } else {
        return "bearer " + getAuthToken();
          }

    }

    public void setEnvironment(int environment) {
        editor.putInt(SELECTED_ENVIRONMENT, environment);
        editor.commit();
    }

    public int getEnvironment() {
        return pref.getInt(SELECTED_ENVIRONMENT, AppConstants.ENV_UNKNOWN);
    }

    public void setEnvironmentMode(int environmentMode) {
        editor.putInt(KEY_ENVIRONMENT_MODE, environmentMode);
        editor.commit();
    }

    public int getEnvironmentMode() {
        return pref.getInt(KEY_ENVIRONMENT_MODE, AppConstants.NO_MODE_SELECTED);
    }

    public void setSelectedMode(int selectedMode) {
        editor.putInt(KEY_SELECTED_MODE, selectedMode);
        editor.commit();
    }

    public int getSelectedMode() {
        return pref.getInt(KEY_SELECTED_MODE, AppConstants.NO_MODE_SELECTED);
    }

    public void setSessionError(int count) {
        editor.putInt(KEY_SESSION_ERROR, count);
        editor.commit();
    }

    public int getSessionError() {
        return pref.getInt(KEY_SESSION_ERROR, 0);
    }

    public String getCurrentLogFilePath() {
        return pref.getString(LOG_FILE, null);
    }

    public void setCurrentLogFilePath(String name) {
        editor.putString(LOG_FILE, name);
        editor.commit();
    }

    public String getStdTestQrCode() {
        return pref.getString(KEY_STD_TEST_QR_CODE, null);
    }

    public void setStdTestQrCode(String qrCode) {
        editor.putString(KEY_STD_TEST_QR_CODE, qrCode);
        editor.commit();
    }

    public void setPersonSyncTimestamp(long timestamp) {
        editor.putLong(KEY_PERSON_SYNC_TIMESTAMP, timestamp);
        editor.commit();
    }

    public long getLastPersonSyncTimestamp() {
        return pref.getLong(KEY_PERSON_SYNC_TIMESTAMP, 0);
    }

    public void setLastSyncDailyReport(long timestamp) {
        editor.putLong(KEY_LAST_SYNC_DAILY_REPORT, timestamp);
        editor.commit();
    }

    public long getLastSyncDailyReport() {
        return pref.getLong(KEY_LAST_SYNC_DAILY_REPORT, 0);
    }

    //Store and retrive workflows ids
    public String getWorkFlowId(String key) {
        return pref.getString(key, null);
    }

    public void setWorkFlowId(String key, String values) {
        editor.putString(key, values);
        editor.commit();
    }

    public String getArcoreCaliFile() {
        return pref.getString(KEY_AR_CORE_CALI, null);
    }

    public void setArcoreCaliFile(String values) {
        editor.putString(KEY_AR_CORE_CALI, values);
        editor.commit();
    }

    public String getRgbSensor() {
        return pref.getString(RGB_SENSOR, "NOT DONE");
    }

    public void setRgbSensor(String values) {
        editor.putString(RGB_SENSOR, values);
        editor.commit();
    }

    public String getTofSensor() {
        return pref.getString(TOF_SENSOR, "NOT DONE");
    }

    public void setTofSensor(String values) {
        editor.putString(TOF_SENSOR, values);
        editor.commit();
    }
    public void setLocationIndiaVersion(int environment) {
        editor.putInt(KEY_LOCATION_INDIA_VERSION, environment);
        editor.commit();
    }

    public int getIndiaVersionLocation() {
        return pref.getInt(KEY_LOCATION_INDIA_VERSION,0);
    }

    public void setLocationDemoQAVersion(int environment) {
        editor.putInt(KEY_LOCATION_DEMO_QA_VERSION, environment);
        editor.commit();
    }

    public int getDemoQaVersionLocation() {
        return pref.getInt(KEY_LOCATION_DEMO_QA_VERSION,0);
    }

    public void setLocality(String locality){
        editor.putString(KEY_LOCATLITY, locality);
        editor.commit();
    }

    public String getLocality() {
        return pref.getString(KEY_LOCATLITY, null);
    }


    public void clearSharedpreference(){
        editor.clear();
        editor.commit();
    }

    public void setBackgrounThreadCount(int count) {
        editor.putInt(BACKGROUND_TASK_COUNT, count);
        editor.commit();
    }

    public int getBackgrounThreadCount() {
        return pref.getInt(BACKGROUND_TASK_COUNT, 0);
    }
}
