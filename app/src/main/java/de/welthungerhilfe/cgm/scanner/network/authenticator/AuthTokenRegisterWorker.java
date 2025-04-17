package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

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

        context = getApplicationContext();
        int environment = AuthenticationHandler.getEnvironment(context);
        LogFileUtils.logInfo(TAG, "Renewing token for " + SyncingWorkManager.getAPI());
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                AuthenticationHandler.getConfig(environment),
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        singleAccountApp = application;
                        updateToken();
                        LogFileUtils.logInfo(TAG, "Renewing token created "+application.toString());

                    }

                    @Override
                    public void onError(MsalException exception) {
                        exception.printStackTrace();
                        LogFileUtils.logError(TAG, "Renewing token error " + exception.getMessage());

                    }
                });
        return null;
    }

    public void updateToken() {
        String[] scopes = AuthenticationHandler.getScopes(getApplicationContext());
        SessionManager session = new SessionManager(getApplicationContext());

        String authority = singleAccountApp.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
        singleAccountApp.acquireTokenSilentAsync(scopes, authority, new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                session.setAuthToken(authenticationResult.getAccessToken());
                Log.d(TAG, "Token for " + SyncingWorkManager.getAPI() + " set");
                LogFileUtils.logInfo(TAG, "Renewing token created sucessfully "+authenticationResult.getAccessToken());

                SyncingWorkManager.startSyncingWithWorkManager(getApplicationContext());
            }

            @Override
            public void onError(MsalException exception) {
                exception.printStackTrace();
                LogFileUtils.logInfo(TAG, "Renewing token error "+exception.getMessage());

            }
        });
    }
}
