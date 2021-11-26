package de.welthungerhilfe.cgm.scanner;

import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.internal.matchers.TypeSafeMatcher;

public class UITestUtils {

    protected final int TRANSITION_DELAY = 1000;

    @Rule public GrantPermissionRule permissionRule01 = GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule public GrantPermissionRule permissionRule02 = GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule public GrantPermissionRule permissionRule03 = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule public GrantPermissionRule permissionRule04 = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);
    @Rule public GrantPermissionRule permissionRule05 = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_COARSE_LOCATION);
    @Rule public GrantPermissionRule permissionRule06 = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_NETWORK_STATE);
    @Rule public GrantPermissionRule permissionRule07 = GrantPermissionRule.grant(android.Manifest.permission.READ_SYNC_STATS);
    @Rule public GrantPermissionRule permissionRule08 = GrantPermissionRule.grant(android.Manifest.permission.READ_SYNC_SETTINGS);
    @Rule public GrantPermissionRule permissionRule09 = GrantPermissionRule.grant(android.Manifest.permission.WRITE_SYNC_SETTINGS);
    @Rule public GrantPermissionRule permissionRule10 = GrantPermissionRule.grant(android.Manifest.permission.VIBRATE);
    @Rule public GrantPermissionRule permissionRule11 = GrantPermissionRule.grant(android.Manifest.permission.WAKE_LOCK);
    @Rule public GrantPermissionRule permissionRule12 = GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);
    @Rule public GrantPermissionRule permissionRule13 = GrantPermissionRule.grant(android.Manifest.permission.FOREGROUND_SERVICE);
    @Rule public GrantPermissionRule permissionRule14 = GrantPermissionRule.grant(android.Manifest.permission.CHANGE_WIFI_STATE);
    @Rule public GrantPermissionRule permissionRule15 = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_WIFI_STATE);
    @Rule public GrantPermissionRule permissionRule16 = GrantPermissionRule.grant(android.Manifest.permission.INTERNET);

    public static Matcher<View> withIdAndIndex(int id, int index) {
        Matcher<View> matcher = withId(id);
        return new TypeSafeMatcher<View>() {

            private int current = 0;

            @Override
            public void describeTo(Description description) {
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                boolean matches = matcher.matches(view);
                if (matches) {
                    current++;
                    return current -1 == index;
                }
                return false;
            }
        };
    }
}
