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

package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
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

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.authenticator.MSGraphRequestWrapper;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = LoginActivity.class.toString();

    @OnClick({R.id.btnLoginMicrosoft})
    void doSignIn() {
        doSignInAction();
    }

    private SessionManager session;
    private AccountManager accountManager;
    private ISingleAccountPublicClientApplication singleAccountApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LanguageHelper.onAttach(this);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        accountManager = AccountManager.get(this);
        session = new SessionManager(this);

        // Creates a PublicClientApplication object with res/raw/auth_config_single_account.json
        PublicClientApplication.createSingleAccountPublicClientApplication(getBaseContext(),
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

    @Override
    public void onStart() {
        super.onStart();

        RemoteConfig config = session.getRemoteConfig();
        if (config == null) {
            session.saveRemoteConfig(new RemoteConfig());
        }

        if (session.isSigned()) {
            Account[] accounts = accountManager.getAccountsByType(AppConstants.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                if (!ContentResolver.isSyncActive(accounts[0], getString(R.string.sync_authority))) {
                    session.setSyncTimestamp(0);
                    SyncAdapter.startPeriodicSync(accounts[0], getApplicationContext());
                }
            } else {
                session.setSyncTimestamp(0);

                try {
                    JWT parsedToken = JWTParser.parse(session.getAuthToken());
                    Map<String, Object> claims = parsedToken.getJWTClaimsSet().getClaims();

                    JSONArray emails = (JSONArray) claims.get("emails");
                    if (emails != null && !emails.isEmpty()) {
                        String token = (String) claims.get("at_hash");
                        String firstEmail = emails.get(0).toString();

                        final Account account = new Account(firstEmail, AppConstants.ACCOUNT_TYPE);

                        accountManager.addAccountExplicitly(account, token, null);

                        SyncAdapter.startPeriodicSync(account, getApplicationContext());

                        final Intent intent = new Intent();
                        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, firstEmail);
                        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
                        intent.putExtra(AccountManager.KEY_AUTHTOKEN, token);

                        setAccountAuthenticatorResult(intent.getExtras());
                        setResult(RESULT_OK, intent);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if (session.getTutorial())
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            else
                startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
            finish();
        }
    }

    private void doSignInAction() {
        if (BuildConfig.DEBUG) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            return;
        }

        if (!Utils.isNetworkAvailable(this)) {
            Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
            return;
        }

        if (singleAccountApp == null) {
            return;
        }

        String[] scopes = { "{OAUTH_SCOPE}" };
        singleAccountApp.signIn(this, null, scopes, getAuthInteractiveCallback());
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
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getIdToken());

                /* Update account */
                processAuth(authenticationResult.getAccount(), true);

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
                getBaseContext(),
                MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me",
                authenticationResult.getAccessToken(),
                response -> Log.d(TAG, "Response: " + response.toString()),
                error -> Log.d(TAG, "Error: " + error.toString()));
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
                processAuth(activeAccount, false);
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) { }

            @Override
            public void onError(@NonNull MsalException exception) {
                Log.e(TAG, exception.toString());
            }
        });
    }


    private void processAuth(IAccount account, boolean feedback) {

        try {
            String token = account.getIdToken();
            String email = account.getUsername();
            if (email != null && !email.isEmpty()) {
                session.setAuthToken(token);

                final Account accountData = new Account(email, AppConstants.ACCOUNT_TYPE);
                accountManager.addAccountExplicitly(accountData, token, null);

                SyncAdapter.startPeriodicSync(accountData, getApplicationContext());

                final Intent intent = new Intent();
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
                intent.putExtra(AccountManager.KEY_AUTHTOKEN, token);

                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);


                session.saveRemoteConfig(new RemoteConfig());
                session.setSigned(true);
                session.setUserEmail(email);

                if (session.getTutorial())
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                else
                    startActivity(new Intent(getApplicationContext(), TutorialActivity.class));

                finish();
            } else if (feedback) {
                Toast.makeText(LoginActivity.this, R.string.login_error_invalid, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (feedback) {
                Toast.makeText(LoginActivity.this, R.string.login_error_parse, Toast.LENGTH_LONG).show();
            }
        }
    }
}