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
package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class PersonListViewModel extends AndroidViewModel {
    private PersonRepository repository;

    private SessionManager session;

    private PersonFilter filter;
    private MutableLiveData<PersonFilter> filterLiveData;

    private LiveData<List<Person>> personListLiveData;

    public PersonListViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application);

        session = new SessionManager(application.getApplicationContext());

        filter = new PersonFilter();

        filterLiveData = new MutableLiveData<>();
        filterLiveData.postValue(filter);

        personListLiveData = Transformations.switchMap(filterLiveData, filter ->
            repository.getAvailablePersons(filter)
        );
    }

    public LiveData<PersonFilter> getPersonFilterLiveData() {
        return filterLiveData;
    }

    public LiveData<List<Person>> getAll() {
        return repository.getAll(session.getUserEmail());
    }

    public LiveData<List<Person>> getPersonListLiveData() {
        return personListLiveData;
    }

    public void setCurrentPage(int currentPage) {
        filter.setPage(currentPage);
        filterLiveData.setValue(filter);
    }

    public void setSortType(int sortType) {
        filter.setPage(0);
        filter.setSortType(sortType);
        filterLiveData.setValue(filter);
    }

    public void setFilterOwn() {
        filter.setPage(0);
        filter.setFilterOwn();
        filterLiveData.setValue(filter);
    }

    public void setFilterLocation(Loc fromLOC, int radius) {
        filter.setPage(0);
        filter.setFilterLocation(fromLOC, radius);
        filterLiveData.setValue(filter);
    }

    public void setFilterDate(long fromDate, long toDate) {
        filter.setPage(0);
        filter.setFilterDate(fromDate, toDate);
        filterLiveData.setValue(filter);
    }

    public void setFilterNo() {
        filter.setPage(0);
        filter.setFilterNo();
        filterLiveData.setValue(filter);
    }

    public void setFilterQuery(String query) {
        filter.setPage(0);
        if (query.length() > 0) {
            filter.setFilterQuery(query);
        } else {
            filter.clearFilterQuery();
        }
        filterLiveData.setValue(filter);
    }

    public void setLocation(Loc location) {
        filter.setPage(0);
        filter.setLocation(location);
        filterLiveData.setValue(filter);
    }

    public void clearFilterOwn() {
        filter.setPage(0);
        filter.clearFilterOwn();
        filterLiveData.setValue(filter);
    }

    public void updatePerson(Person person) {
        repository.updatePerson(person);
    }
}
