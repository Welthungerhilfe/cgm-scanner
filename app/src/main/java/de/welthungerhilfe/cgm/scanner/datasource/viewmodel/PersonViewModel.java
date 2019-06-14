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

public class PersonViewModel extends AndroidViewModel {

    private MutableLiveData<Person> personLiveData;
    private LiveData<List<Measure>> measuresLiveData;

    public PersonViewModel(@NonNull Application application) {
        super(application);

        personLiveData = new MutableLiveData<>();
        measuresLiveData = Transformations.switchMap(personLiveData, person -> AppController.getInstance().measureRepository.getPersonMeasures(person.getId()));
    }

    public LiveData<Person> getPerson() {
        return personLiveData;
    }

    public void setPerson(Person person) {
        personLiveData.setValue(person);
    }

    public LiveData<List<Measure>> getMeasures(String personId) {
        return AppController.getInstance().measureRepository.getPersonMeasures(personId);
    }

    public LiveData<List<Measure>> getMeasuresLiveData() {
        return measuresLiveData;
    }

    public void savePerson(Person person) {
        AppController.getInstance().personRepository.insertPerson(person);

        personLiveData.setValue(person);
    }
}
