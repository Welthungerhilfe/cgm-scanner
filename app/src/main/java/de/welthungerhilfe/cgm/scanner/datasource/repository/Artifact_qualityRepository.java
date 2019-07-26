package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;
import android.os.AsyncTask;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Artifact_quality;

public class Artifact_qualityRepository {
    private static Artifact_qualityRepository instance;
    private CgmDatabase database;

    private Artifact_qualityRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static Artifact_qualityRepository getInstance(Context context) {
        if(instance == null) {
            instance = new Artifact_qualityRepository(context);
        }
        return instance;
    }

    public void insertArtifact_quality(Artifact_quality artifact_quality) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                database.artifact_qualityDao().insertArtifact_quality(artifact_quality);
                return null;
            }
        }.execute();
    }
}
