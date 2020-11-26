package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncAdapter;
import retrofit2.Retrofit;

public class CreateDataViewModel extends ViewModel {

    private LiveData<Person> personLiveData;
    private LiveData<List<Measure>> measuresLiveData;
    private LiveData<Measure> lastMeasureLiveData;

    private MutableLiveData<Integer> tabLiveData = new MutableLiveData<>(0);

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private  SessionManager sessionManager;
    Context context;

   /* public CreateDataViewModel(@NonNull Application application) {
        super(application);

    }*/

   public CreateDataViewModel(Context context, Retrofit retrofit) {

        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context,retrofit);
        sessionManager = new SessionManager(context);
        this.context = context;



   }

    public LiveData<Integer> getCurrentTab() {
        return tabLiveData;
    }

    private void setActiveTab(int tab) {
        tabLiveData.setValue(tab);
    }

    public LiveData<Person> getPersonLiveData(String qrCode) {
        personLiveData = personRepository.getPerson(qrCode);

        return personLiveData;
    }

    public LiveData<List<Measure>> getMeasuresLiveData() {
        measuresLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonMeasures(person.getId());
            }
        });

        return measuresLiveData;
    }

    public LiveData<List<Measure>> getManualMeasuresLiveData() {
        measuresLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getManualMeasuresLiveData(person.getId());
            }
        });

        return measuresLiveData;
    }

    public LiveData<Measure> getLastMeasureLiveData() {
        lastMeasureLiveData = Transformations.switchMap(personLiveData, person -> {
            if (person == null)
                return null;
            else {
                return measureRepository.getPersonLastMeasureLiveData(person.getId());
            }
        });

        return lastMeasureLiveData;
    }

    @SuppressLint("StaticFieldLeak")
    public void savePerson(Person person) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                personRepository.insertPerson(person);

                return null;
            }

            public void onPostExecute(Void result) {

                setActiveTab(1);

      //         startPerodicSync();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    public void insertMeasure(Measure measure) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                measureRepository.insertMeasure(measure);
                return null;
            }

            public void onPostExecute(Void result) {
                setActiveTab(2);
          //     startPerodicSync();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void startPerodicSync()
    {
        final Account accountData = new Account(sessionManager.getUserEmail(), AppConstants.ACCOUNT_TYPE);

        SyncAdapter.startPeriodicSync(accountData,context.getApplicationContext());
    }
}
