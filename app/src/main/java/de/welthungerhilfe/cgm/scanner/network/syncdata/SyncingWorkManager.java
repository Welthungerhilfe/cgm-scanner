package de.welthungerhilfe.cgm.scanner.network.syncdata;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncingWorkManager extends Worker {

    public static final String TAG = SyncingWorkManager.class.getSimpleName();

    public SyncingWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncAdapter.getInstance(getApplicationContext()).startPeriodicSync();
        return null;
    }

    public static void startSyncingWithWorkManager(Context context) {

        PeriodicWorkRequest SyncingWorkManager =
                new PeriodicWorkRequest.Builder(SyncingWorkManager.class, 16, TimeUnit.MINUTES).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SyncingWorkManager",
                ExistingPeriodicWorkPolicy.REPLACE,
                SyncingWorkManager);
    }

    public static Retrofit provideRetrofit() {

        //build gson
        Gson gson = new GsonBuilder().setLenient()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls().create();

        //build http client
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .build();

        //build retrofit
        Log.d(TAG, "Url backend " + getUrl());
        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .baseUrl(getUrl())
                .client(okHttpClient)
                .build();
    }

    public static String getAPI() {
        if (BuildConfig.DEBUG) {
            return "localhost";
        }
        Log.d(TAG, "Url backend " + getUrl());

        String apiURL = getUrl();
        if (apiURL.contains("https://")) {
            apiURL = apiURL.replaceFirst("https://", "");
            apiURL = apiURL.substring(0, apiURL.indexOf('.'));
        } else {
            apiURL = apiURL.replaceFirst("http://", "");
            apiURL = apiURL.substring(0, apiURL.indexOf('/'));
        }
        return apiURL;
    }

    public static String getUrl() {
        if (BuildConfig.DEBUG) {
            // development build
            return AppConstants.API_TESTING_URL;
        } else {
            Context context = AppController.getInstance().getApplicationContext();
            switch (AuthenticationHandler.getEnvironment(context)) {
                case AppConstants.ENV_SANDBOX:
                    return AppConstants.API_URL_SANDBOX;
                case AppConstants.ENV_DEMO_QA:
                    return AppConstants.API_URL_DEMO_QA;
                case AppConstants.ENV_IN_BMZ:
                    return AppConstants.API_URL_IN_BMZ;
                case AppConstants.ENV_NAMIBIA:
                    return AppConstants.API_URL_NAMIBIA;
                case AppConstants.ENV_NEPAL:
                    return AppConstants.API_URL_NEPAL;
                default:
                    Log.e(TAG, "Environment not configured");
                    System.exit(0);
                    return null;
            }
        }
    }
}
