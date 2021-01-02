package de.welthungerhilfe.cgm.scanner.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import de.welthungerhilfe.cgm.scanner.R;

public class AuthTokenRegisterWorker extends Worker {

    Context context;
    private ISingleAccountPublicClientApplication singleAccountApp;

    private static final String TAG = AuthTokenRegisterWorker.class.getSimpleName();
    public AuthTokenRegisterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Run your task here
                Toast.makeText(getApplicationContext(), "Testing auth", Toast.LENGTH_SHORT).show();
            }
        }, 0 );
        Log.i(TAG, "doWork for Sync");
        context = getApplicationContext();
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                R.raw.auth_config_single_account,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        singleAccountApp = application;
                        updateToken();

                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, exception.toString());
                    }
                });
        return null;
    }

    public void updateToken()
    {
        String[] scopes ={"https://cgm-tagging-api-poc.azurewebsites.net/user_impersonation"};
        SessionManager session = new SessionManager(getApplicationContext());

        String authority = singleAccountApp.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
        singleAccountApp.acquireTokenSilentAsync(scopes, authority, new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                session.setAuthToken(authenticationResult.getAccessToken());
                Log.i(TAG,"this is value of auth token "+session.getAuthToken());
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Run your task here
                        Toast.makeText(getApplicationContext(), session.getAuthToken(), Toast.LENGTH_SHORT).show();
                    }
                }, 0 );

            }

            @Override
            public void onError(MsalException exception) {

            }
        });
    }
}
