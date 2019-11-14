package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class CreateDataViewModel extends AndroidViewModel {

    private LiveData<Person> personLiveData;
    private LiveData<List<Measure>> measuresLiveData;
    private LiveData<Measure> lastMeasureLiveData;

    private MutableLiveData<Integer> tabLiveData;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;

    public CreateDataViewModel(@NonNull Application application) {
        super(application);

        personRepository = PersonRepository.getInstance(application);
        measureRepository = MeasureRepository.getInstance(application);

        tabLiveData = new MutableLiveData<>();
        tabLiveData.setValue(0);
    }

    public LiveData<Integer> getCurrentTab() {
        return tabLiveData;
    }

    private void setActiveTab(int tab) {
        tabLiveData.setValue(tab);
    }

    public LiveData<Person> getPersonLiveData(String qrCode) {
        personLiveData = personRepository.getPerson(qrCode);

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

    public LiveData<List<Measure>> getManualMeasuresLiveData() {
        measuresLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getManualMeasuresLiveData(person.getId());
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

    public void savePerson(Person person) {
        personRepository.insertPerson(person);

        setActiveTab(1);
    }

    @SuppressLint("StaticFieldLeak")
    public void insertMeasure(Measure measure) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                measureRepository.insertMeasure(measure);
                return null;
            }

            public void onPostExecute(Void result) {
                setActiveTab(2);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
