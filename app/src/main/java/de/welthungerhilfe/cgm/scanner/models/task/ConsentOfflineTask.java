package de.welthungerhilfe.cgm.scanner.models.task;

import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Consent;
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

public class ConsentOfflineTask {
    public interface OnLoadAll {
        void onLoadAll(List<Consent> consentList);
    }

    public interface OnSearch {
        void onSearch(Consent consent);
    }

    public interface OnComplete {
        void onComplete();
    }

    private OnLoadAll loadAll;

    private OnSearch searchId;

    private OnLoadAll searchQr;

    private OnComplete completeUpdate;

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

    public void getByQrCode(String qr, OnLoadAll s) {
        searchQr = s;

        new GetByQrCodeTask().execute(qr);
    }

    public void update (OnComplete c, Consent... consents) {
        completeUpdate = c;

        new UpdateTask().execute(consents);
    }

    public void insertAll(OnComplete c, Consent... consents) {
        completeInsert = c;

        new InsertAllTask().execute(consents);
    }

    public void delete(Consent consent, OnComplete c) {
        completeDelete = c;

        new DeleteTask().execute(consent);
    }


    private class LoadAllTask extends AsyncTask<Void, Void, List<Consent>> {

        @Override
        protected List<Consent> doInBackground(Void... voids) {
            return AppController.getInstance().offlineDb.consentDao().loadAll();
        }

        @Override
        protected void onPostExecute(List<Consent> consentList) {
            if (loadAll != null)
                loadAll.onLoadAll(consentList);
        }
    }

    private class GetByIdTask extends AsyncTask<String, Void, Consent> {

        @Override
        protected Consent doInBackground(String... id) {
            return AppController.getInstance().offlineDb.consentDao().getById(id[0]);
        }

        @Override
        protected void onPostExecute(Consent consent) {
            if (searchId != null)
                searchId.onSearch(consent);
        }
    }

    private class GetByQrCodeTask extends AsyncTask<String, Void, List<Consent>> {

        @Override
        protected List<Consent> doInBackground(String... qrCode) {
            return AppController.getInstance().offlineDb.consentDao().getByQrCode(qrCode[0]);
        }

        @Override
        protected void onPostExecute(List<Consent> consentList) {
            if (searchQr != null)
                searchQr.onLoadAll(consentList);
        }
    }

    private class UpdateTask extends  AsyncTask<Consent, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Consent... consents) {
            AppController.getInstance().offlineDb.consentDao().update(consents);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeUpdate != null)
                completeUpdate.onComplete();
        }
    }

    private class InsertAllTask extends AsyncTask<Consent, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Consent... consents) {
            AppController.getInstance().offlineDb.consentDao().insertAll(consents);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeInsert != null)
                completeInsert.onComplete();
        }
    }

    private class DeleteTask extends AsyncTask<Consent, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Consent... consent) {
            AppController.getInstance().offlineDb.consentDao().delete(consent[0]);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeDelete != null)
                completeDelete.onComplete();
        }
    }
}
