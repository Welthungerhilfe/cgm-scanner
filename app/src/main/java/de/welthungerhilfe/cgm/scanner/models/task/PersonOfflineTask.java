package de.welthungerhilfe.cgm.scanner.models.task;

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

public class PersonOfflineTask {
    public interface OnLoadAll {
        void onLoadAll(List<Person> personList);
    }

    public interface OnSearch {
        void onSearch(Person person);
    }

    public interface OnComplete {
        void onComplete();
    }

    private OnLoadAll loadAll;

    private OnSearch searchId;

    private OnSearch searchQr;

    private OnComplete completeInsert;

    private OnComplete completeDelete;

    public void loadAll(OnLoadAll l) {
        loadAll = l;

        new LoadAllTask().execute();
    }

    public void getById(String id, OnSearch s) {
        searchId = s;

        new GetByIdTask().execute(id);
    }

    public void getByQrCode(String qr, OnSearch s) {
        searchQr = s;

        new GetByQrCodeTask().execute(qr);
    }

    public void insertAll(OnComplete c, Person... persons) {
        completeInsert = c;

        new InsertAllTask().execute(persons);
    }

    public void delete(Person person, OnComplete c) {
        completeDelete = c;

        new DeleteTask().execute(person);
    }


    private class LoadAllTask extends AsyncTask<Void, Void, List<Person>> {

        @Override
        protected List<Person> doInBackground(Void... voids) {
            return AppController.getInstance().offlineDb.personDao().loadAll();
        }

        @Override
        protected void onPostExecute(List<Person> personList) {
            if (loadAll != null)
                loadAll.onLoadAll(personList);
        }
    }

    private class GetByIdTask extends AsyncTask<String, Void, Person> {

        @Override
        protected Person doInBackground(String... id) {
            return AppController.getInstance().offlineDb.personDao().getById(id[0]);
        }

        @Override
        protected void onPostExecute(Person person) {
            if (searchId != null)
                searchId.onSearch(person);
        }
    }

    private class GetByQrCodeTask extends AsyncTask<String, Void, Person> {

        @Override
        protected Person doInBackground(String... qrCode) {
            return AppController.getInstance().offlineDb.personDao().getByQrCode(qrCode[0]);
        }

        @Override
        protected void onPostExecute(Person person) {
            if (searchQr != null)
                searchQr.onSearch(person);
        }
    }

    private class InsertAllTask extends AsyncTask<Person, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Person... persons) {
            AppController.getInstance().offlineDb.personDao().insertAll(persons);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeInsert != null)
                completeInsert.onComplete();
        }
    }

    private class DeleteTask extends AsyncTask<Person, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Person... person) {
            AppController.getInstance().offlineDb.personDao().delete(person[0]);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeDelete != null)
                completeDelete.onComplete();
        }
    }
}
