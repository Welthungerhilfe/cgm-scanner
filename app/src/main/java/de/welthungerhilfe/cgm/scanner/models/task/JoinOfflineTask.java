package de.welthungerhilfe.cgm.scanner.models.task;

import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.PersonConsent;
import de.welthungerhilfe.cgm.scanner.models.PersonLoc;
import de.welthungerhilfe.cgm.scanner.models.PersonMeasure;

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

public class JoinOfflineTask {
    public interface OnLoadAll {
        void onLoadAll(List<Consent> consentList);
    }

    public interface OnSearch {
        void onSearch(Consent consent);
    }

    public interface OnComplete {
        void onComplete();
    }

    private OnComplete completeJoinPersonConsent;

    private OnComplete completeJoinPersonMeasure;

    private OnComplete completeJoinPersonLoc;

    public void joinPersonConsent(PersonConsent personConsent, OnComplete l) {
        completeJoinPersonConsent = l;

        new JoinPersonConsentTask().execute(personConsent);
    }

    public void joinPersonMeasure(PersonMeasure personMeasure, OnComplete l) {
        completeJoinPersonMeasure = l;

        new JoinPersonMeasureTask().execute(personMeasure);
    }

    public void joinPersonLoc(PersonLoc personLoc, OnComplete l) {
        completeJoinPersonLoc = l;

        new JoinPersonLocTask().execute(personLoc);
    }

    private class JoinPersonConsentTask extends AsyncTask<PersonConsent, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PersonConsent... personConsents) {
            AppController.getInstance().offlineDb.personConsentDao().insert(personConsents);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeJoinPersonConsent != null)
                completeJoinPersonConsent.onComplete();
        }
    }

    private class JoinPersonMeasureTask extends AsyncTask<PersonMeasure, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PersonMeasure... personMeasures) {
            AppController.getInstance().offlineDb.personMeasureDao().insert(personMeasures);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeJoinPersonMeasure != null)
                completeJoinPersonMeasure.onComplete();
        }
    }

    private class JoinPersonLocTask extends AsyncTask<PersonLoc, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PersonLoc... personLocs) {
            AppController.getInstance().offlineDb.personLocDao().insert(personLocs);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (completeJoinPersonLoc != null)
                completeJoinPersonLoc.onComplete();
        }
    }
}
