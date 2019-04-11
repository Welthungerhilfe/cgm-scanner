package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.content.Context;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
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

    private final int PAGE_SIZE = 30;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);

        executor = Executors.newSingleThreadExecutor();
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
        executor.execute(() -> database.personDao().insertPerson(person));
    }

    public void updatePerson(Person person) {
        executor.execute(() -> database.personDao().updatePerson(person));
    }

    public void getSyncablePerson(OnPersonsLoad listener, long timestamp) {
        executor.execute(() -> {
            List<Person> data = database.personDao().getSyncablePersons(timestamp);
            listener.onPersonsLoaded(data);
        });
    }

    public LiveData<List<Person>> getAvailablePersons(PersonFilter filter) {
        String selectClause = "*";
        String whereClause = "deleted=0";
        String orderByClause = "created DESC";
        String limitClause = String.format(Locale.US, "LIMIT %d OFFSET %d", PAGE_SIZE, filter.getPage() * PAGE_SIZE);

        if (filter.isDate()) {
            whereClause += String.format(Locale.US, " AND created<=%d AND created>=%d ", filter.getToDate(), filter.getFromDate());
        }

        if (filter.isOwn()) {
            whereClause += String.format(" AND createdBy=%s ", Objects.requireNonNull(AppController.getInstance().firebaseAuth.getCurrentUser()).getEmail());
        }

        /*
        if (filter.isLocation()) {
            selectClause += String.format(", (6371*acos(cos(radians(%.8f))*cos(radians(lat))* cos(radians(lng)-radians(%.8f))+sin(radians(%.8f))*sin(radians(lat)))) AS distance", filter.getFromLOC().getLatitude(), filter.getFromLOC().getLongitude(), filter.getFromLOC().getLatitude());
            whereClause += String.format(" AND distance<=%d", filter.getRadius());
        }
        */

        if (filter.isQuery()) {
            whereClause += String.format(" AND (name LIKE \"%%%s%%\" OR surname LIKE \"%%%s%%\")", filter.getQuery(), filter.getQuery());
        } else {
            whereClause += " AND STRFTIME('%Y-%m-%d', DATETIME(created/1000, 'unixepoch'))=DATE('now')";
        }

        switch (filter.getSortType()) {
            case SORT_DATE:
                orderByClause = "created DESC";
                break;
            case SORT_LOCATION:
                break;
            case SORT_WASTING:
                break;
            case SORT_STUNTING:
                break;
        }

        String query = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s %s", selectClause, TABLE_PERSON, whereClause, orderByClause, limitClause);
        return database.personDao().getResultPerson(new SimpleSQLiteQuery(query));
    }
}
