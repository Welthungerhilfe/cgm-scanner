package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.PostScanResult;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class PostScanResultrepository {
    private static PostScanResultrepository instance;
    private CgmDatabase database;
    private SessionManager session;

    private PostScanResultrepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
    }

    public static PostScanResultrepository getInstance(Context context) {
        if (instance == null) {
            instance = new PostScanResultrepository(context);
        }
        return instance;
    }

    public void insertPostScanResult(PostScanResult postScanResult) {
        database.postScanResultDao().insertPostScanResult(postScanResult);
    }

    public void updatePostScanResult(PostScanResult postScanResult) {
        database.postScanResultDao().updatePostScanResult(postScanResult);
    }

    public List<PostScanResult> getSyncablePostScanResult(int environment) {
        return database.postScanResultDao().getSyncablePostScanResult(environment);
    }

    public List<String> getScanIdsFromMeasureId(String measureId){
        return database.postScanResultDao().getScanIdsFromMeasureId(measureId);

    }
}
