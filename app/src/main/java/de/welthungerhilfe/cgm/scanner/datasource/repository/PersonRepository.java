package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.content.Context;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataFactory;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonsLoad;
import de.welthungerhilfe.cgm.scanner.utils.PersonFilter;

import static de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase.TABLE_PERSON;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_DATE;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_LOCATION;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_STUNTING;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SORT_WASTING;

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

    public LiveData<List<Person>> getAll(String createdBy) {
        return database.personDao().getAll(createdBy);
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

    public LiveData<List<Person>> getPersonByPage(int page) {
        return database.personDao().getPersonByPage(page * 50);
    }

    public LiveData<List<Person>> getAvailablePersons(PersonFilter filter, int page) {
        String selectClause = "*";
        String whereClause = "deleted=0";
        String orderByClause = "created DESC";

        if (filter.isDate()) {
            whereClause += String.format(" AND created<=%d AND created>=%d ", filter.getToDate(), filter.getFromDate());
        } else if (filter.isOwn()) {
            whereClause += String.format(" AND createdBy=%s ", AppController.getInstance().firebaseAuth.getCurrentUser().getEmail());
        } else if (filter.isLocation()) {
            selectClause += String.format(", ((((acos(sin((%.8f*pi()/180)) * sin((latitude*pi()/180))+cos((%.8f*pi()/180)) * cos((latitude*pi()/180)) * cos(((%.8f-longitude)*pi()/180))))*180/pi())*60*1.1515) * 1.609344) as distance");
            whereClause += String.format(" AND distance<=%d", filter.getRadius());
        } else if (filter.isQuery()) {
            whereClause += String.format(" AND (name LIKE \"%%%s%%\" OR surname LIKE \"%%%s%%\")", filter.getQuery(), filter.getQuery());
        } else {
            whereClause += " AND STRFTIME('%Y-%m-%d', DATETIME(created/1000, 'unixepoch'))=DATE('now')";
        }

        switch (filter.getSortType()) {
            case SORT_DATE:
                break;
            case SORT_LOCATION:
                break;
            case SORT_WASTING:
                break;
            case SORT_STUNTING:
                break;
        }

        String query = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s LIMIT 100", selectClause, TABLE_PERSON, whereClause, orderByClause);
        return database.personDao().getResultPerson(new SimpleSQLiteQuery(query));
    }
}
