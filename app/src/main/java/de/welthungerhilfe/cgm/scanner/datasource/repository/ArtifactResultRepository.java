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

    @SuppressLint("StaticFieldLeak")
    public void insertArtifactResult(ArtifactResult artifactResult) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                database.artifactResultDao().insertArtifact_quality(artifactResult);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public double getArtifactResult(String measureId, int key) {
        return database.artifactResultDao().getArtifactResult(measureId, key);
    }

    public double getAveragePointCount(String measureId, int key) {
        return database.artifactResultDao().getAveragePointCount(measureId, key);
    }

    public int getPointCloudCount(String measureId, int key) {
        return database.artifactResultDao().getPointCloudCount(measureId, key);
    }
}