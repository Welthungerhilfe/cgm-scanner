package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import android.content.Context;
import android.location.Location;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.PersonFilter;


public class PersonRepository {
    private static PersonRepository instance;

    private CgmDatabase database;
    private SessionManager session;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
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
        database.personDao().insertPerson(person);
    }

    public void updatePerson(Person person) {
        database.personDao().updatePerson(person);
    }

    public List<Person> getSyncablePerson(long timestamp) {
        return database.personDao().getSyncablePersons(timestamp);
    }

    public LiveData<List<Person>> getAvailablePersons(PersonFilter filter) {
        String selectClause = "*";
        String whereClause = "deleted=0";
        String orderByClause = "created DESC";
        int PAGE_SIZE = 30;
        String limitClause = String.format(Locale.US, "LIMIT %d OFFSET %d", PAGE_SIZE, filter.getPage() * PAGE_SIZE);

        if (filter.isDate()) {
            whereClause += String.format(Locale.US, " AND created<=%d AND created>=%d", filter.getToDate(), filter.getFromDate());
        }

        if (filter.isOwn()) {
            whereClause += String.format(" AND createdBy LIKE '%s'", Objects.requireNonNull(session.getUserEmail()));
        }

        if (filter.isLocation()) {
            double f = 0.1;
            double radius = filter.getRadius() * 1000;
            Location center = new Location("search center");
            center.setLatitude(filter.getFromLOC().getLatitude());
            center.setLongitude(filter.getFromLOC().getLongitude());
            Location east = new Location("search east");
            east.setLatitude(filter.getFromLOC().getLatitude());
            east.setLongitude(filter.getFromLOC().getLongitude() + f);
            Location north = new Location("search north");
            north.setLatitude(filter.getFromLOC().getLatitude() + f);
            north.setLongitude(filter.getFromLOC().getLongitude());
            double fx = radius / center.distanceTo(east);
            double fy = radius / center.distanceTo(north);
            double maxlon = center.getLongitude() + fx * f;
            double maxlat = center.getLatitude() + fy * f;
            double minlon = center.getLongitude() - fx * f;
            double minlat = center.getLatitude() - fy * f;

            whereClause += String.format(Locale.US, " AND longitude>=%f AND longitude<=%f AND latitude>=%f AND latitude<=%f", minlon, maxlon, minlat, maxlat);
        }

        if (filter.isQuery()) {
            whereClause += String.format(" AND (name LIKE \"%%%s%%\" OR surname LIKE \"%%%s%%\")", filter.getQuery(), filter.getQuery());
        }

        switch (filter.getSortType()) {
            case AppConstants.SORT_DATE:
                orderByClause = "created ASC";
                break;
            case AppConstants.SORT_LOCATION:
                Loc loc = filter.getFromLOC();
                if (loc != null) {
                    double lat = filter.getFromLOC().getLatitude();
                    double lon = filter.getFromLOC().getLongitude();
                    orderByClause = "distance ASC";
                    selectClause += String.format(Locale.US, ", ((latitude-%.8f)*(latitude-%.8f)+(longitude-%.8f)*(longitude-%.8f)) AS distance", lat, lat, lon, lon);
                    whereClause += " AND latitude!=0 AND longitude!=0";
                }
                break;
            case AppConstants.SORT_WASTING:
                break;
            case AppConstants.SORT_STUNTING:
                break;
        }

        String query = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s %s", selectClause, CgmDatabase.TABLE_PERSON, whereClause, orderByClause, limitClause);
        return database.personDao().getResultPerson(new SimpleSQLiteQuery(query));
    }

    public long getOwnPersonCount() {
        return database.personDao().getOwnPersonCount(session.getUserEmail());
    }

    public long getTotalPersonCount() {
        return database.personDao().getTotalPersonCount();
    }

    public List<Person> getAll() {
        return database.personDao().getAll();
    }
}
