package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

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

    public double getArtifactResult(String measureId, int key) {
        return database.artifactResultDao().getArtifactResult(measureId, key);
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
}