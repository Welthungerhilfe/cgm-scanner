package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;

public class AuthTokenRegisterWorker extends Worker {

    Context context;
    private ISingleAccountPublicClientApplication singleAccountApp;

    public AuthTokenRegisterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        context = getApplicationContext();
        int environment = AuthenticationHandler.getEnvironment(context);
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                AuthenticationHandler.getConfig(environment),
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        singleAccountApp = application;
                        updateToken();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        exception.printStackTrace();
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
                SyncingWorkManager.startSyncingWithWorkManager(getApplicationContext());
            }

            @Override
            public void onError(MsalException exception) {
                exception.printStackTrace();
            }
        });
    }
}
