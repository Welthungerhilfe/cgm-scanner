package de.welthungerhilfe.cgm.scanner.activities;


import android.Manifest;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.MeasureScanFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING_FRONT;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_LYING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SCAN_STANDING_FRONT;

public class ScanModeActivity extends AppCompatActivity {
    private final String SCAN_FRAGMENT = "scan_fragment";
    private final int PERMISSION_LOCATION = 0x0001;

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
        MeasureScanFragment scanFragment = new MeasureScanFragment();
        if (SCAN_MODE == SCAN_STANDING)
            scanFragment.setMode(SCAN_STANDING_FRONT);
        else if (SCAN_MODE == SCAN_LYING)
            scanFragment.setMode(SCAN_LYING_FRONT);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .add(R.id.scanner, scanFragment, SCAN_FRAGMENT)
                .addToBackStack(null)
                .commit();
    }
    @OnClick(R.id.btnScanStep2)
    void scanStep2(Button btnScanStep2) {

    }
    @OnClick(R.id.btnScanStep3)
    void scanStep3(Button btnScanStep3) {

    }

    private static final String TAG = ScanModeActivity.class.getSimpleName();

    public int SCAN_MODE = SCAN_STANDING;
    public int SCAN_STEP = 0;

    public final int SCAN_FRONT = 1;
    public final int SCAN_SIDE = 2;
    public final int SCAN_BACK = 3;

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

    public void closeScan() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SCAN_FRAGMENT);
        if (fragment != null)
            getSupportFragmentManager().popBackStack();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION && grantResults[0] >= 0) {
            getCurrentLocation();
        }
    }

    public void onBackPressed() {
        finish();
    }
}
