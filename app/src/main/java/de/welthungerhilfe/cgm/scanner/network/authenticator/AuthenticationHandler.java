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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AuthenticationHandler {

    public interface IAuthenticationCallback {

        void processAuth(String email, String token, boolean feedback);
    }

    private static final String TAG = AuthenticationHandler.class.toString();

    private Activity activity;
    private Context context;
    private SessionManager session;
    private ISingleAccountPublicClientApplication singleAccountApp;
    private IAuthenticationCallback callback;
    private String[] scopes;

    @SuppressLint("StaticFieldLeak")
    private static AuthenticationHandler instance;

    public AuthenticationHandler(Activity activity, IAuthenticationCallback callback, String scope) {

        this.activity = activity;
        this.callback = callback;
        context = activity.getApplicationContext();
        session = new SessionManager(context);
        scopes = new String[1];
        scopes[0] = scope;
        instance = this;

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                R.raw.auth_config_single_account,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        singleAccountApp = application;
                        if (session.isSigned()) {
                            loadAccount();
                        } else {
                            singleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                                @Override
                                public void onSignOut() {
                                    Log.d(TAG, "Signed out");
                                }

                                @Override
                                public void onError(@NonNull MsalException exception) {
                                    Log.e(TAG, exception.toString());
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, exception.toString());
                    }
                });
    }

    public static AuthenticationHandler getInstance() {
        return instance;
    }

    public void doSignInAction() {
        if (!Utils.isNetworkAvailable(context)) {
            Toast.makeText(context, R.string.error_network, Toast.LENGTH_LONG).show();
            return;
        }

        if (singleAccountApp == null) {
            return;
        }

        singleAccountApp.signIn(activity, null, scopes, getAuthInteractiveCallback());
    }

    public boolean isExpiredToken(String message) {
        return message.contains("401");
    }

    public void updateToken(IAuthenticationCallback callback) {
        this.callback = callback;

        String authority = singleAccountApp.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
        singleAccountApp.acquireTokenSilentAsync(scopes, authority, new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                session.setAuthToken(authenticationResult.getAccessToken());
                if (callback != null) {
                    callback.processAuth(session.getUserEmail(), session.getAuthToken(), false);
                }
            }

            @Override
            public void onError(MsalException exception) {
                if (callback != null) {
                    callback.processAuth(null, null, false);
                }
            }
        });
    }

    /**
     * Load the currently signed-in account, if there's any.
     */
    private void loadAccount() {
        if (singleAccountApp == null) {
            return;
        }

        singleAccountApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                // You can use the account data to update your UI or your app database.
                callback.processAuth(activeAccount.getUsername(), session.getAuthToken(), false);
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Log.e(TAG, exception.toString());
            }
        });
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated");

                /* Update account */
                callback.processAuth(authenticationResult.getAccount().getUsername(), authenticationResult.getAccessToken(), true);

                /* call graph */
                callGraphAPI(authenticationResult);
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
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
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