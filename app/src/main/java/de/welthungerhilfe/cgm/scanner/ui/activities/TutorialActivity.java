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

import android.view.View;


import androidx.databinding.DataBindingUtil;



import java.util.ArrayList;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityTutorialBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.SelectModeDialog;
import de.welthungerhilfe.cgm.scanner.ui.fragments.TutorialFragment;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class TutorialActivity extends BaseActivity implements SelectModeDialog.SetupmodeListner {

    ArrayList<TutorialData> tutorialDataList;

    ActivityTutorialBinding activityTutorialBinding;

    SessionManager session;

    public void startWork(View view) {
        new SessionManager(this).setTutorial(true);
        if (!again)
            startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean again;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityTutorialBinding = DataBindingUtil.setContentView(this,R.layout.activity_tutorial);

        again = getIntent().getBooleanExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, false);
        tutorialDataList = getTutorialData();

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        for (int i = 0; i < tutorialDataList.size(); i++) {
            adapter.addFragment(TutorialFragment.newInstance(tutorialDataList.get(i)), "tutorial" + (i + 1));
        }

        activityTutorialBinding.viewPager.setAdapter(adapter);
        activityTutorialBinding.viewPager.setOffscreenPageLimit(4);
        activityTutorialBinding.viewPager.setSwipeEnabled(false);
        session = new SessionManager(TutorialActivity.this);
        if(session.getSelectedMode() == AppConstants.NO_MODE_SELECTED){
            if(session.getEnvironmentMode() == AppConstants.CGM_RST_MODE){
                SelectModeDialog selectModeDialog = new SelectModeDialog();
                selectModeDialog.show(getSupportFragmentManager(),"SelectModeDialog");
            }else if(session.getEnvironmentMode() == AppConstants.CGM_MODE){
                session.setSelectedMode(AppConstants.CGM_MODE);

            } else if(session.getEnvironmentMode() == AppConstants.RST_MODE){
                session.setSelectedMode(AppConstants.RST_MODE);

            }
        }
    }

    public void gotoNext() {
        int curpos = activityTutorialBinding.viewPager.getCurrentItem();
        if (curpos + 1 >= activityTutorialBinding.viewPager.getOffscreenPageLimit()) {
            showCompleteView();
        } else {
            activityTutorialBinding.viewPager.setCurrentItem(curpos + 1);
        }
    }

    private void showCompleteView() {
        activityTutorialBinding.lytStart.setVisibility(View.VISIBLE);
        activityTutorialBinding.stepView.go(3,false);
        activityTutorialBinding.stepView.done(true);
        activityTutorialBinding.lytStart.animate()
                .translationY(0)
                .setDuration(500);
    }

    private ArrayList<TutorialData> getTutorialData() {
        ArrayList<TutorialData> tutorialDataList = new ArrayList<>();
        tutorialDataList.add(new TutorialData(R.drawable.tutorial1 ,getString(R.string.tutorial1), getString(R.string.tutorial11), getString(R.string.tutorial12),0));
        tutorialDataList.add(new TutorialData(R.drawable.tutorial2 ,getString(R.string.tutorial2), getString(R.string.tutorial21), getString(R.string.tutorial22),1));
        tutorialDataList.add(new TutorialData(R.drawable.tutorial3 ,getString(R.string.tutorial3), getString(R.string.tutorial31), getString(R.string.tutorial32),2));
        tutorialDataList.add(new TutorialData(R.drawable.tutorial4 ,getString(R.string.tutorial4), getString(R.string.tutorial41), getString(R.string.tutorial42),3));
        return tutorialDataList;
    }

    @Override
    public void changeSetupMode() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}