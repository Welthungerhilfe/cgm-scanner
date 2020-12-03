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

import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;

public class MeasureResultRepository {
    private static MeasureResultRepository instance;
    private CgmDatabase database;

    private MeasureResultRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static MeasureResultRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MeasureResultRepository(context);
        }
        return instance;
    }

    public void insertMeasureResult (MeasureResult result) {
        database.measureResultDao().insertMeasureResult(result);
    }

    public float getConfidence(String id, String key) {
        return database.measureResultDao().getConfidence(id, key);
    }

    public float getMaxConfidence(String id, String key) {
        return database.measureResultDao().getMaxConfidence(id, key);
    }

    public List<MeasureResult> getAll() {
        return database.measureResultDao().getAll();
    }
}
