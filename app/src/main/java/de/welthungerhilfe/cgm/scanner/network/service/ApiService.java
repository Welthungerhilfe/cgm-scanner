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

import de.welthungerhilfe.cgm.scanner.datasource.location.india.Root;
import de.welthungerhilfe.cgm.scanner.datasource.models.CompleteScan;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.EstimatesResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.ReceivedResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.RemainingData;
import de.welthungerhilfe.cgm.scanner.datasource.models.ResultsData;
import de.welthungerhilfe.cgm.scanner.datasource.models.Scan;
import de.welthungerhilfe.cgm.scanner.datasource.models.SyncManualMeasureResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.SyncPersonsResponse;
import de.welthungerhilfe.cgm.scanner.datasource.models.WorkflowsResponse;
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
import retrofit2.http.Query;

public interface ApiService {

    @POST("persons")
    Observable<Person> postPerson(@Header("Authorization") String auth, @Body RequestBody person);

    @PUT("persons/{id}")
    Observable<Person> putPerson(@Header("Authorization") String auth, @Body RequestBody person, @Path("id") String id);

    @GET("persons/sync_persons")
    Observable<SyncPersonsResponse> getSyncPersons(@Header("Authorization") String auth, @Query("sync_date") String sync_date, @Query("belongs_to_rst") boolean belongs_to_rst);

    @POST("measurements")
    Observable<Measure> postMeasure(@Header("Authorization") String auth, @Body RequestBody measure);

    @PUT("measurements/{id}")
    Observable<Measure> putMeasure(@Header("Authorization") String auth, @Body RequestBody measure, @Path("id") String id);

    @GET("persons/{person_id}/sync_measurement")
    Observable<SyncManualMeasureResponse> getSyncManualMeasure(@Header("Authorization") String auth, @Path("person_id") String person_id, @Query("sync_date") String sync_date);

    /*@POST("scans")
    Observable<Scan> postScans(@Header("Authorization") String auth, @Body RequestBody scan);*/

    @POST("complete-scan")
    Observable<CompleteScan> postScans(@Header("Authorization") String auth, @Body RequestBody scans);

    @Multipart
    @POST("files")
    Observable<String> uploadFiles(@Header("Authorization") String auth, @Part MultipartBody.Part file, @Part("filename") RequestBody id);

    @POST("persons/{person_id}/consent")
    Observable<Consent> postConsent(@Header("Authorization") String auth, @Body RequestBody personConsent, @Path("person_id") String id);

    @GET("scans/{scan_id}/estimates")
    Observable<EstimatesResponse> getEstimates(@Header("Authorization") String auth, @Path("scan_id") String scanId);

    @GET("scans/estimate")
    Observable<ReceivedResult> getEstimatesAll(@Header("Authorization") String auth, @Query("scan_ids") String scan_ids);

    @GET("workflows")
    Observable<WorkflowsResponse> getWorkflows(@Header("Authorization") String auth);

    @POST("results")
    Observable<ResultsData> postWorkFlowsResult(@Header("Authorization") String auth, @Body RequestBody result);

    @POST("remaining_data")
    Observable<RemainingData> postRemainingData(@Header("Authorization") String auth, @Body RequestBody remainingData);

    @GET("test")
    Observable<String> test(@Header("Authorization") String auth);

    @GET("get_locations_hierarchy?country=INDIA")
    Observable<Root> getLocationIndia(@Header("Authorization") String auth);

}


