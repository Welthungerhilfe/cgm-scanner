package de.welthungerhilfe.cgm.scanner.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial1Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial2Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial3Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.Tutorial4Fragment;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

public class TutorialActivity extends AppCompatActivity {
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    FragmentAdapter adapter;
    SessionManager session;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

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
            session.setTutorial(true);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            viewPager.setCurrentItem(curpos + 1);
        }
    }
}
