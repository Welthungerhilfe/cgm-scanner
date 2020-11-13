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
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.minidev.json.JSONArray;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;

public class LoginActivity extends AccountAuthenticatorActivity {

    @BindView(R.id.loginView)
    WebView webView;

    @OnClick({R.id.btnLoginMicrosoft})
    void doSignIn() {
        doSignInAction();
    }

    private SessionManager session;
    private AccountManager accountManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LanguageHelper.onAttach(this);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        accountManager = AccountManager.get(this);
        session = new SessionManager(this);
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
                if(!ContentResolver.isSyncActive(accounts[0], getString(R.string.sync_authority))) {
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
      /*   if (BuildConfig.DEBUG) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            return;
        }*/

        String tenant = "whhict4x";
        String clientID = "658cc6c6-2c1c-45a7-aac9-aaa295f7f308";
        String responseURL = "http://localhost:5000/getAToken";
        String scope = "cgm-scanner-auth";

        String url = "https://whhict4x.b2clogin.com/whhict4x.onmicrosoft.com/oauth2/v2.0/";
        url += "authorize?p=B2C_1_signupsignin1&client_id=" + clientID;
        url += "&nonce=defaultNonce&redirect_uri=" + responseURL;
        url += "&scope=https://" + tenant + ".onmicrosoft.com/" + clientID + "/" + scope;
        url += "&response_type=token&prompt=login&response_mode=query";
        url += "&authorization_user_agent=WEBVIEW";

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String stringUrl) {
                if (stringUrl.startsWith(responseURL)) {
                    try {
                        String error = Uri.parse(stringUrl).getQueryParameter("error");
                        if ((error == null) || (error.length() == 0)) {
                            String authToken = Uri.parse(stringUrl).getQueryParameter("access_token");
                            if (processToken(authToken)) {
                                webView.loadUrl("about:blank");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, R.string.error_login, Toast.LENGTH_LONG).show();
                    }
                    webView.loadUrl("about:blank");
                    webView.setVisibility(View.GONE);
                }
            }
        });

        if (isConnected()) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        } else {
            Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null != cm) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return (info != null && info.isConnected());
        }
        return false;
    }

    private boolean processToken(String idToken) {
        try {
            JWT parsedToken = JWTParser.parse(idToken);
            Map<String, Object> claims = parsedToken.getJWTClaimsSet().getClaims();

            String azureAccountName = (String) claims.get("extension_azure_account_name");
            String azureAccountKey = (String) claims.get("extension_azure_account_key");
            if (azureAccountName != null && !azureAccountName.equals("") && azureAccountKey != null && !azureAccountKey.equals("")) {
                JSONArray emails = (JSONArray) claims.get("emails");
                if (emails != null && !emails.isEmpty()) {
                    try {
                        CloudStorageAccount.parse(String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", azureAccountName, azureAccountKey));

                        session.setAzureAccountName(azureAccountName);
                        session.setAzureAccountKey(azureAccountKey);

                        session.setAuthToken(idToken);

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


                        session.saveRemoteConfig(new RemoteConfig());
                        session.setSigned(true);
                        session.setUserEmail(firstEmail);

                        if (session.getTutorial())
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        else
                            startActivity(new Intent(getApplicationContext(), TutorialActivity.class));

                        finish();
                        return true;
                    } catch (URISyntaxException | InvalidKeyException e) {
                        e.printStackTrace();

                        Toast.makeText(this, R.string.login_error_nobackend, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.login_error_invalid, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, R.string.login_error_backend, Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e) {
            e.printStackTrace();

            Toast.makeText(LoginActivity.this, R.string.login_error_parse, Toast.LENGTH_LONG).show();
        }

        return false;
    }
}