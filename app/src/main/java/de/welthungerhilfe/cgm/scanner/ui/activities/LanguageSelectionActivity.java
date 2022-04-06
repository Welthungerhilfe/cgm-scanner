package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityLanguageSelectionBinding;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;


public class LanguageSelectionActivity extends BaseActivity {

    ActivityLanguageSelectionBinding activityLanguageSelectionBinding;
    private SessionManager sessionManager;
    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityLanguageSelectionBinding = DataBindingUtil.setContentView(this, R.layout.activity_language_selection);
        activityLanguageSelectionBinding.btDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLanguage(AppConstants.LANG_NEPALI);
            }
        });
    }

    private void changeLanguage(String code) {
        sessionManager = new SessionManager(this);
        sessionManager.setLanguage(code);

        Intent i = new Intent(this,TutorialActivity.class);
        startActivity(i);
        forceSelectedLanguage(this);
        finish();
    }


}
