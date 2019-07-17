package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import de.welthungerhilfe.cgm.scanner.R;

public class SplashActivity extends AppCompatActivity {
    public void onCreate(Bundle saveBundle) {
        AppCenter.start(getApplication(), "{APP_CENTER_KEY}", Analytics.class, Crashes.class);
        boolean isEnabled = Crashes.isEnabled().get();
        if (!isEnabled)
            Crashes.setEnabled(true);



        super.onCreate(saveBundle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));

            finish();
        }, 1 * 1000);
    }
}
