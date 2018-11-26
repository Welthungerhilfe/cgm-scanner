package de.welthungerhilfe.cgm.scanner.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private SessionManager session;

    public void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_splash);

        session = new SessionManager(this);

        ImageView logo = findViewById(R.id.imgLogo);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));

                finish();
            }
        }, 1 * 1000);
    }
}
