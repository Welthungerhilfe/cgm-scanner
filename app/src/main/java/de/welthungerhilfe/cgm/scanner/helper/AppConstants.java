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

package de.welthungerhilfe.cgm.scanner.helper;

/**
 * Created by Emerald on 2/19/2018.
 */

public class AppConstants {
    public static final int MAX_IMAGE_SIZE = 800;

    public static final long SYNC_INTERVAL = 60 * 5;
    public static final long SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final long LOG_MONITOR_INTERVAL = 10 * 1000; // 10 seconds
    public static final long MEMORY_MONITOR_INTERVAL = 60 * 1000; // 60 seconds

    public static final int MULTI_UPLOAD_BUNCH = 5;

    public static final int PAGE_SIZE = 30;

    public static final String GOOGLE_GEO_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    //public static final String STORAGE_ROOT_URL = "gs://child-growth-monitor.appspot.com";
    //public static final String STORAGE_ROOT_URL = "gs://child-growth-monitor-dev.appspot.com";

    public static final String LOCAL_CONSENT_URL = "/{qrcode}/consent/{scantimestamp}/consent/";
    public static final String STORAGE_CONSENT_URL = "/data/person/{qrcode}/consent/{scantimestamp}/";
    public static final String STORAGE_PC_URL = "/data/person/{qrcode}/measurements/{scantimestamp}/pc/";
    public static final String STORAGE_RGB_URL = "/data/person/{qrcode}/measurements/{scantimestamp}/rgb/";

    public static final String VAL_SEX_FEMALE = "female";
    public static final String VAL_SEX_MALE = "male";

    public static final String VAL_MEASURE_MANUAL = "manual";
    public static final String VAL_MEASURE_AUTO = "v0.2";

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_GERMAN = "de";
    public static final String LANG_HINDI = "hi";

    public static final String EXTRA_QR = "extra_qr";
    public static final String EXTRA_QR_BITMAP = "extra_qr_bitmap";
    public static final String EXTRA_QR_URL = "extra_qr_url";
    public static final String EXTRA_LOCATION = "extra_location";
    public static final String EXTRA_RADIUS = "extra_radius";
    public static final String EXTRA_PERSON_LIST = "extra_person_list";
    public static final String EXTRA_PERSON = "extra_person";
    public static final String EXTRA_MEASURE = "extra_measure";
    public static final String EXTRA_SCANTIMESTAMP = "extra_scantimestamp";
    public static final String EXTRA_SCANARTEFACT_SUBFOLDER = "extra_scanartefact_subfolder";
    public static final String EXTRA_ARTEFACT = "extra_artefact";
    public static final String EXTRA_TUTORIAL_AGAIN = "extra_tutorial_again";
    public static final String EXTRA_RESULT_ID = "extra_result_id";
    public static final String EXTRA_RESULT_HEIGHT = "extra_result_height";
    public static final String EXTRA_RESULT_WEIGHT = "extra_result_weight";
    public static final String EXTRA_RESULT_MUAC = "extra_result_muac";
    public static final String EXTRA_RESULT_HEAD = "extra_result_head";

    public static final String PARAM_AUTHTOKEN_TYPE = "authtoken_type";
    public static final String PARAM_AUTH_NAME = "auth_name";
    public static final String PARAM_AUTH_CONFIRM = "auth_confirm";

    public static final String CONFIG_MEASURE_VISIBILITY = "measure_visibility";
    public static final String CONFIG_TIME_TO_ALLOW_EDITING = "time_to_allow_editing";
    public static final String CONFIG_ADMINS = "admins";
    public static final String CONFIG_ALLOW_EDIT = "allow_edit";
    public static final String CONFIG_ALLOW_DELETE = "allow_delete";
    public static final String CONFIG_DEBUG = "debug";

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

    // Workflow
    public static final int CHOOSE_BABY_OR_INFANT = 0;
    public static final int LYING_BABY_SCAN = 100;
    public static final int BABY_FULL_BODY_FRONT_ONBOARDING = 101;
    public static final int BABY_FULL_BODY_FRONT_SCAN = 102;
    public static final int BABY_FULL_BODY_FRONT_RECORDING = 103;
    public static final int BABY_LEFT_RIGHT_ONBOARDING = 104;
    public static final int BABY_LEFT_RIGHT_SCAN = 105;
    public static final int BABY_LEFT_RIGHT_RECORDING = 106;
    public static final int BABY_FULL_BODY_BACK_ONBOARDING = 107;
    public static final int BABY_FULL_BODY_BACK_SCAN = 108;
    public static final int BABY_FULL_BODY_BACK_RECORDING = 109;
    public static final int STANDING_INFANT_SCAN = 200;

    public static final int UPLOADED = 201;
    public static final int UPLOADED_DELETED = 202;
    public static final int NO_NETWORK = 301;
    public static final int DIFF_HASH = 302;
    public static final int UPLOAD_ERROR = 400;
    public static final int FILE_NOT_FOUND = 404;

    public static final int SORT_DATE = 0;
    public static final int SORT_LOCATION = 1;
    public static final int SORT_WASTING = 2;
    public static final int SORT_STUNTING = 3;
}
