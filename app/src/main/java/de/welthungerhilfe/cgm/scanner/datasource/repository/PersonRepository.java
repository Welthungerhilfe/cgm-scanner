/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonFilter;

public class PersonRepository {
    private static PersonRepository instance;

    private CgmDatabase database;
    private SessionManager session;
    private boolean updated;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
        updated = true;
    }

    public static PersonRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PersonRepository(context);
        }
        return instance;
    }

    public LiveData<List<Person>> getAll(String createdBy) {
        return database.personDao().getAll(createdBy);
    }

    public LiveData<Person> getPerson(String key, int environment) {
        return database.personDao().getPersonByQr(key,environment);
    }

    public Person getPersonById(String id) {
        return database.personDao().getPersonById(id);
    }

    public Person findPersonByQr(String qrCode, int environment)
    {
        return database.personDao().findPersonByQr(qrCode,environment);

    }

   public Person findPersonByQrinApp(String qrCode){
        return database.personDao().findPersonByQrinApp(qrCode);
   }

    public void insertPerson(Person person) {
        database.personDao().insertPerson(person);
        setUpdated(true);
    }

    public void updatePerson(Person person) {
        database.personDao().updatePerson(person);
        setUpdated(true);
    }

    public List<Person> getSyncablePerson(int environment) {
        return database.personDao().getSyncablePersons(environment);
    }

    public List<Person> getPersonStat(long currentDate, boolean belongs_to_rst){
        return database.personDao().getPersonStat(currentDate,belongs_to_rst);
    }

    public LiveData<List<Person>> getAvailablePersons(PersonFilter filter,int environment) {
        String selectClause = "*";
        String whereClause = "p.deleted=0 AND p.environment="+environment;
        String orderByClause = "p.created DESC";
        int PAGE_SIZE = 30;
        String limitClause = String.format(Locale.US, "LIMIT %d OFFSET %d", PAGE_SIZE, filter.getPage() * PAGE_SIZE);

        if(session.getSelectedMode() == AppConstants.CGM_MODE){
            whereClause += String.format(Locale.US, " AND p.belongs_to_rst=0");
        }
        else {
            whereClause += String.format(Locale.US, " AND p.belongs_to_rst=1");

        }
        if (filter.isDate()) {
            whereClause += String.format(Locale.US, " AND p.created<=%d AND p.created>=%d", filter.getToDate(), filter.getFromDate());
        }

        if (filter.isOwn()) {
            whereClause += String.format(" AND p.createdBy LIKE '%s'", Objects.requireNonNull(session.getUserEmail()));
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

            whereClause += String.format(Locale.US, " AND p.longitude>=%f AND p.longitude<=%f AND p.latitude>=%f AND p.latitude<=%f", minlon, maxlon, minlat, maxlat);
        }

        if (filter.isQuery()) {
            whereClause += String.format(" AND (p.name LIKE \"%%%s%%\" OR p.qrcode LIKE \"%%%s%%\" OR p.surname LIKE \"%%%s%%\")", filter.getQuery(), filter.getQuery(), filter.getQuery());
        }

        switch (filter.getSortType()) {
            case AppConstants.SORT_DATE:
                orderByClause = "p.last_updated DESC";
                break;
            case AppConstants.SORT_LOCATION:
                Loc loc = filter.getFromLOC();
                if (loc != null) {
                    double lat = filter.getFromLOC().getLatitude();
                    double lon = filter.getFromLOC().getLongitude();
                    orderByClause = "distance ASC";
                    selectClause += String.format(Locale.US, ", ((p.latitude-%.8f)*(p.latitude-%.8f)+(p.longitude-%.8f)*(p.longitude-%.8f)) AS distance", lat, lat, lon, lon);
                }
                break;
            case AppConstants.SORT_WASTING:
                orderByClause = "w/max(1,h) ASC"; //max prevents 0 division
                selectClause += String.format(Locale.US, ", (SELECT m.weight FROM %s m WHERE m.personId=p.id ORDER BY m.timestamp DESC LIMIT 1) AS w", CgmDatabase.TABLE_MEASURE);
                selectClause += String.format(Locale.US, ", (SELECT m.height FROM %s m WHERE m.personId=p.id ORDER BY m.timestamp DESC LIMIT 1) AS h", CgmDatabase.TABLE_MEASURE);
                break;
            case AppConstants.SORT_STUNTING:
                orderByClause = "h/max(1,age) ASC"; //max prevents 0 division
                selectClause += String.format(Locale.US, ", (%d - birthday) / 1000 / 60 / 60 / 24 / 365 AS age", System.currentTimeMillis());
                selectClause += String.format(Locale.US, ", (SELECT m.height FROM %s m WHERE m.personId=p.id ORDER BY m.timestamp DESC LIMIT 1) AS h", CgmDatabase.TABLE_MEASURE);
                break;
        }


        String query = String.format("SELECT %s FROM %s p WHERE %s ORDER BY %s %s", selectClause, CgmDatabase.TABLE_PERSON, whereClause, orderByClause, limitClause);
        Log.i("PersonRepo","this is query "+query);
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

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
