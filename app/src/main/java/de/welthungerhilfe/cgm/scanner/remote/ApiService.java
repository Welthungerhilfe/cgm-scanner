package de.welthungerhilfe.cgm.scanner.remote;



import java.util.HashMap;

import de.welthungerhilfe.cgm.scanner.datasource.models.SuccessResponse;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface ApiService {

    @POST("person")
    Observable<SuccessResponse> postPerson(@Header("Authorization") String auth, @Body RequestBody person);

    @POST("measure")
    Observable<SuccessResponse> postMeasure(@Header("Authorization") String auth, @Body RequestBody measure);

    @POST("artifacts")
    Observable<SuccessResponse> postArtifacts(@Header("Authorization") String auth, @Body RequestBody artifacts);

    @Multipart
    @POST("upload")
    Observable<SuccessResponse> uploadFiles(@Header("Authorization") String auth, @Part MultipartBody.Part file, @Part("id") RequestBody id);


}
