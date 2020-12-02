package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.LinearLayout;

import com.shuhart.stepview.StepView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.TutorialData;
import de.welthungerhilfe.cgm.scanner.ui.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.ui.fragments.TutorialFragment;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.ui.views.PagerView;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class TutorialActivity extends AppCompatActivity {
    @BindView(R.id.viewPager)
    PagerView viewPager;
    @BindView(R.id.lytStart)
    LinearLayout lytStart;
    @BindView(R.id.stepView)
    StepView stepView;

    ArrayList<TutorialData> tutorialDataList;

    @OnClick(R.id.btnStart)
    void startWork() {
        session.setTutorial(true);
        if (!again)
            startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private SessionManager session;
    private boolean again;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

        again = getIntent().getBooleanExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, false);

        session = new SessionManager(this);
        tutorialDataList = Utils.getTutorialData(this);

        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());

        for (int i = 0; i < tutorialDataList.size(); i++) {
            adapter.addFragment(TutorialFragment.newInstance(tutorialDataList.get(i)), "tutorial" + (i + 1));
        }

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setSwipeEnabled(false);
    }

    public void gotoNext() {
        int curpos = viewPager.getCurrentItem();
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
}
