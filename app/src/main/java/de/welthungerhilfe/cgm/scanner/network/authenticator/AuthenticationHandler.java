/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.minidev.json.JSONArray;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AuthenticationHandler {


    public interface IAuthenticationCallback {

        void processAuth(String email, String token, boolean feedback);
    }

    private static final String ENVIRONMENT_KEY = "ENVIRONMENT_KEY";
    private static final String TAG = AuthenticationHandler.class.toString();

    private Activity activity;
    private Context context;
    private SessionManager session;
    private ISingleAccountPublicClientApplication singleAccountApp;
    private IAuthenticationCallback callback;

    public AuthenticationHandler(Activity activity, IAuthenticationCallback callback, Runnable onFail) {

        this.activity = activity;
        this.callback = callback;
        context = activity.getApplicationContext();
        session = new SessionManager(context);

        // Creates a PublicClientApplication object
        PublicClientApplication.createSingleAccountPublicClientApplication(context, getConfig(getEnvironment(context)),
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        singleAccountApp = application;
                        if (!session.isSigned()) {
                            singleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                                @Override
                                public void onSignOut() {
                                    Log.d(TAG, "Signed out");
                                    doSignInAction(onFail);
                                }

                                @Override
                                public void onError(@NonNull MsalException exception) {
                                    Log.e(TAG, exception.toString());
                                    doSignInAction(onFail);
                                }
                            });
                        } else {
                            doSignInAction(onFail);
                        }
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, exception.toString());
                        onFail.run();
                    }
                });
    }

    public static void restoreToken(Context context) {
        OneTimeWorkRequest.Builder request = new OneTimeWorkRequest.Builder(AuthTokenRegisterWorker.class);

        // Use this when you want to add initial delay or schedule initial work to `OneTimeWorkRequest` e.g. setInitialDelay(2, TimeUnit.HOURS)
        OneTimeWorkRequest mywork = request.setInitialDelay(5, TimeUnit.SECONDS).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork("AuthTokenRegisterWorker", ExistingWorkPolicy.KEEP, mywork);
    }

    public static int getConfig(int environment) {
        switch (environment) {
            case AppConstants.ENV_SANDBOX:
                return R.raw.auth_config_sandbox;
            case AppConstants.ENV_DEMO_QA:
                return R.raw.auth_config_demoqa;
            case AppConstants.ENV_IN_BMZ:
                return R.raw.auth_config_inbmz;
            case AppConstants.ENV_NAMIBIA:
                return R.raw.auth_config_namibia;
            default:
                Log.e(TAG, "Environment not configured");
                System.exit(0);
                return -1;
        }
    }

    public static int getEnvironment(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        return sessionManager.getEnvironment();
    }

    public static String[] getScopes(Context context) {
        switch (getEnvironment(context)) {
            case AppConstants.ENV_SANDBOX:
                return new String[]{AppConstants.AUTH_SANDBOX};
            case AppConstants.ENV_DEMO_QA:
                return new String[]{AppConstants.AUTH_DEMO_QA};
            case AppConstants.ENV_IN_BMZ:
                return new String[]{AppConstants.AUTH_IN_BMZ};
            case AppConstants.ENV_NAMIBIA:
                return new String[]{AppConstants.AUTH_NAMIBIA};
            default:
                Log.e(TAG, "Environment not configured");
                System.exit(0);
                return null;
        }
    }

    private void doSignInAction(Runnable onFail) {
        if (!Utils.isNetworkAvailable(context)) {
            Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
            onFail.run();
            return;
        }

        if (singleAccountApp == null) {
            return;
        }

        singleAccountApp.signIn(activity, null, getScopes(activity), getAuthInteractiveCallback(onFail));
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private AuthenticationCallback getAuthInteractiveCallback(Runnable onFail) {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated");

                /* Update account */
                try {
                    //AAD authentication
                    String username = authenticationResult.getAccount().getUsername();
                    if (username.indexOf('@') > 0) {
                        callback.processAuth(username, authenticationResult.getAccessToken(), true);
                        callGraphAPI(authenticationResult);
                    }
                    //B2C authentication
                    else {
                        JWT parsedToken = JWTParser.parse(authenticationResult.getAccessToken());
                        Map<String, Object> claims = parsedToken.getJWTClaimsSet().getClaims();
                        JSONArray emails = (JSONArray) claims.get("emails");
                        if (emails != null && !emails.isEmpty()) {
                            callback.processAuth(emails.get(0).toString(), authenticationResult.getAccessToken(), true);
                            callGraphAPI(authenticationResult);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    onFail.run();
                }
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }

                onFail.run();
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
                onFail.run();
            }
        };
    }

    /**
     * Make an HTTP request to obtain MSGraph data
     */
    private void callGraphAPI(final IAuthenticationResult authenticationResult) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
                context, MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me",
                authenticationResult.getAccessToken(),
                response -> Log.d(TAG, "Response: " + response.toString()),
                error -> Log.d(TAG, "Error: " + error.toString()));
    }
}
