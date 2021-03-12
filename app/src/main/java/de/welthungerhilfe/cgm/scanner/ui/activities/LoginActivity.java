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

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;
import de.welthungerhilfe.cgm.scanner.utils.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;

public class LoginActivity extends AccountAuthenticatorActivity implements AuthenticationHandler.IAuthenticationCallback {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @OnClick({R.id.btnLoginMicrosoft})
    void doSignIn() {
        if (BuildConfig.DEBUG) {
            if (session.getEnvironment() == AppConstants.ENV_UNKNOWN) {
                Toast.makeText(this, R.string.login_backend_environment, Toast.LENGTH_LONG).show();
                return;
            }
            session.setSigned(true);
            startApp();
        } else {
            if (session.getEnvironment() != AppConstants.ENV_UNKNOWN) {
                Log.d(TAG, "Login into " + SyncingWorkManager.getAPI());
                layout_login.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                new AuthenticationHandler(this, this, () -> runOnUiThread(() -> {
                    layout_login.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }));
            } else {
                Toast.makeText(this, R.string.login_backend_environment, Toast.LENGTH_LONG).show();
            }
        }
    }

    private SessionManager session;

    @BindView(R.id.rb_sandbox)
    RadioButton rb_sandbox;

    @BindView(R.id.layout_login)
    LinearLayout layout_login;

    @BindView(R.id.login_progressbar)
    ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LanguageHelper.onAttach(this);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        session = new SessionManager(this);
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
            startApp();
        }
    }

    private void startApp() {
        layout_login.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            AppController.getInstance().getRootDirectory(getApplicationContext());
            runOnUiThread(() -> {
                if (session.getTutorial())
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                else
                    startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
                finish();
            });
        }).start();
    }

    public void processAuth(String email, String token, boolean feedback) {

        try {
            if (email != null && !email.isEmpty()) {

                //update session
                session.setAuthToken(token);
                session.saveRemoteConfig(new RemoteConfig());
                session.setSigned(true);
                session.setUserEmail(email);
                Log.d(TAG, "Token for " + SyncingWorkManager.getAPI() + " set");

                //update account manager
                final Intent intent = new Intent();
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, email);
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
                intent.putExtra(AccountManager.KEY_AUTHTOKEN, token);
                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);

                //start the app
                SyncAdapter.getInstance(getApplicationContext()).resetRetrofit();
                UploadService.resetRetrofit();
                startApp();

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

    @OnCheckedChanged({R.id.rb_prod_darshna, R.id.rb_prod_aah, R.id.rb_prod_namibia, R.id.rb_demo_qa, R.id.rb_sandbox})
    public void onRadioButtonCheckChanged(CompoundButton button, boolean checked) {
        if (checked) {
            switch (button.getId()) {
                case R.id.rb_prod_aah:
                case R.id.rb_prod_darshna:
                    session.setEnvironment(AppConstants.ENV_IN_BMZ);
                    break;
                case R.id.rb_prod_namibia:
                    session.setEnvironment(AppConstants.ENV_NAMIBIA);
                    break;
                case R.id.rb_demo_qa:
                    session.setEnvironment(AppConstants.ENV_DEMO_QA);
                    break;
                case R.id.rb_sandbox:
                    session.setEnvironment(AppConstants.ENV_SANDBOX);
                    break;
            }
        }
    }
}