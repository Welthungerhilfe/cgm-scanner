package de.welthungerhilfe.cgm.scanner.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;

/**
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

public class PersonListViewModel extends AndroidViewModel {
    private OfflineRepository offlineRepo;

    private LiveData<Person> observablePerson;
    private LiveData<List<Person>> observablePersonList;
    private LiveData<List<Consent>> observableConsentList;
    private LiveData<List<Measure>> observableMeasureList;

    public PersonListViewModel(Application app) {
        super(app);

        offlineRepo = OfflineRepository.getInstance();

        observablePersonList = offlineRepo.getPersonList();
    }

    public LiveData<Person> getObservablePerson(String personId) {
        observablePerson = OfflineRepository.getInstance().getPerson(personId);
        return observablePerson;
    }

    public LiveData<Person> getObservablePersonByQr(String qrCode) {
        observablePerson = OfflineRepository.getInstance().getPersonByQr(qrCode);
        return observablePerson;
    }

    public LiveData<List<Person>> getObservablePersonList(String email) {
        observablePersonList = OfflineRepository.getInstance().getOwnPersonList(email);
        return observablePersonList;
    }

    public LiveData<List<Person>> getObservableOwnPersonList() {
        return observablePersonList;
    }

    public LiveData<List<Consent>> getObservableConsentList(Person person) {
        observableConsentList = offlineRepo.getConsentList(person);
        return observableConsentList;
    }

    public LiveData<List<Measure>> getObservableMeasureList(Person person) {
        observableMeasureList = offlineRepo.getMeasureList(person);
        return observableMeasureList;
    }

    public void createPerson(Person person) {
        OfflineRepository.getInstance().createPerson(person);
    }

    public void createMeasure(Measure measure) {
        OfflineRepository.getInstance().createMeasure(measure);
    }
}
