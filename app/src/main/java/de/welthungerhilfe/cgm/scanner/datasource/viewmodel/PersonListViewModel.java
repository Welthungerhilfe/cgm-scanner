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
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.PersonFilter;

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

    public void clearFilterOwn() {
        filter.setPage(0);
        filter.clearFilterOwn();
        filterLiveData.setValue(filter);
    }

    public void updatePerson(Person person) {
        repository.updatePerson(person);
    }
}
