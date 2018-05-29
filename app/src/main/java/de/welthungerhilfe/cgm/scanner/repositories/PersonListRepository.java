package de.welthungerhilfe.cgm.scanner.repositories;

import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
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

public class PersonListRepository {
    private static PersonListRepository personListRepo;

    public synchronized static PersonListRepository getInstance() {
        if (personListRepo == null) {
            personListRepo = new PersonListRepository();
        }
        return personListRepo;
    }

    public MutableLiveData<List<Person>> getPersonList() {
        final MutableLiveData<List<Person>> data = new MutableLiveData<>();

        /*
        AppController.getInstance().firebaseFirestore.collection("persons")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        List<Person> result = new ArrayList<>();

                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Person person = document.toObject(Person.class);
                                result.add(person);
                            }

                            data.setValue(result);
                        }
                    }
                });
        */
        new OfflineTask().getPersonList(personList -> {
            data.setValue(personList);
        });

        return data;
    }
}
