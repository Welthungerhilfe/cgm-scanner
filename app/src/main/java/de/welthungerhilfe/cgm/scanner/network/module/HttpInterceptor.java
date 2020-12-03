/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.module;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.SocketTimeoutException;

import de.welthungerhilfe.cgm.scanner.AppController;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class HttpInterceptor implements Interceptor {

    private final OkHttpClient.Builder okHttpClient;
    private String mBaseUrl;


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
                //logout();
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();

        }
        return response;
    }
}
