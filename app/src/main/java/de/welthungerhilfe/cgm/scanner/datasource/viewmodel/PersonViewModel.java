package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class PersonViewModel extends AndroidViewModel {
    private PersonRepository repository;

    public PersonViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<Person>> getPersons() {
        return repository.getPersons();
    }

    public void loadMorePersons() {
        repository.loadMorePersons();
    }

    public LiveData<Person> getPerson(String key) {
        return repository.getPerson(key);
    }

    public void insertPerson(Person person) {
        repository.insertPerson(person);
    }
}
