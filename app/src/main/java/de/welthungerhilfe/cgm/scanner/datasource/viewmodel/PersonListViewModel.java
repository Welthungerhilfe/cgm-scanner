package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataSource;
import de.welthungerhilfe.cgm.scanner.datasource.models.Loc;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.utils.PersonFilter;

public class PersonListViewModel extends AndroidViewModel {
    private PersonRepository repository;

    private PersonFilter filter;
    private MutableLiveData<PersonFilter> filterLiveData;

    private LiveData<List<Person>> personListLiveData;

    public PersonListViewModel(@NonNull Application application) {
        super(application);

        repository = PersonRepository.getInstance(application);

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
        String createdBy = AppController.getInstance().firebaseUser.getEmail();
        return repository.getAll(createdBy);
    }

    public LiveData<List<Person>> getPersonListLiveData() {
        return personListLiveData;
    }

    public void setCurrentPage(int currentPage) {
        filter.setPage(currentPage);
        filterLiveData.setValue(filter);
    }

    public void setSortType(int sortType) {
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
        filter.setFilterQuery(query);
        filterLiveData.setValue(filter);
    }

    public void clearFilterOwn() {
        filter.setPage(0);
        filter.clearFilterOwn();
        filterLiveData.setValue(filter);
    }

    public void clearFilterQuery() {
        filter.setPage(0);
        filter.clearFilterQuery();
        filterLiveData.setValue(filter);
    }

    public void clearFilterDate() {
        filter.setPage(0);
        filter.clearFilterDate();
        filterLiveData.setValue(filter);
    }

    public void clearFilterLocation() {
        filter.setPage(0);
        filter.clearFilterLocation();
        filterLiveData.setValue(filter);
    }
}
