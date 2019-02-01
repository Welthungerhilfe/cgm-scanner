package de.welthungerhilfe.cgm.scanner.datasource.datasource.person;

import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class PersonDataSource extends PageKeyedDataSource<Integer, Person> {
    private final String TAG = PersonDataSource.class.getSimpleName();
    private final PersonDao dao;

    public PersonDataSource(PersonDao dao) {
        this.dao = dao;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, Person> callback) {
        Log.e(TAG, "Loading Initial Persons, Count " + params.requestedLoadSize);
        List<Person> personList = dao.getPersons();

        if(personList.size() != 0) {
            callback.onResult(personList, 0, 1);
        }
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Person> callback) {
        int a = 0;
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Person> callback) {
        int b = 0;
    }
}
