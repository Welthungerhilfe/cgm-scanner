package de.welthungerhilfe.cgm.scanner.models.tasks;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.FileLog;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

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
    public interface OnLoadPerson {
        void onPersonLoaded(List<Person> list);
    }

    public interface OnLoadMeasure {
        void onMeasureLoaded(List<Measure> list);
    }

    public interface OnLoadLastMeasure {
        void onLastMeasureLoaded(Measure measure);
    }

    public interface OnLoadFileLog {
        void onLoadFileLog(FileLog log);
    }

    public interface OnLoadFileLogs {
        void onLoadFileLogs(List<FileLog> logs);
    }

    public void getSyncablePerson(OnLoadPerson l, long timestamp) {
        new LoadPersonTask(l).execute(timestamp);
    }

    public void getSyncableMeasure(OnLoadMeasure l, long timestamp) {
        new LoadMeasureTask(l).execute(timestamp);
    }

    public void savePerson(Person person) {
        new SaveTask().execute(person);
    }

    public void updatePerson(Person person) {
        person.setTimestamp(Utils.getUniversalTimestamp());
        new UpdateTask().execute(person);
    }

    public void saveConsent(Consent consent) {
        new SaveTask().execute(consent);
    }

    public void updateConsent(Consent consent) {
        consent.setTimestamp(Utils.getUniversalTimestamp());
        new UpdateTask().execute(consent);
    }

    public void saveMeasure(Measure measure) {
        new SaveTask().execute(measure);
    }

    public void updateMeasure(Measure measure) {
        measure.setTimestamp(Utils.getUniversalTimestamp());
        new UpdateTask().execute(measure);
    }

    public void getLastMeasure(Person person, OnLoadLastMeasure listener) {
        new LoadLastMeasureTask(listener).execute(person);
    }

    public void saveFileLog(FileLog log) {
        new SaveTask().execute(log);
    }

    public void updateFileLog(FileLog log) {
        new UpdateTask().execute(log);
    }

    public void getFileLog(String param, OnLoadFileLog listener) {
        new LoadFileLogTask(listener).execute(param);
    }

    public void getSyncableFileLog(OnLoadFileLogs listener) {
        new LoadFileLogsTask(listener).execute();
    }

    public void deleteRecords(long timestamp) {
        new DeleteRecordsTask().execute(timestamp);
    }

    private static class LoadPersonTask extends AsyncTask<Long, Void, List<Person>> {
        OnLoadPerson listener;

        LoadPersonTask(OnLoadPerson listener) {
            this.listener = listener;
        }

        @Override
        protected List<Person> doInBackground(Long... timestamp) {
            return AppController.getInstance().offlineDb.offlineDao().getSyncablePersons(timestamp[0]);
        }

        @Override
        public void onPostExecute(List<Person> data) {
            listener.onPersonLoaded(data);
        }
    }

    private static class LoadMeasureTask extends AsyncTask<Long, Void, List<Measure>> {
        OnLoadMeasure listener;

        LoadMeasureTask(OnLoadMeasure listener) {
            this.listener = listener;
        }

        @Override
        protected List<Measure> doInBackground(Long... timestamp) {
            return AppController.getInstance().offlineDb.offlineDao().getSyncableMeasure(timestamp[0]);
        }

        @Override
        public void onPostExecute(List<Measure> data) {
            listener.onMeasureLoaded(data);
        }
    }

    private static class LoadFileLogsTask extends AsyncTask<Void, Void, List<FileLog>> {
        OnLoadFileLogs listener;

        LoadFileLogsTask(OnLoadFileLogs listener) {
            this.listener = listener;
        }


        @Override
        protected List<FileLog> doInBackground(Void... voids) {
            return AppController.getInstance().offlineDb.offlineDao().getSyncableFileLogs();
        }

        @Override
        public void onPostExecute(List<FileLog> data) {
            listener.onLoadFileLogs(data);
        }
    }

    private static class SaveTask extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... objects) {
            if (objects[0].getClass() == Person.class) {
                AppController.getInstance().offlineDb.offlineDao().savePerson((Person) objects[0]);
            } else if (objects[0].getClass() == Consent.class) {
                AppController.getInstance().offlineDb.offlineDao().saveConsent((Consent) objects[0]);
            } else if (objects[0].getClass() == Measure.class) {
                AppController.getInstance().offlineDb.offlineDao().saveMeasure((Measure) objects[0]);
            } else if (objects[0].getClass() == FileLog.class) {
                AppController.getInstance().offlineDb.offlineDao().saveFileLog((FileLog) objects[0]);
            }

            return true;
        }
    }

    private static class UpdateTask extends AsyncTask<Object, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Object... objects) {
            if (objects[0].getClass() == Person.class) {
                AppController.getInstance().offlineDb.offlineDao().savePerson((Person) objects[0]);
            } else if (objects[0].getClass() == Consent.class) {
                //AppController.getInstance().offlineDb.offlineDao().updateConsent((Consent) objects[0]);
            } else if (objects[0].getClass() == Measure.class) {
                AppController.getInstance().offlineDb.offlineDao().saveMeasure((Measure) objects[0]);
            } else if (objects[0].getClass() == FileLog.class) {
                AppController.getInstance().offlineDb.offlineDao().updateFileLog((FileLog) objects[0]);
            }

            return true;
        }
    }

    private static class CheckQrTask extends AsyncTask<String, Void, LiveData<Person>> {

        @Override
        protected LiveData<Person> doInBackground(String... qrCode) {
            return AppController.getInstance().offlineDb.offlineDao().getPersonByQr(qrCode[0]);
        }
    }

    private static class LoadLastMeasureTask extends AsyncTask<Person, Void, Measure> {
        OnLoadLastMeasure listener;

        public LoadLastMeasureTask(OnLoadLastMeasure listener) {
            this.listener = listener;
        }

        @Override
        protected Measure doInBackground(Person... people) {
            return AppController.getInstance().offlineDb.offlineDao().getLastMeasure(people[0].getId());
        }

        @Override
        public void onPostExecute(Measure data) {
            listener.onLastMeasureLoaded(data);
        }
    }

    private static class LoadFileLogTask extends AsyncTask<String, Void, FileLog> {
        OnLoadFileLog listener;

        public LoadFileLogTask(OnLoadFileLog listener) {
            this.listener = listener;
        }

        @Override
        protected FileLog doInBackground(String... artefactIds) {
            return AppController.getInstance().offlineDb.offlineDao().getFileLog(artefactIds[0]);
        }

        @Override
        public void onPostExecute(FileLog data) {
            listener.onLoadFileLog(data);
        }
    }

    private static class DeleteRecordsTask extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... timestamps) {
            AppController.getInstance().offlineDb.offlineDao().deletePersonGarbage();
            AppController.getInstance().offlineDb.offlineDao().deleteMeasureGarbage(timestamps[0]);
            return null;
        }
    }
}
