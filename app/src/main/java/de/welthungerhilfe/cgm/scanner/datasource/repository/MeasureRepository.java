/*
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
package de.welthungerhilfe.cgm.scanner.datasource.repository;

import androidx.lifecycle.LiveData;

import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;

public class MeasureRepository {
    private static MeasureRepository instance;
    private CgmDatabase database;
    private SessionManager session;

    private MeasureRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
    }

    public static MeasureRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MeasureRepository(context);
        }
        return instance;
    }

    public void insertMeasure(Measure measure) {
        database.measureDao().insertMeasure(measure);
    }

    public void updateMeasure(Measure measure) {
        database.measureDao().updateMeasure(measure);
    }

    public List<Measure> getSyncableMeasure(int environment) {
        return database.measureDao().getSyncableMeasure(environment);
    }

    public Measure getMeasureById(String id) {
        return database.measureDao().getMeasureById(id);
    }

    public LiveData<List<Measure>> getPersonMeasures(String personId) {
        return database.measureDao().getPersonMeasures(personId);
    }

    public LiveData<Measure> getPersonLastMeasureLiveData(String personId) {
        return database.measureDao().getLastMeasureLiveData(personId);
    }

    public long getOwnMeasureCount() {
        return database.measureDao().getOwnMeasureCount(session.getUserEmail());
    }

    public List<Measure> getManualMeasures(String personId) {
        return database.measureDao().getManualMeasures(personId);
    }
    public List<Measure> getAllMeasuresByPersonId(String personId) {
        return database.measureDao().getAllMeasuresByPersonId(personId);
    }
    public long getTotalMeasureCount() {
        return database.measureDao().getTotalMeasureCount();
    }

    public List<Measure> getAll() {
        return database.measureDao().getAll();
    }

    public LiveData<List<Measure>> getUploadMeasures() {
        return database.measureDao().getUploadMeasures();
    }

    public Measure getMeasureByMeasureServerKey(String measureServerKey){
        return database.measureDao().getMeasureByMeasureServerKey(measureServerKey);
    }
}
