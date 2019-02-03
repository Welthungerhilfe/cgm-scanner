package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.paging.PagedList;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonsLoad;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.PAGE_SIZE;

public class PersonRepository {
    private static PersonRepository instance;

    private CgmDatabase database;

    private MediatorLiveData liveDataMerger;
    private LiveData<PagedList<Person>> personListLiveData;

    private int curPage = -1;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);

        liveDataMerger = new MediatorLiveData<>();
    }

    public static PersonRepository getInstance(Context context) {
        if(instance == null) {
            instance = new PersonRepository(context);
        }
        return instance;
    }

    public LiveData<PagedList<Person>> getPersons() {
        personListLiveData = database.getPersons();
        liveDataMerger.addSource(personListLiveData, value -> {
            liveDataMerger.setValue(value);
        });

        return liveDataMerger;
    }

    public LiveData<List<Person>> getAll() {
        return database.personDao().getAll();
    }

    public LiveData<Person> getPerson(String key) {
        return database.personDao().getPersonByQr(key);
    }

    public LiveData<List<Person>> loadMore() {
        curPage ++;

        return database.personDao().loadMore(curPage * PAGE_SIZE, PAGE_SIZE);
    }

    public void insertPerson(Person person) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                database.personDao().insertPerson(person);
                return true;
            }
        }.execute();
    }

    public void updatePerson(Person person) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                database.personDao().updatePerson(person);
                return null;
            }
        }.execute();
    }

    public void getSyncablePerson(OnPersonsLoad listener, long timestamp) {
        new AsyncTask<Long, Void, List<Person>>() {
            @Override
            protected List<Person> doInBackground(Long... timestamp) {
                return database.personDao().getSyncablePersons(timestamp[0]);
            }

            @Override
            public void onPostExecute(List<Person> data) {
                listener.onPersonsLoaded(data);
            }
        }.execute(timestamp);
    }
}
