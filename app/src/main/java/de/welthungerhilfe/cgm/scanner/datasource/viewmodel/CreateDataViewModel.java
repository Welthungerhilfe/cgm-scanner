/*
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
package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.annotation.SuppressLint;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncManualMeasureAdapter;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncingWorkManager;


public class CreateDataViewModel extends ViewModel {

    private LiveData<Person> personLiveData;
    private LiveData<List<Measure>> measuresLiveData;
    private LiveData<Measure> lastMeasureLiveData;

    private MutableLiveData<Integer> tabLiveData = new MutableLiveData<>(0);

    private Context context;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;

    String TAG = CreateDataViewModel.class.getSimpleName();

    public CreateDataViewModel(Context context) {
        this.context = context;

        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
    }

    public LiveData<Integer> getCurrentTab() {
        return tabLiveData;
    }

    private void setActiveTab(int tab) {
        tabLiveData.setValue(tab);
    }

    public LiveData<Person> getPersonLiveData(String qrCode,int environment) {
        personLiveData = personRepository.getPerson(qrCode,environment);
        Log.i(TAG, "this is value of person livedata " + personLiveData.getValue());

        return personLiveData;
    }

    public LiveData<List<Measure>> getMeasuresLiveData() {
        measuresLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonMeasures(person.getId());
            }
        });

        return measuresLiveData;
    }

    public LiveData<Measure> getLastMeasureLiveData() {
        lastMeasureLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonLastMeasureLiveData(person.getId());
            }
        });

        return lastMeasureLiveData;
    }

    public void syncManualMeasurements(String qrCode,int environment) {
        Person person = personRepository.findPersonByQr(qrCode,environment);
        SyncManualMeasureAdapter syncManualMeasureAdapter = SyncManualMeasureAdapter.getInstance(context);
        syncManualMeasureAdapter.getSyncManualMeasure(person);
    }

    @SuppressLint("StaticFieldLeak")
    public void savePerson(Person person) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                personRepository.insertPerson(person);
                startSyncImmediate();
                return null;
            }

            public void onPostExecute(Void result) {
                setActiveTab(1);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void insertMeasure(Measure measure) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                measureRepository.insertMeasure(measure);
                startSyncImmediate();
                return null;
            }

            public void onPostExecute(Void result) {
                setActiveTab(2);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void startSyncImmediate() {
        Context appContext = context.getApplicationContext();
        SyncingWorkManager.startSyncingWithWorkManager(appContext);
    }
}
