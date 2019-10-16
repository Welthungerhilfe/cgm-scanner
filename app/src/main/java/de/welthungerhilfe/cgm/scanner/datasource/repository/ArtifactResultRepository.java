package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.arch.lifecycle.LiveData;
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
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                database.artifactResultDao().insertArtifact_quality(artifactResult);
                return null;
            }
        }.execute();
    }
    public List<Double> getArtifactResult(){
        return database.artifactResultDao().getArtifactResult();
    }

}