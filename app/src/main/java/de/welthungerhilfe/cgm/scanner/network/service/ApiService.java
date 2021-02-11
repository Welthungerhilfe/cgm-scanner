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
package de.welthungerhilfe.cgm.scanner.network.service;

import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.EstimatesResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @POST("persons")
    Observable<Person> postPerson(@Header("Authorization") String auth, @Body RequestBody person);

    @PUT("persons/{id}")
    Observable<Person> putPerson(@Header("Authorization") String auth, @Body RequestBody person, @Path("id") String id);


    @POST("measurements")
    Observable<Measure> postMeasure(@Header("Authorization") String auth, @Body RequestBody measure);

    @PUT("measurements/{id}")
    Observable<Measure> putMeasure(@Header("Authorization") String auth, @Body RequestBody measure, @Path("id") String id);

    @POST("scans")
    Observable<Scan> postScans(@Header("Authorization") String auth, @Body RequestBody scan);

    @Multipart
    @POST("files")
    Observable<String> uploadFiles(@Header("Authorization") String auth, @Part MultipartBody.Part file, @Part("filename") RequestBody id);

    @POST("persons/{person_id}/consent")
    Observable<Consent> postConsent(@Header("Authorization") String auth, @Body RequestBody personConsent, @Path("person_id") String id);

    @GET("scans/{scan_id}/estimates")
    Observable<EstimatesResponse> getEstimates(@Header("Authorization") String auth, @Path("scan_id") String scanId);
}

