package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

public class PersonViewModel extends AndroidViewModel {
    private PersonRepository personRepository;
    private MeasureRepository measureRepository;

    private String qrCode;

    public PersonViewModel(@NonNull Application application) {
        super(application);

        personRepository = PersonRepository.getInstance(application);
        measureRepository = MeasureRepository.getInstance(application);
    }

    public LiveData<Person> getPerson() {
        return personRepository.getPerson(qrCode);
    }

    public LiveData<List<Measure>> getMeasures(String personId) {
        return measureRepository.getPersonMeasures(personId);
    }

    public void registerPersonQR (String qr) {
        qrCode = qr;
    }

    public String getPersonQR () {
        return qrCode;
    }

    public void savePerson(Person person) {
        personRepository.insertPerson(person);
    }

    public void saveMeasure(Person person, Measure measure) {
        measureRepository.insertMeasure(measure);

        person.setLastLocation(measure.getLocation());
        person.setLastMeasure(measure);
        personRepository.insertPerson(person);
    }
}
