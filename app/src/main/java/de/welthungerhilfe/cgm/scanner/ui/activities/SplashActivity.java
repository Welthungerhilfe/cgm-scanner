package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.appcenter.crashes.model.ErrorReport;

import java.util.Collections;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class SplashActivity extends AppCompatActivity {
    public void onCreate(Bundle saveBundle) {
        AppCenter.start(getApplication(), "{APP_CENTER_KEY}", Analytics.class, Crashes.class);
        boolean isCrashEnabled = Crashes.isEnabled().get();
        if (!isCrashEnabled)
            Crashes.setEnabled(true);

        AbstractCrashesListener customListener = new AbstractCrashesListener() {
            @Override
            public Iterable<ErrorAttachmentLog> getErrorAttachments(ErrorReport report) {
                ErrorAttachmentLog azureAccount = ErrorAttachmentLog.attachmentWithText("Azure Account Name : " + new SessionManager(SplashActivity.this).getAzureAccountName(), "azure_account.txt");

                return Collections.singletonList(azureAccount);
            }
        };
        Crashes.setListener(customListener);

        super.onCreate(saveBundle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));

            finish();
        }, 1 * 1000);
    }
}
