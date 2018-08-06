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

package de.welthungerhilfe.cgm.scanner.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerDataAdapter;
import de.welthungerhilfe.cgm.scanner.delegators.SwipeViewActions;
import de.welthungerhilfe.cgm.scanner.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.helper.InternalStorageContentProvider;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.service.FileLogMonitorService;
import de.welthungerhilfe.cgm.scanner.helper.service.FirebaseUploadService;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.viewmodels.PersonListViewModel;
import de.welthungerhilfe.cgm.scanner.views.SwipeView;

public class MainActivity extends BaseActivity implements RecyclerDataAdapter.OnPersonDetail, DateRangePickerDialog.Callback, EventListener<QuerySnapshot> {
    private final String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_LOCATION = 0x1000;
    private final int REQUEST_CAMERA = 0x1001;

    private final int PERMISSION_CAMERA = 0x1002;

    private int sortType = 0;
    private int diffDays = 0;
    private ArrayList<Person> personList = new ArrayList<>();

    private File mFileTemp;

    private PersonListViewModel viewModel;

    @OnClick(R.id.fabCreate)
    void createData(FloatingActionButton fabCreate) {
        Crashlytics.log("Add person by QR");
        startActivity(new Intent(MainActivity.this, QRScanActivity.class));

        /*
        if (!Utils.checkPermission(MainActivity.this, "android.permission.CAMERA") || !Utils.checkPermission(MainActivity.this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSION_CAMERA);
        } else {
            takePhoto();
        }
        */
    }

    @OnClick(R.id.txtSort)
    void doSort(TextView txtSort) {
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
                            case R.id.rytSortDate:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByDate();
                                break;
                            case R.id.rytSortLocation:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByLocation();
                                break;
                            case R.id.rytSortWasting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);

                                txtSortCase.setText(R.string.wasting_weight_height);
                                dialog.dismiss();

                                doSortByWasting();
                                break;
                            case R.id.rytSortStunting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);

                                txtSortCase.setText(R.string.stunting_height_age);
                                dialog.dismiss();

                                doSortByStunting();
                                break;
                            case R.id.rytSortClear:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.VISIBLE);

                                txtSortCase.setText(R.string.no_filter);
                                dialog.dismiss();

                                clearFilters();
                                break;
                        }
                    }
                })
                .create();
        TextView txtSortDate = sortDialog.getHolderView().findViewById(R.id.txtSortDate);
        txtSortDate.setText(getResources().getString(R.string.last_days, diffDays));

        TextView txtSortLocation = sortDialog.getHolderView().findViewById(R.id.txtSortLocation);
        if (session.getLocation().getAddress().equals("")) {
            txtSortLocation.setText(R.string.last_location_error);
        } else {
            txtSortLocation.setText(session.getLocation().getAddress());
        }

        ImageView imgSortDate = sortDialog.getHolderView().findViewById(R.id.imgSortDate);
        ImageView imgSortLocation = sortDialog.getHolderView().findViewById(R.id.imgSortLocation);
        ImageView imgSortWasting = sortDialog.getHolderView().findViewById(R.id.imgSortWasting);
        ImageView imgSortStunting = sortDialog.getHolderView().findViewById(R.id.imgSortStunting);
        ImageView imgSortClear = sortDialog.getHolderView().findViewById(R.id.imgSortClear);
        switch (sortType) {
            case 0:
                imgSortClear.setVisibility(View.VISIBLE);
                break;
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

    @BindView(R.id.recyclerData)
    RecyclerView recyclerData;
    RecyclerDataAdapter adapterData;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer)
    DrawerLayout drawerLayout;
    @BindView(R.id.navMenu)
    NavigationView navMenu;
    @BindView(R.id.txtSortCase)
    TextView txtSortCase;
    @BindView(R.id.txtNoPerson)
    TextView txtNoPerson;

    private ActionBarDrawerToggle mDrawerToggle;

    private SessionManager session;
    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        session = new SessionManager(MainActivity.this);
        accountManager = AccountManager.get(this);

        setupSidemenu();
        setupActionBar();
        setupRecyclerView();

        txtSortCase.setText(getResources().getString(R.string.last_scans, 0));

        showProgressDialog();

        viewModel = ViewModelProviders.of(this).get(PersonListViewModel.class);
        viewModel.getObservablePersonList().observe(this, personList->{
            if (personList.size() == 0) {
                txtNoPerson.setVisibility(View.VISIBLE);
                recyclerData.setVisibility(View.GONE);
            } else {
                txtNoPerson.setVisibility(View.GONE);
                recyclerData.setVisibility(View.VISIBLE);

                adapterData = new RecyclerDataAdapter(this, personList);
                adapterData.setPersonDetailListener(this);
                recyclerData.setAdapter(adapterData);
                recyclerData.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            }
        });

        checkLocalFiles();
        checkDeletedRecords();

        startService(new Intent(this, FileLogMonitorService.class));
    }

    private void setupSidemenu() {
        navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menuHome:
                        break;
                    case R.id.menuSettings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
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
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.title_scans);

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

    private void setupRecyclerView() {
        SwipeView swipeController = new SwipeView(ItemTouchHelper.LEFT, this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (!AppController.getInstance().firebaseAuth.getCurrentUser().getEmail().equals("mmatiaschek@gmail.com") && !AppController.getInstance().firebaseAuth.getCurrentUser().getEmail().equals("zhangnemo34@hotmail.com")) {
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
                                OfflineRepository.getInstance().updatePerson(person);
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

    private void doSortByDate() {
        sortType = 1;

        DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
        dateRangePicker.setCallback(this);
        dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        dateRangePicker.show(getFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void doSortByLocation() {
        sortType = 2;

        Intent intent = new Intent(MainActivity.this, LocationSearchActivity.class);
        startActivityForResult(intent, REQUEST_LOCATION);
    }

    private void doSortByWasting() {
        sortType = 3;

        adapterData.setWastingFilter();
    }

    private void doSortByStunting() {
        sortType = 4;

        adapterData.setStuntingFilter();
    }

    private void clearFilters() {
        sortType = 0;
        adapterData.clearFitlers();
    }

    private void checkLocalFiles() {
        File root = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());
        File[] qrCodes = root.listFiles();
        for (File qrCode : qrCodes) {

            if (qrCode.isDirectory()) {
                File[] measurements = qrCode.listFiles();

                for (File measure : measurements) {
                    File[] timestamps = measure.listFiles();

                    for (File timestamp : timestamps) {
                        File[] types = timestamp.listFiles();

                        for (File type : types) {
                            File[] datas = type.listFiles();

                            for (File data : datas) {
                                new OfflineTask().getFileLog(data.getPath(), new OfflineTask.OnLoadFileLog() {
                                    @Override
                                    public void onLoadFileLog(FileLog log) {
                                        if (log != null) {
                                            startService(new Intent(MainActivity.this, FirebaseUploadService.class)
                                                    .putExtra(FirebaseUploadService.EXTRA_FILE_URI, Uri.fromFile(data))
                                                    .putExtra(AppConstants.EXTRA_QR, qrCode.getName())
                                                    .putExtra(AppConstants.EXTRA_SCANTIMESTAMP, timestamp.getName())
                                                    .putExtra(AppConstants.EXTRA_SCANARTEFACT_SUBFOLDER, AppConstants.STORAGE_CONSENT_URL)
                                                    .setAction(FirebaseUploadService.ACTION_UPLOAD));
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkDeletedRecords() {
        new OfflineTask().deleteRecords(session.getSyncTimestamp());
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

        txtSortCase.setText(getResources().getString(R.string.last_scans, diffDays));
        adapterData.setDateFilter(startDate, endDate);
    }

    @Override
    public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException e) {
        List<DocumentChange> documents = snapshot.getDocumentChanges();
        for (DocumentChange change: documents) {
            Person person = change.getDocument().toObject(Person.class);
            if (change.getType().equals(DocumentChange.Type.ADDED)) {
                adapterData.addPerson(person);
            } else if (change.getType().equals(DocumentChange.Type.MODIFIED)) {
                adapterData.updatePerson(person);
            } else if (change.getType().equals(DocumentChange.Type.REMOVED)) {
                adapterData.removePerson(person);
            }
            if (adapterData.getItemCount() == 0) {
                recyclerData.setVisibility(View.GONE);
                txtNoPerson.setVisibility(View.VISIBLE);
            } else {
                recyclerData.setVisibility(View.VISIBLE);
                txtNoPerson.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(MainActivity.this.getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapterData.search(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA && grantResults[0] >= 0 && grantResults[1] >= 0) {
            takePhoto();
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent result) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            int radius = result.getIntExtra(AppConstants.EXTRA_RADIUS, 0);

            adapterData.setLocationFilter(session.getLocation(), radius);
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
