package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataSource;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;

public class PersonListViewModel extends AndroidViewModel {
    private PersonRepository repository;

    public PersonListViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<Person>> getAll() {
        return repository.getAll();
    }

    public LiveData<PagedList<Person>> getPagedPerson() {
        return repository.getPagedPerson();
    }
}
