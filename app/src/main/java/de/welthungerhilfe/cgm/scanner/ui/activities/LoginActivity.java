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
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.Date;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @BindView(R.id.editUser)
    EditText editUser;
    @BindView(R.id.editPassword)
    EditText editPassword;

    @BindString(R.string.validate_user)
    String strUserValidation;
    @BindString(R.string.validate_password)
    String strPasswordValidation;

    @OnClick(R.id.btnOK)
    void doSignIn(TextView btnOK) {
        doSignInAction();
    }

    /*
    @OnClick(R.id.txtCancel)
    void doSignOut(TextView txtCancel) {
        AppController.getInstance().firebaseAuth.signOut();
        session.setSigned(false);

        //createUser("zhangnemo34@hotmail.com", "Crystal");
    }
    */

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

        editPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doSignInAction();

                    return true;
                }
                return false;
            }
        });

        session = new SessionManager(this);
    }

    public void onStart() {
        super.onStart();

        if (AppController.getInstance().firebaseUser != null && session.isSigned()) {
            Account[] accounts = accountManager.getAccountsByType(AppConstants.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                if(!ContentResolver.isSyncActive(accounts[0], getString(R.string.sync_authority))) {
                    session.setSyncTimestamp(0);
                    SyncAdapter.startPeriodicSync(accounts[0], getApplicationContext());
                }
            } else {
                session.setSyncTimestamp(0);

                final Account account = new Account(AppController.getInstance().firebaseUser.getEmail(), AppConstants.ACCOUNT_TYPE);

                accountManager.addAccountExplicitly(account, "welthungerhilfe", null);

                SyncAdapter.startPeriodicSync(account, getApplicationContext());

                final Intent intent = new Intent();
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, AppController.getInstance().firebaseUser.getEmail());
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
        if (!Utils.isNetworkConnectionAvailable(LoginActivity.this)) {
            Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
        } else if (validate()) {
            String email = editUser.getText().toString();
            String password = editPassword.getText().toString();
            Crashlytics.setUserIdentifier(email);

            AppController.getInstance().firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
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

                                Crashlytics.setUserIdentifier(email);
                                Crashlytics.log(0, "user login: ", String.format("user logged in with email %s at %s", email, Utils.beautifyDateTime(new Date())));

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
                        }
                    });
        }
    }
}