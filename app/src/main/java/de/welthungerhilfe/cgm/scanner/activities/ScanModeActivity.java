package de.welthungerhilfe.cgm.scanner.activities;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.MeasureScanFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_SIDE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_BACK;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_SIDE;

public class ScanModeActivity extends AppCompatActivity {
    private final String SCAN_FRAGMENT = "scan_fragment";
    private final int PERMISSION_LOCATION = 0x0001;
    private final int PERMISSION_CAMERA = 0x0002;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.imgScanStanding)
    ImageView imgScanStanding;
    @BindView(R.id.imgScanStandingCheck)
    ImageView imgScanStandingCheck;
    @BindView(R.id.txtScanStanding)
    TextView txtScanStanding;

    @BindView(R.id.imgScanLying)
    ImageView imgScanLying;
    @BindView(R.id.imgScanLyingCheck)
    ImageView imgScanLyingCheck;
    @BindView(R.id.txtScanLying)
    TextView txtScanLying;

    @BindView(R.id.imgScanStep1)
    ImageView imgScanStep1;
    @BindView(R.id.imgScanStep2)
    ImageView imgScanStep2;
    @BindView(R.id.imgScanStep3)
    ImageView imgScanStep3;

    @BindView(R.id.btnScanStep1)
    Button btnScanStep1;
    @BindView(R.id.btnScanStep2)
    Button btnScanStep2;
    @BindView(R.id.btnScanStep3)
    Button btnScanStep3;
    @BindView(R.id.btnScanComplete)
    Button btnScanComplete;

    @BindView(R.id.lytScanStep1)
    LinearLayout lytScanStep1;
    @BindView(R.id.lytScanStep2)
    LinearLayout lytScanStep2;
    @BindView(R.id.lytScanStep3)
    LinearLayout lytScanStep3;

    @BindView(R.id.lytSelectMode)
    LinearLayout lytSelectMode;
    @BindView(R.id.lytSelectedMode)
    LinearLayout lytSelectedMode;
    @BindView(R.id.imgSelectedMode)
    ImageView imgSelectedMode;
    @BindView(R.id.txtSelectedMode)
    TextView txtSelectedMode;

    @OnClick(R.id.lytScanStanding)
    void scanStanding(LinearLayout lytScanStanding) {
        SCAN_MODE = SCAN_STANDING;

        imgScanStanding.setImageResource(R.drawable.standing_active);
        imgScanStandingCheck.setImageResource(R.drawable.radio_active);
        txtScanStanding.setTextColor(getResources().getColor(R.color.colorBlack));

        imgScanLying.setImageResource(R.drawable.lying_inactive);
        imgScanLyingCheck.setImageResource(R.drawable.radio_inactive);
        txtScanLying.setTextColor(getResources().getColor(R.color.colorGreyDark));

        changeMode();
    }
    @OnClick(R.id.lytScanLying)
    void scanLying(LinearLayout lytScanLying) {
        SCAN_MODE = SCAN_LYING;

        imgScanLying.setImageResource(R.drawable.lying_active);
        imgScanLyingCheck.setImageResource(R.drawable.radio_active);
        txtScanLying.setTextColor(getResources().getColor(R.color.colorBlack));

        imgScanStanding.setImageResource(R.drawable.standing_inactive);
        imgScanStandingCheck.setImageResource(R.drawable.radio_inactive);
        txtScanStanding.setTextColor(getResources().getColor(R.color.colorGreyDark));

        changeMode();
    }
    @OnClick(R.id.btnScanStep1)
    void scanStep1(Button btnScanStep1) {
        /*
        lytSelectMode.setVisibility(View.GONE);
        lytSelectedMode.setVisibility(View.VISIBLE);
        */

        MeasureScanFragment scanFragment = new MeasureScanFragment();
        if (SCAN_MODE == SCAN_STANDING) {
            imgSelectedMode.setImageResource(R.drawable.standing_active);
            txtSelectedMode.setText(R.string.mode_standing);

            SCAN_STEP = SCAN_STANDING_FRONT;
        } else if (SCAN_MODE == SCAN_LYING) {
            imgSelectedMode.setImageResource(R.drawable.lying_active);
            txtSelectedMode.setText(R.string.mode_lying);

            SCAN_STEP = SCAN_LYING_FRONT;
        }

        scanFragment.setMode(SCAN_STEP);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.scanner, scanFragment, SCAN_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }
    @OnClick(R.id.btnScanStep2)
    void scanStep2(Button btnScanStep2) {
        MeasureScanFragment scanFragment = new MeasureScanFragment();
        if (SCAN_MODE == SCAN_STANDING) {
            imgSelectedMode.setImageResource(R.drawable.standing_active);
            txtSelectedMode.setText(R.string.mode_standing);

            SCAN_STEP = SCAN_STANDING_SIDE;
        } else if (SCAN_MODE == SCAN_LYING) {
            imgSelectedMode.setImageResource(R.drawable.lying_active);
            txtSelectedMode.setText(R.string.mode_lying);

            SCAN_STEP = SCAN_LYING_SIDE;
        }

        scanFragment.setMode(SCAN_STEP);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.scanner, scanFragment, SCAN_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }
    @OnClick(R.id.btnScanStep3)
    void scanStep3(Button btnScanStep3) {
        MeasureScanFragment scanFragment = new MeasureScanFragment();
        if (SCAN_MODE == SCAN_STANDING) {
            imgSelectedMode.setImageResource(R.drawable.standing_active);
            txtSelectedMode.setText(R.string.mode_standing);

            SCAN_STEP = SCAN_STANDING_BACK;
        } else if (SCAN_MODE == SCAN_LYING) {
            imgSelectedMode.setImageResource(R.drawable.lying_active);
            txtSelectedMode.setText(R.string.mode_lying);

            SCAN_STEP = SCAN_LYING_BACK;
        }
        scanFragment.setMode(SCAN_STEP);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.scanner, scanFragment, SCAN_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }
    @OnClick(R.id.btnScanComplete)
    void completeScan(Button btnScanComplete) {
        completeScan();
    }

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = SCAN_STANDING;
    public int SCAN_STEP = 0;
    private boolean step1 = false, step2 = false, step3 = false;

    public Person person;
    public Measure measure;
    public Loc location;

    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        measure = (Measure) getIntent().getSerializableExtra(AppConstants.EXTRA_MEASURE);
        if (person == null) Log.e(TAG,"person was null!");
        if (measure == null) Log.e(TAG,"measure was null!");

        setContentView(R.layout.activity_scan_mode);

        ButterKnife.bind(this);

        setupToolbar();

        getCurrentLocation();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_CAMERA);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_add_measure);
    }

    private void changeMode() {
        if (SCAN_MODE == SCAN_STANDING) {
            imgScanStep1.setImageResource(R.drawable.stand_front_active);
            imgScanStep2.setImageResource(R.drawable.stand_side_active);
            imgScanStep3.setImageResource(R.drawable.stand_back_active);
        } else if (SCAN_MODE == SCAN_LYING) {
            imgScanStep1.setImageResource(R.drawable.lying_front_active);
            imgScanStep2.setImageResource(R.drawable.lying_side_active);
            imgScanStep3.setImageResource(R.drawable.lying_back_active);
        }
    }

    public void goToNextStep() {
        closeScan();

        if (SCAN_STEP == SCAN_STANDING_FRONT || SCAN_STEP == SCAN_LYING_FRONT) {
            lytScanStep1.setVisibility(View.GONE);
            btnScanStep1.setText(R.string.retake_scan);
            btnScanStep1.setTextColor(getResources().getColor(R.color.colorWhite));
            btnScanStep1.setBackground(getResources().getDrawable(R.drawable.button_green_circular));

            step1 = true;
        } else if (SCAN_STEP == SCAN_STANDING_SIDE || SCAN_STEP == SCAN_LYING_SIDE) {
            lytScanStep2.setVisibility(View.GONE);
            btnScanStep2.setText(R.string.retake_scan);
            btnScanStep2.setTextColor(getResources().getColor(R.color.colorWhite));
            btnScanStep2.setBackground(getResources().getDrawable(R.drawable.button_green_circular));

            step2 = true;
        } else if (SCAN_STEP == SCAN_STANDING_BACK || SCAN_STEP == SCAN_LYING_BACK) {
            lytScanStep3.setVisibility(View.GONE);
            btnScanStep3.setText(R.string.retake_scan);
            btnScanStep3.setTextColor(getResources().getColor(R.color.colorWhite));
            btnScanStep3.setBackground(getResources().getDrawable(R.drawable.button_green_circular));

            step3 = true;
        }

        if (step1 && step2 && step3) {
            showCompleteButton();
        }
    }

    private void showCompleteButton() {
        btnScanComplete.setVisibility(View.VISIBLE);
        btnScanComplete.requestFocus();

        if (android.os.Build.VERSION.SDK_INT >=  android.os.Build.VERSION_CODES.LOLLIPOP) {
            int cx = (btnScanComplete.getLeft() + btnScanComplete.getRight()) / 2;
            int cy = (btnScanComplete.getTop() + btnScanComplete.getBottom()) / 2;

            int dx = Math.max(cx, btnScanComplete.getWidth() - cx);
            int dy = Math.max(cy, btnScanComplete.getHeight() - cy);
            float finalRadius = (float) Math.hypot(dx, dy);

            Animator animator = ViewAnimationUtils.createCircularReveal(btnScanComplete, cx, cy, 0, finalRadius);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(300);
            animator.start();
        }
    }

    private void hideCompleteButton() {
        if (android.os.Build.VERSION.SDK_INT >=  android.os.Build.VERSION_CODES.LOLLIPOP) {
            int cx = (btnScanComplete.getLeft() + btnScanComplete.getRight()) / 2;
            int cy = (btnScanComplete.getTop() + btnScanComplete.getBottom()) / 2;

            int dx = Math.max(cx, btnScanComplete.getWidth() - cx);
            int dy = Math.max(cy, btnScanComplete.getHeight() - cy);
            float finalRadius = (float) Math.hypot(dx, dy);

            Animator animator = ViewAnimationUtils.createCircularReveal(btnScanComplete, cx, cy, finalRadius, 0);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    btnScanComplete.setVisibility(View.GONE);
                }
            });
            animator.start();
        } else {
            btnScanComplete.setVisibility(View.GONE);
        }
    }

    public void closeScan() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SCAN_FRAGMENT);
        if (fragment != null)
            getSupportFragmentManager().popBackStack();
    }

    public void completeScan() {
        if (measure == null)
            measure = new Measure();
        if (location != null)
            measure.setLocation(location);
        measure.setCreatedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
        measure.setDate(Utils.getUniversalTimestamp());
        measure.setType("v1.1.2");
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setType(AppConstants.VAL_MEASURE_AUTO);
        measure.setWeight(0.0f);
        measure.setHeight(0.0f);
        measure.setHeadCircumference(0.0f);
        measure.setMuac(0.0f);
        measure.setOedema(false);
        if (measure.getId() == null)
            EventBus.getDefault().post(new MeasureResult(measure));
        else
            new OfflineRepository().updateMeasure(measure);

        finish();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location loc = null;

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (isNetworkEnabled || isGPSEnabled) {
                List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (loc == null || l.getAccuracy() < loc.getAccuracy()) {
                        loc = l;
                    }
                }
                if (loc != null) {
                    getAddressFromLocation(loc.getLatitude(), loc.getLongitude());
                }
            }
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(ScanModeActivity.this, Locale.getDefault());
                String result = null;
                try {
                    List <Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addressList != null && addressList.size() > 0) {
                        Address address = addressList.get(0);
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++)
                            sb.append(address.getAddressLine(i));

                        result = sb.toString();
                    }
                } catch (IOException e) {
                    Log.e("Location Address Loader", "Unable connect to Geocoder", e);
                    Crashlytics.log(Log.ERROR, TAG, "IOException Unable connect to Geocoder");
                } finally {
                    location = new Loc();

                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    location.setAddress(result);

                    if (measure != null)
                        measure.setLocation(location);
                }
            }
        });
        thread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION && grantResults.length > 0 && grantResults[0] >= 0) {
            getCurrentLocation();
        } else if (requestCode == PERMISSION_CAMERA && (grantResults.length == 0 || grantResults[0] < 0)) {
            Toast.makeText(ScanModeActivity.this, R.string.permission_camera, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SCAN_FRAGMENT);
        if (fragment != null)
            getSupportFragmentManager().popBackStack();
        else
            finish();
    }
}
