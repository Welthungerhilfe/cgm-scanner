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
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;

public class ArtifactResultRepository {

    private static ArtifactResultRepository instance;
    private CgmDatabase database;

    private ArtifactResultRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static ArtifactResultRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ArtifactResultRepository(context);
        }
        return instance;
    }

    public void insertArtifactResult(ArtifactResult artifactResult) {
        database.artifactResultDao().insertArtifact_quality(artifactResult);
    }

    public double getAveragePointCount(String measureId, int key) {
        return database.artifactResultDao().getAveragePointCount(measureId, key);
    }

    public double getAveragePointCountForFront(String measureId) {
        return database.artifactResultDao().getAveragePointCountForFront(measureId);
    }

    public double getAveragePointCountForSide(String measureId) {
        return database.artifactResultDao().getAveragePointCountForSide(measureId);
    }

    public double getAveragePointCountForBack(String measureId) {
        return database.artifactResultDao().getAveragePointCountForBack(measureId);
    }

    public int getPointCloudCount(String measureId, int key) {
        return database.artifactResultDao().getPointCloudCount(measureId, key);
    }

    public int getPointCloudCountForFront(String measureId) {
        return database.artifactResultDao().getPointCloudCountForFront(measureId);
    }

    public int getPointCloudCountForSide(String measureId) {
        return database.artifactResultDao().getPointCloudCountForSide(measureId);
    }

    public int getPointCloudCountForBack(String measureId) {
        return database.artifactResultDao().getPointCloudCountForBack(measureId);
    }

    public List<ArtifactResult> getAll() {
        return database.artifactResultDao().getAll();
    }
}