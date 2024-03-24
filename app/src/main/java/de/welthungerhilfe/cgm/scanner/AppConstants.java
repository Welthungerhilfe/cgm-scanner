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
package de.welthungerhilfe.cgm.scanner;


public class AppConstants {

    public static final String AUTH_SANDBOX = "{OAUTH_SCOPE_SANDBOX}";



    public static final String AUTH_IN_BMZ = "{OAUTH_SCOPE_PRODUCTION}";
    public static final String AUTH_NAMIBIA = "{OAUTH_SCOPE_NAMIBIA}";
    public static final String AUTH_NEPAL = "{OAUTH_SCOPE_NEPAL}";
    public static final String AUTH_UGANDA = "{OAUTH_SCOPE_UGANDA}";
    public static final String AUTH_BAN = "{OAUTH_SCOPE_BAN}";
    public static final String AUTH_MALAWI = "{OAUTH_SCOPE_MALAWI}";
    public static final String AUTH_SIERRA = "{OAUTH_SCOPE_SIERRA}";

     public static final String AUTH_DEMO_QA = "{OAUTH_SCOPE_QA}";

    public static final String API_URL_DEMO_QA = "{API_URL_QA}/";

    public static final String API_URL_SANDBOX = "{API_URL_SANDBOX}/";
    public static final String API_URL_IN_BMZ = "{API_URL_PRODUCTION}/";
    public static final String API_URL_NAMIBIA = "{API_URL_NAMIBIA}/";
    public static final String API_URL_NEPAL = "{API_URL_NEPAL}/";
    public static final String API_URL_UGANDA = "{API_URL_UGANDA}/";
    public static final String API_URL_BAN = "{API_URL_BAN}/";

    public static final String API_URL_MALAWI = "{API_URL_MALAWI}/";

    public static final String API_URL_SIERRA = "{API_URL_SIERRA}/";


    public static final String API_TESTING_URL = "http://192.168.43.252:5001/api/";

    public static final String APP_CENTER_KEY="{APP_CENTER_KEY}";


    //Environments Type
    public static final int ENV_UNKNOWN = 0;
    public static final int ENV_SANDBOX = 1;
    public static final int ENV_DEMO_QA = 2;
    public static final int ENV_IN_BMZ = 3;
    public static final int ENV_NAMIBIA = 4;
    public static final int ENV_NEPAL = 5;
    public static final int ENV_UGANDA = 6;
    public static final int ENV_BAN = 7;

    public static final int ENV_MALAWI = 8;

    public static final int ENV_SIERRA = 9;





    public static final int MAX_IMAGE_SIZE = 512;

    public static final long HEALTH_INTERVAL = 60 * 60 * 1000; // 1 hour

    public static final int MULTI_UPLOAD_BUNCH = 5;

    public static final String LOCAL_CONSENT_URL = "/{qrcode}/consent/{scantimestamp}/consent/";

    public static final String VAL_SEX_FEMALE = "female";
    public static final String VAL_SEX_MALE = "male";

    public static final String VAL_MEASURE_MANUAL = "manual";
    public static final String VAL_MEASURE_AUTO = "v1.2.3";

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_GERMAN = "de";
    public static final String LANG_HINDI = "hi";
    public static final String LANG_NEPALI = "ne";
    public static final String LANG_BANGLA = "bn";


    public static final String[] SUPPORTED_LANGUAGES = {
            LANG_ENGLISH,
            LANG_GERMAN,
            LANG_HINDI,
            LANG_NEPALI,
            LANG_BANGLA
    };

    public static final String EXTRA_QR = "extra_qr";
    public static final String EXTRA_QR_BITMAP = "extra_qr_bitmap";
    public static final String EXTRA_QR_URL = "extra_qr_url";
    public static final String EXTRA_RADIUS = "extra_radius";
    public static final String EXTRA_PERSON = "extra_person";
    public static final String EXTRA_MEASURE = "extra_measure";
    public static final String EXTRA_TUTORIAL_AGAIN = "extra_tutorial_again";

    public static final String PARAM_AUTHTOKEN_TYPE = "authtoken_type";
    public static final String PARAM_AUTH_NAME = "auth_name";
    public static final String PARAM_AUTH_CONFIRM = "auth_confirm";

    public static final String AUTHTOKEN_TYPE = "de.welthungerhilfe.cgm.scanner";
    public static final String ACCOUNT_TYPE = "de.welthungerhilfe.cgm.scanner";

    public static final int SCAN_STANDING = 0x00;
    public static final int SCAN_LYING = 0x01;

    public static final int SCAN_PREVIEW = 0;
    public static final int SCAN_STANDING_FRONT = 100;
    public static final int SCAN_STANDING_SIDE = 101;
    public static final int SCAN_STANDING_BACK = 102;
    public static final int SCAN_LYING_FRONT = 200;
    public static final int SCAN_LYING_SIDE = 201;
    public static final int SCAN_LYING_BACK = 202;

    public static final int UPLOADED = 201;
    public static final int UPLOADED_DELETED = 202;
    public static final int CONSENT_UPLOADED = 203;

    public static final int UPLOAD_ERROR = 400;
    public static final int FILE_NOT_FOUND = 404;

    public static final int SORT_DATE = 0;
    public static final int SORT_LOCATION = 1;
    public static final int SORT_WASTING = 2;
    public static final int SORT_STUNTING = 3;

    public static final int MIN_CONFIDENCE = 100;

    //scan feedback and capture parameters
    public static final int SCAN_FRAMESKIP = 6;
    public static final float TOO_NEAR = 0.7f;
    public static final float TOO_FAR = 1.5f;

    public static final String IS_FOREGROUND = "isForeGround";

    //QRScanActivity
    public static final String ACTIVITY_BEHAVIOUR_TYPE = "activityBehaviourType";

    public static final int QR_SCAN_REQUEST = 900;

    public static final int CONSENT_CAPTURED_REQUEST = 901;

    //WifiStateChangereceiverHelperService
    public static final String STOP_SERVICE = "stop_service";

    //LogFileUtils
    public static final String LOG_FILE_FOLDER = "LogFileFolder";

    //Workflows list for result generation

    public static final String APP_AUTO_DETECT_1_0 = "app_auto_detect-1.0";

    public static final String APP_HEIGHT_1_0 = "app_height-1.0";

    public static final String APP_POSE_PREDICITION_1_0 = "app_pose_predicition-1.0";

    public static final String APP_CHILD_DISTANCE_1_0 = "app_child_distance-1.0";

    public static final String APP_LIGHT_SCORE_1_0 = "app_lightscore-1.0";

    public static final String APP_BOUNDING_BOX_1_0 = "app_bounding_box-1.0";

    public static final String APP_ORIENTATION_1_0 = "app_orientation-1.0";


    public static final String[] workflowsList = {APP_AUTO_DETECT_1_0, APP_HEIGHT_1_0, APP_POSE_PREDICITION_1_0,APP_CHILD_DISTANCE_1_0,APP_LIGHT_SCORE_1_0,APP_BOUNDING_BOX_1_0,APP_ORIENTATION_1_0};

    public static final int NO_MODE_SELECTED = 0;

    public static final int CGM_MODE = 1;

    public static final int RST_MODE = 2;

    public static final int CGM_RST_MODE = 3;



}
