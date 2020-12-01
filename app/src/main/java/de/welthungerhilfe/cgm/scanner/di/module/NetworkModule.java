package de.welthungerhilfe.cgm.scanner.di.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.datasource.manager.HttpInterceptor;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetworkModule {

    @Provides
    Cache provideHttpCache(Application application) {
        int cacheSize = 10 * 1024 * 1024;
        Cache cache = new Cache(application.getCacheDir(), cacheSize);
        return cache;
    }

    @Provides
    Gson provideGson() {
        GsonBuilder gsonBuilder = new GsonBuilder().setLenient();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        return gsonBuilder.serializeNulls().create();
    }

    @Provides
    OkHttpClient provideOkhttpClient(Cache cache) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        if (BuildConfig.DEBUG) {
            // development build
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            // production build
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        client.connectTimeout(5, TimeUnit.MINUTES);
        client.readTimeout(5, TimeUnit.MINUTES);
        // add logging as last interceptor
//        client.cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(NapsNibbleApp.getContext())));
        client.addInterceptor(logging);
        client.addInterceptor(new HttpInterceptor(getUrl(), client));
//        client.addInterceptor(new AddCookiesInterceptor());
//        client.addInterceptor(new ReceivedCookiesInterceptor());
        client.cache(cache);
        client.retryOnConnectionFailure(true);
        client.connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS));

        return client.build();
    }

    @Provides
    Retrofit provideRetrofit(Gson gson) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .baseUrl(getUrl())
                .client(new OkHttpClient())
                .build();
        return retrofit;
    }

    private String getUrl() {
        if (BuildConfig.DEBUG) {
            // development build
            return AppConstants.testing_url;
        } else {
            // production build
            return "{API_URL}/";
        }
    }
}
