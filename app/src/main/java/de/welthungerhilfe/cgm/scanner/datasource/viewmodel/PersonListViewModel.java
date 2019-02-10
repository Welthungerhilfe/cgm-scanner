package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataSource;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;

public class PersonListViewModel extends AndroidViewModel {
    private PersonRepository repository;

    private MutableLiveData<List<Person>> personList;
    private MediatorLiveData<List<Person>> liveDataMerger;

    public PersonListViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application);

        personList = new MutableLiveData<>();
        liveDataMerger = new MediatorLiveData<>();
    }

    public LiveData<List<Person>> getAll() {
        String createdBy = AppController.getInstance().firebaseUser.getEmail();
        return repository.getAll(createdBy);
    }

    public LiveData<PagedList<Person>> getPagedPerson() {
        return repository.getPagedPerson();
    }

    public LiveData<List<Person>> loadMore(int page) {
        /*
        LiveData<List<Person>> pList = repository.getPersonByPage(page);
        liveDataMerger.addSource(pList, list->{
            liveDataMerger.setValue(list);
        });

        return liveDataMerger;
        */
        return repository.getPersonByPage(page);
    }

    public MutableLiveData<List<Person>> getPersonList() {
        return personList;
    }
}
