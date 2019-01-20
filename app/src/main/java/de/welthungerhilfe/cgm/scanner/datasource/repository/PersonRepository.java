package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.PAGE_SIZE;

public class PersonRepository {
    private static PersonRepository instance;

    private CgmDatabase database;

    private MediatorLiveData liveDataMerger;
    private int currentPage = 0;
    private boolean moreAvailable = true;

    private PersonRepository(Context context) {
        database = CgmDatabase.getInstance(context);

        liveDataMerger = new MediatorLiveData<>();
        loadMorePersons();
    }

    public static PersonRepository getInstance(Context context) {
        if(instance == null) {
            instance = new PersonRepository(context);
        }
        return instance;
    }

    public LiveData<List<Person>> getPersons(){
        return liveDataMerger;
    }

    public void loadMorePersons() {
        if (moreAvailable) {
            LiveData<List<Person>> personListLiveData = database.getPersons(currentPage * PAGE_SIZE, PAGE_SIZE);
            liveDataMerger.addSource(personListLiveData, value -> {
                liveDataMerger.setValue(value);

                currentPage ++;
                //liveDataMerger.removeSource(personListLiveData);

                if (((List<Person>)value).size() < PAGE_SIZE) {
                    moreAvailable = false;
                }
            });
        }
    }

    public LiveData<Person> getPerson(String key) {
        return database.getPerson(key);
    }

    public void insertPerson(Person person) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                database.insertPerson(person);
                return true;
            }
        }.execute();
    }
}
