package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
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

    private MutableLiveData<Integer> tabLiveData;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;

    public CreateDataViewModel(@NonNull Application application) {
        super(application);

        personRepository = PersonRepository.getInstance(application);
        measureRepository = MeasureRepository.getInstance(application);

        tabLiveData = new MutableLiveData<>();
        tabLiveData.setValue(0);

        measuresLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonMeasures(person.getId());
            }
        });
    }

    public LiveData<Person> getPerson() {
        return personLiveData;
    }

    public LiveData<Integer> getCurrentTab() {
        return tabLiveData;
    }

    public void setActiveTab(int tab) {
        tabLiveData.setValue(tab);
    }

    public void setPerson(Person person) {
        personLiveData.setValue(person);
    }

    public LiveData<Person> getPersonLiveData(String qrCode) {
        LiveData<Person> liveData = personRepository.getPerson(qrCode);

        measuresLiveData = Transformations.switchMap(liveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonMeasures(person.getId());
            }
        });

        return liveData;
    }

    public LiveData<List<Measure>> getMeasures(String personId) {
        return measureRepository.getPersonMeasures(personId);
    }

    public LiveData<List<Measure>> getMeasuresLiveData() {
        return measuresLiveData;
    }

    public void savePerson(Person person) {
        personRepository.insertPerson(person);

        personLiveData.setValue(person);

        setActiveTab(1);
    }

    public void insertMeasure(Measure measure) {
        measureRepository.insertMeasure(measure);

        setActiveTab(2);
    }
}
