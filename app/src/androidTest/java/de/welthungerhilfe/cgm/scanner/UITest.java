package de.welthungerhilfe.cgm.scanner;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import de.welthungerhilfe.cgm.scanner.hardware.io.LocalPersistency;
import de.welthungerhilfe.cgm.scanner.ui.activities.DeviceCheckActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.LoginActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UITest extends UITestUtils {

    @Rule public ActivityTestRule<LoginActivity> loginActivityActivityTestRule = new ActivityTestRule<>(LoginActivity.class);
    @Rule public ActivityTestRule<MainActivity> mainActivityActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void init() {
        //disable tutorial on start
        MainActivity activity = mainActivityActivityTestRule.getActivity();
        new SessionManager(activity).setTutorial(true);

        //disable device check popup
        LocalPersistency.setLong(activity, DeviceCheckActivity.KEY_LAST_DEVICE_CHECK, System.currentTimeMillis());

        //logout
        activity.logout();
    }

    @Test
    public void test01_loginDemoQA() {
        onView(withId(R.id.rb_demo_qa)).perform(click());
        onView(withId(R.id.btnLoginMicrosoft)).perform(click());
    }

    @Test
    public void test02_tutorial() {
        onView(withId(R.id.drawer)).perform(open());
        onView(withId(R.id.navMenu)).perform(NavigationViewActions.navigateTo(R.id.menuTutorial));
        Utils.sleep(TRANSITION_DELAY);

        //screen 1
        onView(withText(R.string.tutorial11)).perform(click());
        onView(withText(R.string.tutorial12)).perform(click());
        onView(withIdAndIndex(R.id.btnNext, 0)).perform(click());
        Utils.sleep(TRANSITION_DELAY);

        //screen 2
        onView(withText(R.string.tutorial21)).perform(click());
        onView(withText(R.string.tutorial22)).perform(click());
        onView(withIdAndIndex(R.id.btnNext, 1)).perform(click());
        Utils.sleep(TRANSITION_DELAY);

        //screen 3
        onView(withText(R.string.tutorial31)).perform(click());
        onView(withText(R.string.tutorial32)).perform(click());
        onView(withIdAndIndex(R.id.btnNext, 2)).perform(click());
        Utils.sleep(TRANSITION_DELAY);

        //screen 4
        onView(withText(R.string.tutorial41)).perform(click());
        onView(withText(R.string.tutorial42)).perform(click());
        onView(withIdAndIndex(R.id.btnNext, 3)).perform(click());
        Utils.sleep(TRANSITION_DELAY);

        //final screen
        onView(withId(R.id.btnStart)).perform(click());
    }
}