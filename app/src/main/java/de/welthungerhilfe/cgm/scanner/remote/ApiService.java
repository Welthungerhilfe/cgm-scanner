package de.welthungerhilfe.cgm.scanner.remote;



import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.datasource.models.Posts;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("posts")
    Observable<Posts> getPost(@Body HashMap<String, Object> data);
}
