package de.welthungerhilfe.cgm.scanner.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.repositories.PersonListRepository;

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
    private MutableLiveData<List<Person>> personListObservable;

    public PersonListViewModel(Application app) {
        super(app);

        personListObservable = PersonListRepository.getInstance().getPersonList();
    }

    public LiveData<List<Person>> getPersonListObservable() {
        return personListObservable;
    }
}
