package de.welthungerhilfe.cgm.scanner.models.tasks;

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

public class OfflineTask {
    public interface OnLoad<T> {
        void onLoad(List<T> listData);
    }

    public void getPersonList(OnLoad<Person> listener) {
        new LoadPersonTask(listener).execute();
    }

    private static class LoadPersonTask extends AsyncTask<Void, Void, List<Person>> {
        private OnLoad<Person> listener;

        public LoadPersonTask(OnLoad<Person> l) {
            listener = l;
        }

        @Override
        protected List<Person> doInBackground(Void... voids) {
            return AppController.getInstance().offlineDb.offlineDao().getPersons();
        }

        @Override
        protected void onPostExecute(List<Person> personList) {
            listener.onLoad(personList);
        }
    }
}
