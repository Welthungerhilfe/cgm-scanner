package de.welthungerhilfe.cgm.scanner.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial1Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial2Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial3Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial4Fragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class TutorialActivity extends AppCompatActivity {
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.lytStart)
    LinearLayout lytStart;

    @OnClick(R.id.btnStart)
    void startWork() {
        session.setTutorial(true);
        if (!again)
            startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private FragmentAdapter adapter;
    private SessionManager session;
    private boolean again;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

        again = getIntent().getBooleanExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, false);

        session = new SessionManager(this);

        Tutorial1Fragment tur1 = new Tutorial1Fragment();
        Tutorial2Fragment tur2 = new Tutorial2Fragment();
        Tutorial3Fragment tur3 = new Tutorial3Fragment();
        Tutorial4Fragment tur4 = new Tutorial4Fragment();

        adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(tur1, "tutorial1");
        adapter.addFragment(tur2, "tutorial2");
        adapter.addFragment(tur3, "tutorial3");
        adapter.addFragment(tur4, "tutorial4");
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);
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
        lytStart.animate()
                .translationY(0)
                .setDuration(500);
    }
}
