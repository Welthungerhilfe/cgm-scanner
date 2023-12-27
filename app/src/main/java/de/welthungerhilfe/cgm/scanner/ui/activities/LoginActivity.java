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

import android.Manifest;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.test.espresso.remote.EspressoRemoteMessage;

import com.google.firebase.analytics.FirebaseAnalytics;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityLoginBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.repository.LanguageSelectedRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.LogFileUtils;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AuthenticationHandler;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;

public class LoginActivity extends AccountAuthenticatorActivity implements AuthenticationHandler.IAuthenticationCallback, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int STORAGE_PEMISSION = 100;

    ActivityLoginBinding activityLoginBinding;

    FirebaseAnalytics firebaseAnalytics;

    String selectedBackend;
    String selectedCountry = null;
    String selectedOrganization = null;

    LanguageSelectedRepository languageSelectedRepository;
    String[] country;
    String india[] ={"India"};
    String namibia[] ={"Namibia"};
    String nepal[] ={"Nepal"};
    String uganda[] ={"Uganda"};
    String bangladesh[] ={"Bangladesh"};

    String sierra_leone[]={"Sierra Leone"};

    String malawi[] ={"Malawi"};
    String demo[] ={"Demo/Test","Demo/Test - CGM","Demo/Test - RST"};
    String sandbox[] ={"Sandbox"};
    String organization[] = null;



    public void doSignIn() {
        if (!checkStoragePermissions()) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("backend_selected",selectedBackend);
        firebaseAnalytics.logEvent("signin_started",bundle);

        if (BuildConfig.DEBUG) {
            if (session.getEnvironment() == AppConstants.ENV_UNKNOWN) {
                Toast.makeText(this, R.string.login_backend_environment, Toast.LENGTH_LONG).show();
                return;
            }
            session.setSigned(true);
            LogFileUtils.startSession(LoginActivity.this, session);

            startApp();
        } else {
            if (session.getEnvironment() != AppConstants.ENV_UNKNOWN) {
                Log.d(TAG, "Login into " + SyncingWorkManager.getAPI());
                activityLoginBinding.layoutLogin.setVisibility(View.GONE);
                activityLoginBinding.loginProgressbar.setVisibility(View.VISIBLE);
                new AuthenticationHandler(this, this, () -> runOnUiThread(() -> {
                    activityLoginBinding.layoutLogin.setVisibility(View.VISIBLE);
                    activityLoginBinding.loginProgressbar.setVisibility(View.GONE);
                }));
            } else {
                Toast.makeText(this, R.string.login_backend_environment, Toast.LENGTH_LONG).show();
            }
        }
    }

    private SessionManager session;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //BaseActivity.forceSelectedLanguage(this);

        activityLoginBinding = DataBindingUtil.setContentView(this,R.layout.activity_login);
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(this);
        session = new SessionManager(this);
        languageSelectedRepository = LanguageSelectedRepository.getInstance(this);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            if (version.contains("dev")) {
              //  activityLoginBinding.rbSandbox.setVisibility(View.VISIBLE);
               country =new String[]{"Select Country","India","Malawi","Sierra Leone","Namibia","Nepal","Uganda","Bangladesh","Demo/Test","Sandbox"};
            }
            else {
                country =new String[]{"Select Country","India","Malawi","Sierra Leone","Namibia","Nepal","Uganda","Bangladesh","Demo/Test"};

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        checkStoragePermissions();
        activityLoginBinding.rbProdAah.setOnCheckedChangeListener(this);
        activityLoginBinding.rbProdDarshna.setOnCheckedChangeListener(this);
        activityLoginBinding.rbProdNamibia.setOnCheckedChangeListener(this);
        activityLoginBinding.rbDemoQa.setOnCheckedChangeListener(this);
        activityLoginBinding.rbSandbox.setOnCheckedChangeListener(this);
        activityLoginBinding.rbUganda.setOnCheckedChangeListener(this);
        activityLoginBinding.rbBangladesh.setOnCheckedChangeListener(this);

        ArrayAdapter countryAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,country);

        activityLoginBinding.spinnerCountry.setAdapter(countryAdapter);

        activityLoginBinding.spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(country[i].equals("Select Country")) {
                    view.setAlpha(0.4F);
                }
                else {
                    view.setAlpha(1);
                }
                switch (country[i]){
                    case "India":
                        selectedCountry = country[i];
                        organization = india;
                        break;
                    case "Malawi":
                        selectedCountry = country[i];
                        organization = malawi;
                        break;
                    case "Namibia":
                        selectedCountry = country[i];
                        organization = namibia;
                        break;
                    case "Nepal":
                        selectedCountry = country[i];
                        organization = nepal;
                        break;
                    case "Uganda":
                        selectedCountry = country[i];
                        organization = uganda;
                        break;
                    case "Bangladesh":
                        selectedCountry = country[i];
                        organization = bangladesh;
                        break;
                    case "Demo/Test":
                        selectedCountry = country[i];
                        organization = demo;
                        break;
                    case "Sandbox":
                        selectedCountry = country[i];
                        organization = sandbox;
                        break;
                    case "Sierra Leone":
                        selectedCountry = country[i];
                        organization = sierra_leone;
                    default:
                        selectedCountry = null;
                        organization = null;
                        break;
                }

                if(organization == null){
                    organization = new String[]{"Select Organization"};
                }
                ArrayAdapter organazationAdapter = new ArrayAdapter(LoginActivity.this, android.R.layout.simple_spinner_dropdown_item,organization);
                activityLoginBinding.spinnerCluster.setAdapter(organazationAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(organization == null){
            organization = new String[]{"Select Organization"};
        }
        ArrayAdapter organazationAdapter = new ArrayAdapter(LoginActivity.this, android.R.layout.simple_spinner_dropdown_item,organization);
        activityLoginBinding.spinnerCluster.setAdapter(organazationAdapter);
        setupOrganazationListner();


        activityLoginBinding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedCountry==null){
                    Toast.makeText(LoginActivity.this,"Please select country and organization", Toast.LENGTH_LONG).show();
                    return;
                }
                if(selectedOrganization==null){
                    Toast.makeText(LoginActivity.this,"Please select organization", Toast.LENGTH_LONG).show();
                    return;
                }
                doSignIn();
            }
        });
    }

    public void setupOrganazationListner(){
        activityLoginBinding.spinnerCluster.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG,"this is spinner org inside ");

                if(organization==null){
                    return;
                }
                if(view!=null) {
                    if (organization[i].equals("Select Organization")) {
                        view.setAlpha(0.4F);
                    } else {
                        view.setAlpha(1);
                    }
                }
                Log.i(TAG,"this is spinner org "+organization[i]);
                switch (organization[i]){
                    case "India":
                        selectedCountry = country[i];
                        selectedOrganization = "India";
                        session.setEnvironment(AppConstants.ENV_IN_BMZ);
                        session.setEnvironmentMode(AppConstants.CGM_RST_MODE);
                        selectedBackend = "in_bmz";
                        break;

                    case "Malawi":
                        selectedCountry = country[i];
                        selectedOrganization = "Malawi";
                        session.setEnvironment(AppConstants.ENV_MALAWI);
                        session.setEnvironmentMode(AppConstants.CGM_RST_MODE);
                        selectedBackend = "malawi";
                        break;

                    case "Sierra Leone":
                        selectedCountry = country[i];
                        selectedOrganization = "Sierra Leone";
                        session.setEnvironment(AppConstants.ENV_SIERRA);
                        session.setEnvironmentMode(AppConstants.CGM_RST_MODE);
                        selectedBackend = "Sierra Leone";
                        break;
                    case "Namibia":
                        selectedCountry = country[i];
                        selectedOrganization = "Namibia";
                        session.setEnvironment(AppConstants.ENV_NAMIBIA);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);
                        selectedBackend = "namibia";
                        break;
                    case "Nepal":
                        selectedCountry = country[i];
                        selectedOrganization = "Nepal";
                        session.setEnvironment(AppConstants.ENV_NEPAL);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);
                        selectedBackend = "nepal";
                        break;
                    case "Uganda":
                        selectedCountry = country[i];
                        selectedOrganization = "Uganda";
                        session.setEnvironment(AppConstants.ENV_UGANDA);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);
                        selectedBackend = "uganda";
                        break;
                    case "Bangladesh":
                        selectedCountry = country[i];
                        selectedOrganization = "Bangladesh";
                        session.setEnvironment(AppConstants.ENV_BAN);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);
                        selectedBackend = "bangladesh";
                        break;
                    case "Demo/Test - CGM":
                        selectedCountry = country[i];
                        selectedOrganization = "Demo/Test - CGM";
                        session.setEnvironment(AppConstants.ENV_DEMO_QA);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);
                        selectedBackend = "demo_qa";
                        break;

                    case "Demo/Test - RST":
                        selectedCountry = country[i];
                        selectedOrganization = "Demo/Test - RST";
                        session.setEnvironment(AppConstants.ENV_DEMO_QA);
                        session.setEnvironmentMode(AppConstants.RST_MODE);
                        selectedBackend = "demo_qa";
                        break;

                    case "Demo/Test":
                        selectedCountry = country[i];
                        selectedOrganization = "Demo/Test";
                        session.setEnvironment(AppConstants.ENV_DEMO_QA);
                        session.setEnvironmentMode(AppConstants.CGM_RST_MODE);
                        selectedBackend = "demo_qa";
                        break;

                    case "Sandbox":
                        selectedCountry = country[i];
                        selectedOrganization = "Sandbox";
                        session.setEnvironment(AppConstants.ENV_SANDBOX);
                        session.setEnvironmentMode(AppConstants.CGM_MODE);

                        selectedBackend = "sandbox";
                        break;
                    default:
                        selectedCountry = null;

                        break;


                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
            startApp();
        }
    }

    private void startApp() {
        activityLoginBinding.layoutLogin.setVisibility(View.GONE);
        activityLoginBinding.loginProgressbar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            AppController.getInstance().getRootDirectory(getApplicationContext());
            runOnUiThread(() -> {
                if (languageSelectedRepository.getLanguageSelectedId(session.getUserEmail())!= null)
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                else
                    startActivity(new Intent(getApplicationContext(), LanguageSelectionActivity.class));
                Bundle bundle = new Bundle();
                bundle.putString("backend_selected",selectedBackend);
                firebaseAnalytics.logEvent("signin_finished",bundle);
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
                LogFileUtils.startSession(LoginActivity.this, session);
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

    @Override
    public void onCheckedChanged(CompoundButton button, boolean checked) {
        if (checked) {
            switch (button.getId()) {
                case R.id.rb_prod_aah:
                    session.setEnvironment(AppConstants.ENV_IN_BMZ);
                    selectedBackend = "in_bmz";
                    break;
                case R.id.rb_prod_darshna:
                    session.setEnvironment(AppConstants.ENV_NEPAL);
                    selectedBackend = "nepal";
                    break;
                case R.id.rb_prod_namibia:
                    session.setEnvironment(AppConstants.ENV_NAMIBIA);
                    selectedBackend = "namibia";
                    break;
                case R.id.rb_demo_qa:
                    session.setEnvironment(AppConstants.ENV_DEMO_QA);
                    selectedBackend = "demo_qa";
                    break;
                case R.id.rb_sandbox:
                    session.setEnvironment(AppConstants.ENV_SANDBOX);
                    selectedBackend = "sandbox";
                    break;
                case R.id.rb_uganda:
                    session.setEnvironment(AppConstants.ENV_UGANDA);
                    selectedBackend = "uganda";
                    break;
                case R.id.rb_bangladesh:
                    session.setEnvironment(AppConstants.ENV_BAN);
                    selectedBackend = "bangladesh";
                    break;
            }
        }
    }

    public boolean checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
            return false;
        }
        return true;
    }

    private void requestStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PEMISSION);
    }


}