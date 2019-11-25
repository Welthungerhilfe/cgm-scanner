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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.appcenter.auth.Auth;
import com.microsoft.appcenter.auth.SignInResult;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.minidev.json.JSONArray;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class LoginActivity extends AccountAuthenticatorActivity {

    @BindView(R.id.editUser)
    EditText editUser;
    @BindView(R.id.editPassword)
    EditText editPassword;

    @BindString(R.string.validate_user)
    String strUserValidation;
    @BindString(R.string.validate_password)
    String strPasswordValidation;

    @OnClick({R.id.btnOK, R.id.btnLoginMicrosoft})
    void doSignIn() {
        doSignInAction();
    }

    @OnClick(R.id.txtForgot)
    void doForgot(TextView txtForgot) {

    }

    private SessionManager session;
    private AccountManager accountManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        accountManager = AccountManager.get(this);

        editPassword.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doSignInAction();

                return true;
            }
            return false;
        });

        session = new SessionManager(this);
    }

    public void onStart() {
        super.onStart();

        RemoteConfig config = session.getRemoteConfig();
        if (config == null) {
            session.saveRemoteConfig(new RemoteConfig());
        }

        // Todo : check if AppCenter signed in
        if (true && session.isSigned()) {
            Account[] accounts = accountManager.getAccountsByType(AppConstants.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                if(!ContentResolver.isSyncActive(accounts[0], getString(R.string.sync_authority))) {
                    session.setSyncTimestamp(0);
                    SyncAdapter.startPeriodicSync(accounts[0], getApplicationContext());
                }
            } else {
                session.setSyncTimestamp(0);

                // Todo : add email from AppCenter Auth
                final Account account = new Account("email", AppConstants.ACCOUNT_TYPE);

                accountManager.addAccountExplicitly(account, "welthungerhilfe", null);

                SyncAdapter.startPeriodicSync(account, getApplicationContext());

                final Intent intent = new Intent();
                // Todo : add email from AppCenter Auth
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, "email");
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
                intent.putExtra(AccountManager.KEY_AUTHTOKEN, "welthungerhilfe");

                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);
            }

            if (session.getTutorial())
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            else
                startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
            finish();
        }
    }

    private boolean validate() {
        boolean valid = true;

        String user = editUser.getText().toString();
        String password = editPassword.getText().toString();

        if (user.isEmpty()) {
            editUser.setError(strUserValidation);
            valid = false;
        } else {
            editUser.setError(null);
        }

        if (password.isEmpty()) {
            editPassword.setError(strPasswordValidation);
            valid = false;
        } else {
            editPassword.setError(null);
        }

        return valid;
    }

    private void doSignInAction() {
        Auth.signIn().thenAccept(signInResult -> {

            if (signInResult.getException() == null) {

                // Sign-in succeeded if exception is null.
                // SignInResult is never null, getUserInformation() returns not null when there is no exception.
                // Both getIdToken() / getAccessToken() return non null values.
                String idToken = signInResult.getUserInformation().getIdToken();

                try {
                    JWT parsedToken = JWTParser.parse(idToken);
                    Map<String, Object> claims = parsedToken.getJWTClaimsSet().getClaims();
                    String displayName = (String) claims.get("name");

                    JSONArray emails = (JSONArray) claims.get("emails");
                    if (emails != null && !emails.isEmpty()) {
                        String firstEmail = emails.get(0).toString();

                        Log.e("email", firstEmail);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // Do work with either token.
            } else {

                // Do something with sign in failure.
                Exception signInFailureException = signInResult.getException();
                signInFailureException.printStackTrace();
            }
        });

        /*
        if (!Utils.isNetworkConnectionAvailable(LoginActivity.this)) {
            Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
        } else if (validate()) {
            String email = editUser.getText().toString();
            String password = editPassword.getText().toString();
            // todo: Crashlytics.setUserIdentifier(email);

            AppController.getInstance().firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            final Account account = new Account(email, AppConstants.ACCOUNT_TYPE);

                            accountManager.addAccountExplicitly(account, password, null);

                            SyncAdapter.startPeriodicSync(account, getApplicationContext());

                            final Intent intent = new Intent();
                            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
                            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
                            intent.putExtra(AccountManager.KEY_AUTHTOKEN, password);

                            setAccountAuthenticatorResult(intent.getExtras());
                            setResult(RESULT_OK, intent);

                            // todo: Crashlytics.setUserIdentifier(email);
                            // todo: Crashlytics.log(0, "user login: ", String.format("user logged in with email %s at %s", email, Utils.beautifyDateTime(new Date())));

                            session.saveRemoteConfig(new RemoteConfig());

                            session.setSigned(true);
                            AppController.getInstance().prepareFirebaseUser();
                            if (session.getTutorial())
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            else
                                startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.error_login, Toast.LENGTH_LONG).show();
                        }
                    });
        }
        */
    }
}