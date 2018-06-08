package de.welthungerhilfe.cgm.scanner.repositories;

import android.arch.lifecycle.LiveData;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;

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

public class OfflineRepository {
    private static OfflineRepository offlineRepo;

    public synchronized static OfflineRepository getInstance() {
        if (offlineRepo == null) {
            offlineRepo = new OfflineRepository();
        }
        return offlineRepo;
    }

    public void createPerson(Person person) {
        new OfflineTask().savePerson(person);
    }

    public void updatePerson(Person person) {
        new OfflineTask().updatePerson(person);
    }

    public LiveData<Person> getPerson(String personId) {
        return AppController.getInstance().offlineDb.offlineDao().getPerson(personId);
    }

    public LiveData<Person> getPersonByQr(String qrCode) {
        return AppController.getInstance().offlineDb.offlineDao().getPersonByQr(qrCode);
    }

    public LiveData<List<Person>> getPersonList() {
        return AppController.getInstance().offlineDb.offlineDao().getPersons();
    }

    public void createConsent(Consent consent) {
        new OfflineTask().saveConsent(consent);
    }

    public LiveData<List<Consent>> getConsentList(Person person) {
        return AppController.getInstance().offlineDb.offlineDao().findConsents(person.getId());
    }

    public void createMeasure(Measure measure) {
        new OfflineTask().saveMeasure(measure);
    }

    public LiveData<List<Measure>> getMeasureList(Person person) {
        return AppController.getInstance().offlineDb.offlineDao().findMeasures(person.getId());
    }
}
