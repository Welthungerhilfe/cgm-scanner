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
import android.content.Intent;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncAdapter;

public class LoginActivity extends AccountAuthenticatorActivity implements AuthenticationHandler.IAuthenticationCallback {

    private AuthenticationHandler authentication;

    @OnClick({R.id.btnLoginMicrosoft})
    void doSignIn() {
        if (BuildConfig.DEBUG) {
            final Account accountData = new Account("test@test.com", AppConstants.ACCOUNT_TYPE);
            accountManager.addAccountExplicitly(accountData, "kjjhhj", null);

            SyncAdapter.startPeriodicSync(accountData, getApplicationContext());
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else {
            if (environment != null) {
                authentication = new AuthenticationHandler(this, this, environment, () -> authentication.doSignInAction());
            } else {
                Toast.makeText(this, R.string.login_backend_environment, Toast.LENGTH_LONG).show();
            }
       }
    }

    private AccountManager accountManager;
    private SessionManager session;
    private AuthenticationHandler.Environment environment;

    @BindView(R.id.rb_sandbox)
    RadioButton rb_sandbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LanguageHelper.onAttach(this);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        session = new SessionManager(this);
        accountManager = AccountManager.get(this);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            if (version.contains("dev")) {
                rb_sandbox.setVisibility(View.VISIBLE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        RemoteConfig config = session.getRemoteConfig();
        if (config == null) {
            session.saveRemoteConfig(new RemoteConfig());
        }

        if (session.isSigned()) {
            if (session.getTutorial())
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            else
                startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
            finish();
        }

    }

    public void processAuth(String email, String token, boolean feedback) {

        try {
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

    @OnCheckedChanged({R.id.rb_prod_darshna, R.id.rb_prod_aah, R.id.rb_demo_qa, R.id.rb_sandbox})
    public void onRadioButtonCheckChanged(CompoundButton button, boolean checked) {
        if(checked) {
            switch (button.getId()) {
                case R.id.rb_prod_aah:
                case R.id.rb_prod_darshna:
                    environment = AuthenticationHandler.Environment.PROUDCTION;
                    break;
                case R.id.rb_demo_qa:
                    environment = AuthenticationHandler.Environment.QA;
                    break;
                case R.id.rb_sandbox:
                    environment = AuthenticationHandler.Environment.SANDBOX;
                    break;
            }
        }
    }
}