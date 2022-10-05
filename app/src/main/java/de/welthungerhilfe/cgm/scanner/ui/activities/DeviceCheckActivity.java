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

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;


import java.util.ArrayList;
import java.util.Collections;


import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityDeviceCheckBinding;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.DeviceCheckFragment;
import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;

public class DeviceCheckActivity extends BaseActivity {

    public static final String KEY_LAST_DEVICE_CHECK = "KEY_LAST_DEVICE_CHECK";
    public static final String KEY_LAST_DEVICE_CHECK_ISSUES = "KEY_LAST_DEVICE_CHECK_ISSUES";



    private ArrayList<TutorialData> dataList;
    private ArrayList<DeviceCheckFragment> fragments;
    private ArrayList<DeviceCheckFragment.IssueType> issues;
    ActivityDeviceCheckBinding activityDeviceCheckBinding;

    public void startWork(View view) {
        finish();
    }

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        activityDeviceCheckBinding = DataBindingUtil.setContentView(this,R.layout.activity_device_check);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        dataList = getTutorialData();
        fragments = new ArrayList<>();
        issues = new ArrayList<>();

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        for (int i = 0; i < dataList.size(); i++) {
            DeviceCheckFragment fragment = DeviceCheckFragment.newInstance(dataList.get(i));
            fragments.add(fragment);
            adapter.addFragment(fragment, "tutorial" + (i + 1));
        }

        activityDeviceCheckBinding.viewPager.setAdapter(adapter);
        activityDeviceCheckBinding.viewPager.setOffscreenPageLimit(4);
        activityDeviceCheckBinding.viewPager.setSwipeEnabled(false);
    }

    public void addIssue(DeviceCheckFragment.IssueType issue) {
        if (!issues.contains(issue)) {
            issues.add(issue);
            Collections.sort(issues, (a, b) -> a.ordinal() - b.ordinal());
            fragments.get(dataList.size()-1).updateIssues(issues);
            updateIssues();
        }
    }

    public DeviceCheckFragment.IssueType[] getIssues() {
        updateIssues();
        return issues.toArray(new DeviceCheckFragment.IssueType[0]);
    }

    public boolean hasIssues() {
        updateIssues();
        return !issues.isEmpty();
    }

    public void gotoNext() {
        int curpos = activityDeviceCheckBinding.viewPager.getCurrentItem();
        if (curpos == dataList.size()-1) {
            LocalPersistency.setLong(this, KEY_LAST_DEVICE_CHECK, System.currentTimeMillis());
            showCompleteView();
        } else {
            activityDeviceCheckBinding.viewPager.setCurrentItem(curpos + 1);
        }
    }

    private void showCompleteView() {
        activityDeviceCheckBinding.stepView.go(3, false);
        activityDeviceCheckBinding.stepView.done(true);
        activityDeviceCheckBinding.viewPager.setVisibility(View.GONE);
    }

    private ArrayList<TutorialData> getTutorialData() {
        ArrayList<TutorialData> tutorialDataList = new ArrayList<>();
        tutorialDataList.add(new TutorialData(R.drawable.tutorial1, getString(R.string.device_check1), getString(R.string.device_check11), getString(R.string.device_check12),0));
        tutorialDataList.add(new TutorialData(0, getString(R.string.device_check2),"","",1));
        tutorialDataList.add(new TutorialData(R.drawable.device_check3, getString(R.string.device_check3),"","",2));
        tutorialDataList.add(new TutorialData(R.drawable.device_check4, getString(R.string.device_check4),"", getString(R.string.device_check42),3));
        return tutorialDataList;
    }

    private void updateIssues() {
        StringBuilder value = new StringBuilder();
        for (DeviceCheckFragment.IssueType issue : issues) {
            if (value.length() > 0) {
                value.append(";");
            }
            value.append(issue.toString());
        }
        LocalPersistency.setString(this, KEY_LAST_DEVICE_CHECK_ISSUES, value.toString());
    }
}
