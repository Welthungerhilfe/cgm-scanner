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
import android.widget.LinearLayout;

import com.shuhart.stepview.StepView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.camera.TangoUtils;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.DeviceCheckFragment;
import de.welthungerhilfe.cgm.scanner.ui.views.PagerView;
import de.welthungerhilfe.cgm.scanner.utils.LocalPersistency;

public class DeviceCheckActivity extends BaseActivity {

    public static final String KEY_LAST_DEVICE_CHECK = "KEY_LAST_DEVICE_CHECK";

    @BindView(R.id.viewPager)
    PagerView viewPager;
    @BindView(R.id.lytStart)
    LinearLayout lytStart;
    @BindView(R.id.stepView)
    StepView stepView;

    private ArrayList<TutorialData> dataList;
    private ArrayList<DeviceCheckFragment> fragments;
    private ArrayList<DeviceCheckFragment.IssueType> issues;

    @OnClick(R.id.btnStart)
    void startWork() {
        finish();
    }

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_device_check);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ButterKnife.bind(this);

        dataList = getTutorialData();
        fragments = new ArrayList<>();
        issues = new ArrayList<>();

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        for (int i = 0; i < dataList.size(); i++) {
            DeviceCheckFragment fragment = DeviceCheckFragment.newInstance(dataList.get(i));
            fragments.add(fragment);
            adapter.addFragment(fragment, "tutorial" + (i + 1));
        }

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(TangoUtils.isTangoSupported() ? 2 : 4);
        viewPager.setSwipeEnabled(false);
    }

    public void addIssue(DeviceCheckFragment.IssueType issue) {
        if (!issues.contains(issue)) {
            issues.add(issue);
            issues.sort((a, b) -> a.ordinal() - b.ordinal());
            fragments.get(3).updateIssues(issues);
        }
    }

    public boolean hasIssues() {
        return issues.size() > 0;
    }

    public void gotoNext() {
        int curpos = viewPager.getCurrentItem();
        if (curpos == 3) {
            LocalPersistency.setLong(this, KEY_LAST_DEVICE_CHECK, System.currentTimeMillis());
        }
        if (curpos + 1 >= viewPager.getOffscreenPageLimit()) {
            showCompleteView();
        } else {
            viewPager.setCurrentItem(curpos + 1);
        }
    }

    private void showCompleteView() {
        lytStart.setVisibility(View.VISIBLE);
        stepView.go(3,false);
        stepView.done(true);
        lytStart.animate()
                .translationY(0)
                .setDuration(500);
    }

    private ArrayList<TutorialData> getTutorialData() {
        ArrayList<TutorialData> tutorialDataList = new ArrayList<>();
        if (!TangoUtils.isTangoSupported()) {
            tutorialDataList.add(new TutorialData(R.drawable.tutorial1, getString(R.string.device_check1), getString(R.string.device_check11), getString(R.string.device_check12),0));
            tutorialDataList.add(new TutorialData(0, getString(R.string.device_check2),"","",1));
        }
        tutorialDataList.add(new TutorialData(R.drawable.device_check3, getString(R.string.device_check3),"","",2));
        tutorialDataList.add(new TutorialData(R.drawable.device_check4, getString(R.string.device_check4),"", getString(R.string.device_check42),3));
        return tutorialDataList;
    }
}
