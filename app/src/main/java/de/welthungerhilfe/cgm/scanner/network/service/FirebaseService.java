package de.welthungerhilfe.cgm.scanner.network.service;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;

public class FirebaseService {

    public static FirebaseAnalytics firebaseAnalytics;

    public static final String  SCAN_INFORM_CONSENT_START = "scan_inform_consent_started";
    public static final String  SCAN_INFORM_CONSENT_STOP = "scan_inform_consent_finished";
    public static final String  CREATE_PERSON_START = "create_person_started";
    public static final String  CREATE_PERSON_STOP = "create_person_finished";
    public static final String  SCAN_START = "scan_started";
    public static final String  SCAN_SUCCESSFUL = "scan_successfully_done";
    public static final String  SCAN_CANCELED = "scan_canceled";
    public static final String  MANUAL_MEASURE_START = "manual_measure_started";
    public static final String  MANUAL_MEASURE_STOP = "manual_measure_finished";
    public static final String  STD_TEST_START = "std_test_started";
    public static final String  STD_TEST_STOP = "std_test_finished";
    public static final String  RESULT_RECEIVED = "scan_result_received";
    public static final String  SIGN_IN_STARTED = "signin_started";
    public static final String  SIGN_IN_FINISHED = "signin_finished";


    public static final int ENV_DEMO_QA = 2;
    public static final int ENV_IN_BMZ = 3;
    public static final int ENV_NAMIBIA = 4;

    public static FirebaseAnalytics getFirebaseAnalyticsInstance(Context context){
        if(firebaseAnalytics==null){
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
        }
        return firebaseAnalytics;
    }
}
