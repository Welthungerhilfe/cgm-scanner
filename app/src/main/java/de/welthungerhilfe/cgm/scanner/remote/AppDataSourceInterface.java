package de.welthungerhilfe.cgm.scanner.remote;

import android.database.Observable;

import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.datasource.models.SuccessResponse;

interface AppDataSourceInterface {

    public Observable<SuccessResponse> getPost(HashMap<String,Object> data);
}
