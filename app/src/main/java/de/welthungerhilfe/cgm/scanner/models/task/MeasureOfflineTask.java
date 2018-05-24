package de.welthungerhilfe.cgm.scanner.models.task;

import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Measure;

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

public class MeasureOfflineTask {
    public interface OnLoadAll {
        void onLoadAll(List<Measure> measureList);
    }

    public interface OnSearch {
        void onSearch(Measure measure);
    }

    public interface OnComplete {
        void onComplete();
    }

    private OnLoadAll loadAll;

    private OnSearch searchId;

    private OnComplete completeInsert;

    private OnComplete completeDelete;

    private OnComplete completeUpdate;

    public void loadAll(OnLoadAll l) {
        loadAll = l;

        new LoadAllTask().execute();
    }

    public void getById(String id, OnSearch s) {
        searchId = s;

        new GetByIdTask().execute(id);
    }

    public void update(OnComplete c, Measure... measures) {
        completeUpdate = c;

        new UpdateTask().execute(measures);
    }

    public void insertAll(OnComplete c, Measure... measures) {
        completeInsert = c;

        new InsertAllTask().execute(measures);
    }

    public void delete(Measure measure, OnComplete c) {
        completeDelete = c;

        new DeleteTask().execute(measure);
    }


    private class LoadAllTask extends AsyncTask<Void, Void, List<Measure>> {

        @Override
        protected List<Measure> doInBackground(Void... voids) {
            return AppController.getInstance().offlineDb.measureDao().loadAll();
        }

        @Override
        protected void onPostExecute(List<Measure> measureList) {
            if (loadAll != null)
                loadAll.onLoadAll(measureList);
        }
    }

    private class GetByIdTask extends AsyncTask<String, Void, Measure> {

        @Override
        protected Measure doInBackground(String... id) {
            return AppController.getInstance().offlineDb.measureDao().getById(id[0]);
        }

        @Override
        protected void onPostExecute(Measure consent) {
            if (searchId != null)
                searchId.onSearch(consent);
        }
    }

    private class UpdateTask extends AsyncTask<Measure, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Measure... measures) {
            AppController.getInstance().offlineDb.measureDao().update(measures);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeUpdate != null)
                completeUpdate.onComplete();
        }
    }

    private class InsertAllTask extends AsyncTask<Measure, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Measure... measures) {
            AppController.getInstance().offlineDb.measureDao().insertAll(measures);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeInsert != null)
                completeInsert.onComplete();
        }
    }

    private class DeleteTask extends AsyncTask<Measure, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Measure... measure) {
            AppController.getInstance().offlineDb.measureDao().delete(measure[0]);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeDelete != null)
                completeDelete.onComplete();
        }
    }
}
