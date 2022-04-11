package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

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
    public String code;
    boolean startFromHomeScreen;
    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityLanguageSelectionBinding = DataBindingUtil.setContentView(this, R.layout.activity_language_selection);
        languageSelectedRepository = LanguageSelectedRepository.getInstance(this);
        startFromHomeScreen = getIntent().getBooleanExtra("startFromHomeScreen",false);
        sessionManager = new SessionManager(this);
        initUI();

        activityLanguageSelectionBinding.rbEnglish.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    selectedLanguage(AppConstants.LANG_ENGLISH);
                }
            }
        });
        activityLanguageSelectionBinding.rbHindi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if(isChecked){
                        selectedLanguage(AppConstants.LANG_HINDI);
                    }
            }
        });
        activityLanguageSelectionBinding.rbDeutsch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                        selectedLanguage(AppConstants.LANG_GERMAN);
                    }

            }
        });
        activityLanguageSelectionBinding.rbNepali.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                        selectedLanguage(AppConstants.LANG_NEPALI);
                    }
            }
        });
        activityLanguageSelectionBinding.rbBangla.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(isChecked){
                        selectedLanguage(AppConstants.LANG_BANGLA);
                    }
                }
            }
        });
        activityLanguageSelectionBinding.btDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               changeLanguage(code);
            }
        });
    }

    public void selectedLanguage(String selectedCode){
        code = selectedCode;
    }

    private void changeLanguage(String code) {
        LanguageSelected languageSelected = new LanguageSelected();
        languageSelected.setId(sessionManager.getUserEmail());
        languageSelected.setSelectedLanguage(code);
        languageSelectedRepository.insertLanguageSelected(languageSelected);
        forceSelectedLanguage(this,code);

        if (!startFromHomeScreen){
            Intent i = new Intent(this, TutorialActivity.class);
            startActivity(i);
            finish();
        }
        else {
            Intent i = new Intent(this, MainActivity.class);

            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
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
