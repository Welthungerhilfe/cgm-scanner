package de.welthungerhilfe.cgm.scanner.helper.tasks;

import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Person;

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

public class LoadOfflinePersonTask extends AsyncTask<Void, Void, List<Person>> {
    public interface OnOfflineLoaded {
        void onOfflineLoaded(List<Person> personList);
    }

    private OnOfflineLoaded loadListener;

    public LoadOfflinePersonTask(OnOfflineLoaded listener) {
        loadListener = listener;
    }

    @Override
    protected List<Person> doInBackground(Void... voids) {
        List<Person> personList = AppController.getInstance().offlineDb.personDao().loadAll();

        return personList;
    }

    @Override
    protected void onPostExecute(List<Person> personList) {
        if (loadListener != null)
            loadListener.onOfflineLoaded(personList);
    }
}
