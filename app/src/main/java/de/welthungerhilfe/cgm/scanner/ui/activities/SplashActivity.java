/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import de.welthungerhilfe.cgm.scanner.R;

public class SplashActivity extends AppCompatActivity {
    public void onCreate(Bundle saveBundle) {
        AppCenter.start(getApplication(), "{APP_CENTER_KEY}", Analytics.class, Crashes.class);
        boolean isCrashEnabled = Crashes.isEnabled().get();
        if (!isCrashEnabled)
            Crashes.setEnabled(true);

        super.onCreate(saveBundle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
           // startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            startActivity(new Intent(getApplicationContext(), LanguageSelectionActivity.class));

            finish();
        }, 1 * 1000);
    }
}
