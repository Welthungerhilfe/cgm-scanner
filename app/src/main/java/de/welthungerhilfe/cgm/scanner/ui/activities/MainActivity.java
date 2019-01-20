/**
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerDataAdapter;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonViewModel;
import de.welthungerhilfe.cgm.scanner.ui.delegators.EndlessScrollListener;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.helper.DbConstants;
import de.welthungerhilfe.cgm.scanner.helper.InternalStorageContentProvider;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.service.FileLogMonitorService;
import de.welthungerhilfe.cgm.scanner.helper.service.MemoryMonitorService;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.ui.views.SwipeView;

public class MainActivity extends BaseActivity implements RecyclerDataAdapter.OnPersonDetail, DateRangePickerDialog.Callback {
    private final int REQUEST_LOCATION = 0x1000;
    private final int REQUEST_CAMERA = 0x1001;

    private int sortType = 0;
    private ArrayList<Integer> filters = new ArrayList<>();
    private int diffDays = 0;

    private File mFileTemp;

    private PersonViewModel viewModel;

    @OnClick(R.id.fabCreate)
    void createData(FloatingActionButton fabCreate) {
        Crashlytics.log("Add person by QR");
        //Crashlytics.getInstance().crash();
        startActivity(new Intent(MainActivity.this, QRScanActivity.class));
    }

    @BindView(R.id.recyclerData)
    RecyclerView recyclerData;
    RecyclerDataAdapter adapterData;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.searchbar)
    Toolbar searchbar;
    @BindView(R.id.drawer)
    DrawerLayout drawerLayout;
    @BindView(R.id.navMenu)
    NavigationView navMenu;
    @BindView(R.id.lytNoPerson)
    LinearLayout lytNoPerson;
    @BindView(R.id.searchview)
    SearchView searchView;

    private ActionBarDrawerToggle mDrawerToggle;

    private SessionManager session;
    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Crashlytics.setUserIdentifier(AppController.getInstance().firebaseUser.getEmail());
        Crashlytics.log(0, "user login: ", String.format("user logged in with email %s at %s", AppController.getInstance().firebaseUser.getEmail(), Utils.beautifyDateTime(new Date())));

        session = new SessionManager(MainActivity.this);
        accountManager = AccountManager.get(this);

        setupSidemenu();
        setupActionBar();
        setupRecyclerView();

        adapterData = new RecyclerDataAdapter(this);
        adapterData.setPersonDetailListener(this);
        recyclerData.setAdapter(adapterData);
        recyclerData.addOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onLoadMore() {
                viewModel.loadMorePersons();
            }
        });
        recyclerData.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        viewModel = ViewModelProviders.of(this).get(PersonViewModel.class);
        viewModel.getPersons().observe(this, personList->{
            adapterData.addPersons(personList);
        });

        startService(new Intent(getApplicationContext(), FileLogMonitorService.class));

        fetchRemoteConfig();

        saveFcmToken();

        Log.e("dbPath", getDatabasePath(DbConstants.DATABASE).getAbsolutePath());
    }

    public void onNewIntent(Intent intent) {
        if (adapterData != null)
            adapterData.notifyDataSetChanged();
    }

    private void saveFcmToken() {
        String token = session.getFcmToken();
        String device = Utils.getAndroidID(getContentResolver());
        if (token != null && !session.isFcmSaved()) {
            Map<String, Object> data = new HashMap<>();
            data.put("user", AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
            data.put("device", device);
            data.put("token", token);

            AppController.getInstance().firebaseFirestore.collection("fcm_tokens")
                    .document(device)
                    .set(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            session.setFcmSaved(true);
                        }
                    });
        }
    }

    private void fetchRemoteConfig() {
        long cacheExpiration = 3600 * 3;
        if (AppController.getInstance().firebaseConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        AppController.getInstance().firebaseConfig.fetch(cacheExpiration)
                .addOnSuccessListener(aVoid -> {
                    AppController.getInstance().firebaseConfig.activateFetched();
                    if (AppController.getInstance().firebaseConfig.getBoolean(AppConstants.CONFIG_DEBUG)) {
                        startService(new Intent(this, MemoryMonitorService.class));
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    private void setupSidemenu() {
        navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menuTutorial:
                        Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                        intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
                        startActivity(intent);
                        break;
                    case R.id.menuSettings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;
                    case R.id.menuFastUpload:
                        startActivity(new Intent(MainActivity.this, FastUploadActivity.class));
                        break;
                    case R.id.menuLogout:
                        AppController.getInstance().firebaseAuth.signOut();
                        session.setSigned(false);

                        Account[] accounts = accountManager.getAccounts();
                        for (int i = 0; i < accounts.length; i++) {
                            accountManager.removeAccount(accounts[i], null, null);
                        }

                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                        break;
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });
        View headerView = navMenu.getHeaderView(0);
        TextView txtUsername = headerView.findViewById(R.id.txtUsername);
        txtUsername.setText(AppController.getInstance().firebaseUser.getEmail());
    }

    private void setupActionBar() {
        searchbar.setVisibility(View.GONE);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_scans);
        invalidateOptionsMenu();

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void openSearchBar() {
        searchbar.setVisibility(View.VISIBLE);
        setSupportActionBar(searchbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        invalidateOptionsMenu();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filters.add(4);
                adapterData.setSearchQuery(query);
                doFilter();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        ImageView closeButton = searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery("", false);
                if (filters.contains(4))
                    filters.removeAll(Arrays.asList(4));
                adapterData.setSearchQuery("");
                doFilter();
            }
        });

        ImageView magImage = searchView.findViewById(R.id.search_mag_icon);
        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
    }

    private void setupRecyclerView() {
        SwipeView swipeController = new SwipeView(ItemTouchHelper.LEFT, this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (!AppController.getInstance().isAdmin()) {
                    adapterData.notifyItemChanged(position);

                    Snackbar.make(recyclerData, R.string.permission_delete, Snackbar.LENGTH_LONG).show();
                } else {
                    Person person = adapterData.getItem(position);

                    ConfirmDialog dialog = new ConfirmDialog(MainActivity.this);
                    dialog.setMessage(R.string.delete_person);
                    dialog.setConfirmListener(new ConfirmDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm(boolean result) {
                            if (result) {
                                person.setDeleted(true);
                                person.setDeletedBy(AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
                                person.setTimestamp(Utils.getUniversalTimestamp());
                                // Todo: Write code to update person
                                //OfflineRepository.getInstance(MainActivity.this).updatePerson(person);
                                adapterData.removePerson(person);
                            } else {
                                adapterData.notifyItemChanged(position);
                            }
                        }
                    });
                    dialog.show();
                }
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerData);
    }

    private void createTempFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CGM Scanner");
        if (!mediaStorageDir.exists())
            mediaStorageDir.mkdir();

        String tmp = "IMG_" + Long.toString(Utils.getUniversalTimestamp()) + ".png";

        mFileTemp = new File(mediaStorageDir.getPath() + File.separator + tmp);
    }

    public void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        createTempFile();
        Uri mImageCaptureUri = null;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mImageCaptureUri = Uri.fromFile(mFileTemp);
        } else {
            mImageCaptureUri = InternalStorageContentProvider.CONTENT_URI;
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        takePictureIntent.putExtra("return-data", true);
        startActivityForResult(takePictureIntent, REQUEST_CAMERA);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void doFilter() {
        adapterData.doFilter(filters);
    }

    private void doSort() {
        adapterData.doSort(sortType);
    }

    private void doFilterByDate() {
        DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
        dateRangePicker.setCallback(this);
        dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        dateRangePicker.show(getFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void doFilterByLocation() {
        Intent intent = new Intent(MainActivity.this, LocationSearchActivity.class);
        startActivityForResult(intent, REQUEST_LOCATION);
    }

    private void openSort() {
        ViewHolder viewHolder = new ViewHolder(R.layout.dialog_sort);
        DialogPlus sortDialog = DialogPlus.newDialog(MainActivity.this)
                .setContentHolder(viewHolder)
                .setCancelable(true)
                .setInAnimation(R.anim.abc_fade_in)
                .setOutAnimation(R.anim.abc_fade_out)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {

                        switch (view.getId()) {
                            case R.id.rytFilterData: // own data filter = 1;
                                if (!filters.contains(1)) {
                                    filters.add(1);
                                }

                                dialog.getHolderView().findViewById(R.id.imgFilterData).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.INVISIBLE);

                                doFilter();
                                break;
                            case R.id.rytFilterDate: // date filter = 2;
                                if (!filters.contains(2)) {
                                    filters.add(2);
                                }

                                dialog.getHolderView().findViewById(R.id.imgFilterDate).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.INVISIBLE);

                                doFilterByDate();
                                break;
                            case R.id.rytFilterLocation: // location filter = 3;
                                if (!filters.contains(3)) {
                                    filters.add(3);
                                }

                                dialog.getHolderView().findViewById(R.id.imgFilterLocation).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.INVISIBLE);

                                doFilterByLocation();
                                break;
                            case R.id.rytFilterClear:
                                filters.clear();

                                dialog.getHolderView().findViewById(R.id.imgFilterData).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.VISIBLE);

                                doFilter();
                                break;
                            case R.id.rytSortDate: // date sort = 1;
                                dialog.dismiss();
                                sortType = 1;

                                doSort();
                                break;
                            case R.id.rytSortLocation: // date sort = 2;
                                dialog.dismiss();
                                sortType = 2;

                                doSort();
                                break;
                            case R.id.rytSortWasting: // wasting sort = 3;
                                dialog.dismiss();
                                sortType = 3;

                                doSort();
                                break;
                            case R.id.rytSortStunting: // stunting sort = 4;
                                dialog.dismiss();
                                sortType = 4;

                                doSort();
                                break;
                        }
                    }
                })
                .create();
        TextView txtFilterDate = sortDialog.getHolderView().findViewById(R.id.txtFilterDate);
        txtFilterDate.setText(getResources().getString(R.string.last_days, diffDays));

        TextView txtFilterLocation = sortDialog.getHolderView().findViewById(R.id.txtFilterLocation);
        if (session.getLocation().getAddress().equals("")) {
            txtFilterLocation.setText(R.string.last_location_error);
        } else {
            txtFilterLocation.setText(session.getLocation().getAddress());
        }

        ImageView imgFilterData = sortDialog.getHolderView().findViewById(R.id.imgFilterData);
        ImageView imgFilterDate = sortDialog.getHolderView().findViewById(R.id.imgFilterDate);
        ImageView imgFilterLocation = sortDialog.getHolderView().findViewById(R.id.imgFilterLocation);
        ImageView imgFilterClear = sortDialog.getHolderView().findViewById(R.id.imgFilterClear);
        ImageView imgSortDate = sortDialog.getHolderView().findViewById(R.id.imgSortDate);
        ImageView imgSortLocation = sortDialog.getHolderView().findViewById(R.id.imgSortLocation);
        ImageView imgSortWasting = sortDialog.getHolderView().findViewById(R.id.imgSortWasting);
        ImageView imgSortStunting = sortDialog.getHolderView().findViewById(R.id.imgSortStunting);

        if (filters.size() == 0) {
            imgFilterData.setVisibility(View.INVISIBLE);
            imgFilterDate.setVisibility(View.INVISIBLE);
            imgFilterLocation.setVisibility(View.INVISIBLE);
            imgFilterClear.setVisibility(View.VISIBLE);
        } else {
            imgFilterClear.setVisibility(View.INVISIBLE);
            for (int i = 0; i < filters.size(); i++) {
                if (filters.get(i) == 1) {
                    imgFilterData.setVisibility(View.VISIBLE);
                } else if (filters.get(i) == 2) {
                    imgFilterDate.setVisibility(View.VISIBLE);
                } else if (filters.get(i) == 3) {
                    imgFilterLocation.setVisibility(View.VISIBLE);
                }
            }
        }

        switch (sortType) {
            case 1:
                imgSortDate.setVisibility(View.VISIBLE);
                break;
            case 2:
                imgSortLocation.setVisibility(View.VISIBLE);
                break;
            case 3:
                imgSortWasting.setVisibility(View.VISIBLE);
                break;
            case 4:
                imgSortStunting.setVisibility(View.VISIBLE);
                break;
        }

        sortDialog.show();
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        Calendar start = selectedDate.getStartDate();
        Calendar end = selectedDate.getEndDate();

        diffDays = (int) (end.getTimeInMillis() - start.getTimeInMillis()) / 1000 / 60 / 60 / 24;
        long startDate = start.getTimeInMillis();
        long endDate = end.getTimeInMillis();
        if (start.getTimeInMillis() == end.getTimeInMillis()) {
            diffDays = 1;
            Date date = new Date(start.get(Calendar.YEAR) - 1900, start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

            startDate = date.getTime();
            endDate = startDate + (3600 * 24 - 1) * 1000;
        }

        adapterData.setDateFilter(startDate, endDate);
        doFilter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();

        if (searchbar.getVisibility() == View.VISIBLE) {
            menuInflater.inflate(R.menu.menu_search, menu);
        } else {
            menuInflater.inflate(R.menu.menu_tool, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.actionSearch) {
            openSearchBar();
        } else if (menuItem.getItemId() == R.id.actionQr) {
            startActivity(new Intent(MainActivity.this, QRScanActivity.class));
        } else if (menuItem.getItemId() == R.id.actionFilter) {
            openSort();
        } else if (menuItem.getItemId() == android.R.id.home) {
            if (searchbar.getVisibility() == View.VISIBLE) {
                setupActionBar();
            } else {
                finish();
            }
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent result) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            int radius = result.getIntExtra(AppConstants.EXTRA_RADIUS, 0);

            adapterData.setLocationFilter(session.getLocation(), radius);
            doFilter();
        } else if (reqCode == REQUEST_CAMERA) {
            if (resCode == RESULT_OK) {
                Uri mImageUri = Uri.fromFile(mFileTemp);

                try {
                    FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(MainActivity.this, mImageUri);

                    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE, FirebaseVisionBarcode.FORMAT_AZTEC)
                            .build();


                    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                            .getVisionBarcodeDetector();

                    Task<List<FirebaseVisionBarcode>> scanResult = detector.detectInImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                                    // Task completed successfully
                                    // ...
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    // ...
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPersonDetail(Person person) {
        Intent intent = new Intent(MainActivity.this, CreateDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_PERSON, person);
        startActivity(intent);
    }
}
