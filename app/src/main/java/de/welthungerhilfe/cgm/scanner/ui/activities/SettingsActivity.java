package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemoteConfig;
import de.welthungerhilfe.cgm.scanner.datasource.repository.ArtifactResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureResultRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SettingsActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txtSettingVersion)
    TextView txtSettingVersion;
    @BindView(R.id.txtSettingUuid)
    TextView txtSettingUuid;
    @BindView(R.id.txtSettingAccount)
    TextView txtSettingAccount;
    @BindView(R.id.txtSettingAzureAccount)
    TextView txtSettingAzureAccount;

    @BindView(R.id.radioEnglish)
    AppCompatRadioButton radioEnglish;
    @BindView(R.id.radioGerman)
    AppCompatRadioButton radioGerman;
    @BindView(R.id.radioHindi)
    AppCompatRadioButton radioHindi;

    @BindView(R.id.txtSettingBackupDate)
    TextView txtSettingBackupDate;

    @BindView(R.id.testQAlayout)
    LinearLayout layoutTestQA;

    @OnClick(R.id.submenu_performance_measurement)
    void openPerformanceMeasurement(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsPerformanceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @OnClick(R.id.submenu_remote_config)
    void openRemoteConfig(View view) {
        Intent intent = new Intent(SettingsActivity.this, SettingsRemoteConfigActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @OnClick(R.id.lytLangEnglish)
    void onEnglish(LinearLayout lytLangEnglish) {
        changeLanguage("en");
    }

    @OnClick(R.id.lytLangGerman)
    void onGerman(LinearLayout lytLangGerman) {
        changeLanguage("de");
    }

    @OnClick(R.id.lytLangHindi)
    void onHindi(LinearLayout lytLangHindi) {
        changeLanguage("hi");
    }

    private SessionManager session;
    private RemoteConfig config;

    private AlertDialog progressDialog;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);

        session = new SessionManager(this);
        config = session.getRemoteConfig();

        setupActionBar();

        initUI();

        progressDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(R.layout.dialog_loading)
                .create();
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_settings);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void initUI() {
        String account = session.getAzureAccountName();
        boolean devBackend = (account != null) && account.endsWith("dev");
        boolean devVersion = BuildConfig.VERSION_NAME.endsWith("dev");
        boolean showQA = BuildConfig.DEBUG || devBackend || devVersion;
        layoutTestQA.setVisibility(showQA ? View.VISIBLE : View.GONE);

        txtSettingUuid.setText(Utils.getAndroidID(getContentResolver()));

        try {
            txtSettingVersion.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            txtSettingVersion.setText("1.0");
        }

        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccounts();

        if (accounts.length > 0) {
            txtSettingAccount.setText(accounts[0].name);
        }

        txtSettingAzureAccount.setText(session.getAzureAccountName());

        String code = session.getLanguage();
        switch (code) {
            case AppConstants.LANG_ENGLISH:
                radioEnglish.setChecked(true);
                break;
            case AppConstants.LANG_GERMAN:
                radioGerman.setChecked(true);
                break;
            case AppConstants.LANG_HINDI:
                radioHindi.setChecked(true);
                break;
        }

        if (session.getBackupTimestamp() == 0) txtSettingBackupDate.setText(R.string.no_backups);
        else txtSettingBackupDate.setText(DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, session.getBackupTimestamp()));

        findViewById(R.id.btnBackupNow).setOnClickListener(view -> {
            progressDialog.show();

            ArtifactResultRepository arRepo = ArtifactResultRepository.getInstance(this);
            DeviceRepository dRepo = DeviceRepository.getInstance(this);
            FileLogRepository flRepo = FileLogRepository.getInstance(this);
            MeasureRepository mRepo = MeasureRepository.getInstance(this);
            MeasureResultRepository mrRepo = MeasureResultRepository.getInstance(this);
            PersonRepository pRepo = PersonRepository.getInstance(this);

            long timestamp = System.currentTimeMillis();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        File dbFile = getDatabasePath(CgmDatabase.DATABASE);
                        FileInputStream fis = new FileInputStream(dbFile);

                        String outFileName = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "db-%d.db", timestamp);
                        OutputStream output = new FileOutputStream(outFileName);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            output.write(buffer, 0, length);
                        }

                        output.flush();
                        output.close();
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<ArtifactResult> arAll = arRepo.getAll();
                    String arCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_ARTIFACT_RESULT, timestamp);
                    try {
                        FileWriter writer = new FileWriter(arCsv);
                        writer.append(new ArtifactResult().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < arAll.size(); i++) {
                            writer.append(arAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<Device> dAll = dRepo.getAll();
                    String dCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_DEVICE, timestamp);
                    try {
                        FileWriter writer = new FileWriter(dCsv);
                        writer.append(new Device().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < dAll.size(); i++) {
                            writer.append(dAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    List<FileLog> flAll = flRepo.getAll();
                    String fCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_FILE_LOG, timestamp);
                    try {
                        FileWriter writer = new FileWriter(fCsv);
                        writer.append(new FileLog().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < flAll.size(); i++) {
                            writer.append(flAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    List<Measure> mAll = mRepo.getAll();
                    String mCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_MEASURE, timestamp);
                    try {
                        FileWriter writer = new FileWriter(mCsv);
                        writer.append(new Measure().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < mAll.size(); i++) {
                            writer.append(mAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    List<MeasureResult> mrAll = mrRepo.getAll();
                    String mrCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_MEASURE_RESULT, timestamp);
                    try {
                        FileWriter writer = new FileWriter(mrCsv);
                        writer.append(new MeasureResult().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < mrAll.size(); i++) {
                            writer.append(mrAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    List<Person> pAll = pRepo.getAll();
                    String pCsv = AppController.getInstance().getRootDirectory() + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_PERSON, timestamp);
                    try {
                        FileWriter writer = new FileWriter(pCsv);
                        writer.append(new Person().getCsvHeaderString());
                        writer.append('\n');

                        for (int i = 0; i < pAll.size(); i++) {
                            writer.append(pAll.get(i).getCsvFormattedString());
                            writer.append('\n');
                        }

                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }

                public void onPostExecute(Void result) {
                    session.setBackupTimestamp(timestamp);
                    txtSettingBackupDate.setText(DataFormat.timestamp(getBaseContext(), DataFormat.TimestampFormat.DATE, timestamp));
                    progressDialog.dismiss();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
    }

    private void changeLanguage(String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        session.setLanguage(code);

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
