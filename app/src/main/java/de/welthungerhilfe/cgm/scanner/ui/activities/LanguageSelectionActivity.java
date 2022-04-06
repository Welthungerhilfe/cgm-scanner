package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityLanguageSelectionBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.LanguageSelected;
import de.welthungerhilfe.cgm.scanner.datasource.repository.LanguageSelectedRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;


public class LanguageSelectionActivity extends BaseActivity {

    ActivityLanguageSelectionBinding activityLanguageSelectionBinding;
    private SessionManager sessionManager;
    LanguageSelectedRepository languageSelectedRepository;
    String code;
    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityLanguageSelectionBinding = DataBindingUtil.setContentView(this, R.layout.activity_language_selection);
        languageSelectedRepository = LanguageSelectedRepository.getInstance(this);
        initUI();
        activityLanguageSelectionBinding.rbEnglish.setOnCheckedChangeListener((compoundButton, b) -> code=AppConstants.LANG_ENGLISH);
        activityLanguageSelectionBinding.rbBangla.setOnCheckedChangeListener((compoundButton, b) -> code=AppConstants.LANG_BANGLA);
        activityLanguageSelectionBinding.rbNepali.setOnCheckedChangeListener((compoundButton, b) -> code=AppConstants.LANG_NEPALI);
        activityLanguageSelectionBinding.rbHindi.setOnCheckedChangeListener((compoundButton, b) -> code=AppConstants.LANG_HINDI);
        activityLanguageSelectionBinding.rbDeutsch.setOnCheckedChangeListener((compoundButton, b) -> code=AppConstants.LANG_GERMAN);

        activityLanguageSelectionBinding.btDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               changeLanguage(code);
            }
        });
    }

    private void changeLanguage(String code) {
        LanguageSelected languageSelected = new LanguageSelected();
        languageSelected.setId(sessionManager.getUserEmail());
        languageSelected.setSelectedLanguage(code);
        languageSelectedRepository.insertLanguageSelected(languageSelected);

        Intent i = new Intent(this,TutorialActivity.class);
        startActivity(i);
        forceSelectedLanguage(this,code);
        finish();
    }

    public void initUI(){
        code = languageSelectedRepository.getLanguageSelectedId(sessionManager.getUserEmail());
        if(code==null){
            code = AppConstants.LANG_ENGLISH;
        }
        switch (code) {
            case AppConstants.LANG_ENGLISH:
                activityLanguageSelectionBinding.rbEnglish.setChecked(true);
                break;
            case AppConstants.LANG_GERMAN:
                activityLanguageSelectionBinding.rbDeutsch.setChecked(true);
                break;
            case AppConstants.LANG_HINDI:
                activityLanguageSelectionBinding.rbHindi.setChecked(true);
                break;
            case AppConstants.LANG_NEPALI:
                activityLanguageSelectionBinding.rbNepali.setChecked(true);
                break;
            case AppConstants.LANG_BANGLA:
                activityLanguageSelectionBinding.rbBangla.setChecked(true);
                break;

        }
    }
}
