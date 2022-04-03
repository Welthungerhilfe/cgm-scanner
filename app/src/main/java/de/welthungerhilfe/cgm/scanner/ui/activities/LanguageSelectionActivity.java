package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityLanguageSelectionBinding;


public class LanguageSelectionActivity extends BaseActivity {

    ActivityLanguageSelectionBinding activityLanguageSelectionBinding;
    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityLanguageSelectionBinding = DataBindingUtil.setContentView(this, R.layout.activity_language_selection);
    }
}
