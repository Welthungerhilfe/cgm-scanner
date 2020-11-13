package de.welthungerhilfe.cgm.scanner.remote;

import android.database.Observable;

import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.datasource.models.Posts;

interface AppDataSourceInterface {

    public Observable<Posts> getPost(HashMap<String,Object> data);
}
