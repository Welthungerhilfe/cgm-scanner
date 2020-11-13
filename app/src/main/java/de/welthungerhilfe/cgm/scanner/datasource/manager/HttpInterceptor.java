package de.welthungerhilfe.cgm.scanner.datasource.manager;

import androidx.annotation.NonNull;


import java.io.IOException;
import java.net.SocketTimeoutException;

import javax.inject.Inject;

import de.welthungerhilfe.cgm.scanner.AppController;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class HttpInterceptor implements Interceptor {
    private static final String TAG = "HttpInterceptor";

    private final OkHttpClient.Builder okHttpClient;
    private String mBaseUrl;

    private boolean isAuthNull;
/*
    @Inject
    AuthManager authManager;*/
    private long connectTimeoutMillis = 60000;
    private long readTimeoutMillis = 30000;


    public HttpInterceptor(String mBaseUrl, OkHttpClient.Builder okHttpClient) {
        this.okHttpClient = okHttpClient;
        this.mBaseUrl = mBaseUrl;
        AppController.getInstance().androidInjector().inject(this);
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = null;
        try {
            Request request = chain.request();
            //Build new request
            Request.Builder builder = request.newBuilder();
            builder.header("Accept", "application/json"); //if necessary, say to consume JSON
            request = builder.build(); //overwrite old request
            response = chain.proceed(request); //perform request, here original request will be executed
            if (response.code() == 401 || response.code() == 406) { //if unauthorized



            } else if (response.code() == 410) {
//                logout();
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();

        }
        return response;
    }




}
