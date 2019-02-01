package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class PersonViewModel extends AndroidViewModel {
    private PersonRepository repository;

    public static DiffUtil.ItemCallback<Person> DIFF_CALLBACK = new DiffUtil.ItemCallback<Person>() {

        @Override
        public boolean areItemsTheSame(Person oldItem, Person newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(Person oldItem, Person newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public PersonViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<PagedList<Person>> getPersons() {
        return repository.getPersons();
    }

    public LiveData<List<Person>> getAll() {
        return repository.getAll();
    }

    public LiveData<Person> getPerson(String key) {
        return repository.getPerson(key);
    }

    public LiveData<List<Person>> loadMore() {
        return repository.loadMore();
    }
}
