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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
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
import com.microsoft.appcenter.auth.Auth;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonListViewModel;
import de.welthungerhilfe.cgm.scanner.helper.service.DeviceService;
import de.welthungerhilfe.cgm.scanner.ui.adapters.RecyclerPersonAdapter;
import de.welthungerhilfe.cgm.scanner.ui.delegators.EndlessScrollListener;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import de.welthungerhilfe.cgm.scanner.ui.views.SwipeView;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_DATE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_LOCATION;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_STUNTING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_WASTING;

public class MainActivity extends BaseActivity implements RecyclerPersonAdapter.OnPersonDetail, DateRangePickerDialog.Callback {
    private final int REQUEST_LOCATION = 0x1000;
    private final int REQUEST_CAMERA = 0x1001;

    private File mFileTemp;

    private PersonListViewModel viewModel;

    @OnClick(R.id.fabCreate)
    void createData(FloatingActionButton fabCreate) {
        startActivity(new Intent(MainActivity.this, QRScanActivity.class));
    }

    @BindView(R.id.recyclerData)
    RecyclerView recyclerData;
    RecyclerPersonAdapter adapterData;
    LinearLayoutManager lytManager;
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

    private DialogPlus sortDialog;

    private SessionManager session;
    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        session = new SessionManager(MainActivity.this);
        accountManager = AccountManager.get(this);

        viewModel = ViewModelProviders.of(this).get(PersonListViewModel.class);
        final Observer<List<Person>> observer = list -> {
            Log.e("PersonRecycler", "Observer called");
            if (lytManager.getItemCount() == 0 && list != null && list.size() == 0) {
                lytNoPerson.setVisibility(View.VISIBLE);
            } else {
                lytNoPerson.setVisibility(View.GONE);
                adapterData.addPersons(list);
            }
        };
        viewModel.getPersonListLiveData().observe(this, observer);

        setupSidemenu();
        setupActionBar();
        setupRecyclerView();
        setupSortDialog();

        adapterData = new RecyclerPersonAdapter(this);
        adapterData.setPersonDetailListener(this);

        lytManager = new LinearLayoutManager(MainActivity.this);
        recyclerData.setLayoutManager(lytManager);
        recyclerData.setItemAnimator(new DefaultItemAnimator());
        recyclerData.setHasFixedSize(true);
        recyclerData.setAdapter(adapterData);
        recyclerData.addOnScrollListener(new EndlessScrollListener(lytManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                viewModel.setCurrentPage(page);
            }
        });

        startService(new Intent(this, DeviceService.class));
    }

    public void onNewIntent(Intent intent) {
        if (adapterData != null)
            adapterData.notifyDataSetChanged();
    }

    private void setupSidemenu() {
        navMenu.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menuTutorial:
                    Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                    intent.putExtra(AppConstants.EXTRA_TUTORIAL_AGAIN, true);
                    startActivity(intent);
                    break;
                case R.id.menuSettings:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    break;
                case R.id.menuLogout:
                    session.setSigned(false);
                    session.setAzureAccountName("");
                    session.setAzureAccountKey("");

                    Account[] accounts = accountManager.getAccounts();
                    for (Account account : accounts) {
                        accountManager.removeAccount(account, MainActivity.this, null, null);
                    }

                    Auth.signOut();

                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    break;
            }
            drawerLayout.closeDrawers();
            return true;
        });
        View headerView = navMenu.getHeaderView(0);
        TextView txtUsername = headerView.findViewById(R.id.txtUsername);
        txtUsername.setText(session.getUserEmail());
    }

    private void setupActionBar() {
        searchbar.setVisibility(View.GONE);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.title_scans);
            invalidateOptionsMenu();
        }

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
                adapterData.clear();
                viewModel.setFilterQuery(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        ImageView closeButton = searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            searchView.setQuery("", false);

            viewModel.clearFilterOwn();
        });

        ImageView magImage = searchView.findViewById(R.id.search_mag_icon);
        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
    }

    private void setupRecyclerView() {
        SwipeView swipeController = new SwipeView(ItemTouchHelper.LEFT, this) {
            @SuppressLint("StaticFieldLeak")
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
                    dialog.setConfirmListener(result -> {
                        if (result) {
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... voids) {
                                    person.setDeleted(true);
                                    person.setDeletedBy(session.getUserEmail());
                                    person.setTimestamp(Utils.getUniversalTimestamp());

                                    return null;
                                }

                                public void onPostExecute(Void result) {
                                    viewModel.updatePerson(person);
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else {
                            adapterData.notifyItemChanged(position);
                        }
                    });
                    dialog.show();
                }
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerData);
    }

    private void setupSortDialog() {
        sortDialog = DialogPlus.newDialog(MainActivity.this)
                .setContentHolder(new ViewHolder(R.layout.dialog_sort))
                .setCancelable(true)
                .setInAnimation(R.anim.abc_fade_in)
                .setOutAnimation(R.anim.abc_fade_out)
                .setOnClickListener((dialog, view) -> {

                    switch (view.getId()) {
                        case R.id.rytFilterData:
                            viewModel.setFilterOwn();
                            break;
                        case R.id.rytFilterDate:
                            doFilterByDate();
                            break;
                        case R.id.rytFilterLocation:
                            doFilterByLocation();
                            break;
                        case R.id.rytFilterClear:
                            viewModel.setFilterNo();
                            break;
                        case R.id.rytSortDate:
                            viewModel.setSortType(SORT_DATE);
                            break;
                        case R.id.rytSortLocation:
                            viewModel.setSortType(SORT_LOCATION);
                            break;
                        case R.id.rytSortWasting:
                            viewModel.setSortType(SORT_WASTING);
                            break;
                        case R.id.rytSortStunting:
                            viewModel.setSortType(SORT_STUNTING);
                            break;
                    }

                    dialog.dismiss();
                })
                .create();

        viewModel.getPersonFilterLiveData().observe(this, filter -> {
            if (filter.isOwn()) {
                sortDialog.getHolderView().findViewById(R.id.imgFilterData).setVisibility(View.VISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgFilterData).setVisibility(View.INVISIBLE);
            }

            if (filter.isDate()) {
                sortDialog.getHolderView().findViewById(R.id.imgFilterDate).setVisibility(View.VISIBLE);

                int diff = (int) Math.ceil((double) (filter.getToDate() - filter.getFromDate()) / 1000 / 3600 / 24);
                TextView txtView = sortDialog.getHolderView().findViewById(R.id.txtFilterDate);
                txtView.setText(getResources().getString(R.string.last_days, diff));
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgFilterDate).setVisibility(View.INVISIBLE);
            }

            if (filter.isLocation()) {
                sortDialog.getHolderView().findViewById(R.id.imgFilterLocation).setVisibility(View.VISIBLE);
                TextView txtView = sortDialog.getHolderView().findViewById(R.id.txtFilterLocation);

                if (filter.getFromLOC() != null) {
                    txtView.setText(filter.getFromLOC().getAddress());
                } else {
                    txtView.setText(R.string.last_location_error);
                }
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgFilterLocation).setVisibility(View.INVISIBLE);
            }

            if (filter.isOwn() || filter.isDate() || filter.isLocation()) {
                sortDialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.INVISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgFilterClear).setVisibility(View.VISIBLE);
            }

            if (filter.getSortType() == SORT_DATE) {
                sortDialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.VISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
            }

            if (filter.getSortType() == SORT_LOCATION) {
                sortDialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.VISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
            }

            if (filter.getSortType() == SORT_WASTING) {
                sortDialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.VISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
            }

            if (filter.getSortType() == SORT_STUNTING) {
                sortDialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.VISIBLE);
            } else {
                sortDialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
            }
        });
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
        sortDialog.show();
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        Calendar start = selectedDate.getStartDate();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startDate = start.getTimeInMillis();

        Calendar end = selectedDate.getEndDate();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        long endDate = end.getTimeInMillis();

        adapterData.clear();
        viewModel.setFilterDate(startDate, endDate);
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
        switch (menuItem.getItemId()) {
            case R.id.actionSearch:
                openSearchBar();
                break;
            case R.id.actionQr:
                //startActivity(new Intent(MainActivity.this, QRScanActivity.class));
                startActivity(new Intent(MainActivity.this, ConsentScanActivity.class));
                break;
            case R.id.actionFilter:
                openSort();
                break;
            case android.R.id.home:
                if (searchbar.getVisibility() == View.VISIBLE) {
                    setupActionBar();
                } else {
                    finish();
                }
                break;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent result) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            int radius = result.getIntExtra(AppConstants.EXTRA_RADIUS, 0);

            viewModel.setFilterLocation(session.getLocation(), radius);
        }
    }

    @Override
    public void onPersonDetail(Person person) {
        Intent intent = new Intent(MainActivity.this, CreateDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_QR, person.getQrcode());
        startActivity(intent);
    }
}
