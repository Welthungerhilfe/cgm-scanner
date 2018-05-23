package de.welthungerhilfe.cgm.scanner.activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerDataAdapter;
import de.welthungerhilfe.cgm.scanner.helper.tasks.LoadOfflinePersonTask;
import de.welthungerhilfe.cgm.scanner.helper.tasks.PersonOfflineTask;
import de.welthungerhilfe.cgm.scanner.models.Person;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class OfflineDataActivity extends BaseActivity implements RecyclerDataAdapter.OnPersonDetail, PersonOfflineTask.OnLoadAll {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.recyclerData)
    RecyclerView recyclerData;
    RecyclerDataAdapter adapterData;
    @BindView(R.id.txtNoPerson)
    TextView txtNoPerson;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_offline);

        ButterKnife.bind(this);

        setupToolbar();

        loadData();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Offline Data");
    }

    private void loadData() {
        adapterData = new RecyclerDataAdapter(this, new ArrayList<Person>());
        adapterData.setPersonDetailListener(this);
        recyclerData.setAdapter(adapterData);
        recyclerData.setLayoutManager(new LinearLayoutManager(OfflineDataActivity.this));

        new PersonOfflineTask().loadAll(this);
    }

    @Override
    public void onPersonDetail(Person person) {
        int i = 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onLoadAll(List<Person> personList) {
        if (personList.size() > 0) {
            for (int i = 0; i < personList.size(); i++) {
                adapterData.addPerson(personList.get(i));
            }

            recyclerData.setVisibility(View.VISIBLE);
            txtNoPerson.setVisibility(View.GONE);
        } else {
            recyclerData.setVisibility(View.GONE);
            txtNoPerson.setVisibility(View.VISIBLE);
        }

        refreshLayout.setRefreshing(false);
    }
}
