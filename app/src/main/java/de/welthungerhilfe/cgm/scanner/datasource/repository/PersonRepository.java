package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataFactory;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataSource;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonsLoad;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.PAGE_SIZE;

public class PersonRepository {
    private static PersonRepository instance;

    private CgmDatabase database;

    private ExecutorService executor;
    private Executor bundleExecutor;

    private final int INITIAL_LOAD_KEY = 0;
    private final int INITIAL_LOAD_SIZE = 30;
    private final int PAGE_SIZE = 30;

    private LiveData<PagedList<Person>> pagedListLiveData;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);

        executor = Executors.newSingleThreadExecutor();
        bundleExecutor = Executors.newFixedThreadPool(5);

        PagedList.Config pagingConfig = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(INITIAL_LOAD_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(false)
                .build();

        pagedListLiveData = new LivePagedListBuilder(new PersonDataFactory(database.personDao()), pagingConfig)
                .setFetchExecutor(bundleExecutor)
                .setInitialLoadKey(INITIAL_LOAD_KEY)
                .build();

    }

    public static PersonRepository getInstance(Context context) {
        if(instance == null) {
            instance = new PersonRepository(context);
        }
        return instance;
    }

    public LiveData<List<Person>> getAll() {
        return database.personDao().getAll();
    }

    public LiveData<Person> getPerson(String key) {
        return database.personDao().getPersonByQr(key);
    }

    public void insertPerson(Person person) {
        executor.execute(() -> {
            database.personDao().insertPerson(person);
        });
    }

    public void updatePerson(Person person) {
        executor.execute(() -> {
            database.personDao().updatePerson(person);
        });
    }

    public void getSyncablePerson(OnPersonsLoad listener, long timestamp) {
        executor.execute(() -> {
            List<Person> data = database.personDao().getSyncablePersons(timestamp);
            listener.onPersonsLoaded(data);
        });
    }

    public LiveData<PagedList<Person>> getPagedPerson() {
        return pagedListLiveData;
    }
}
