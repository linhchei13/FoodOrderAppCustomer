package com.example.foodorderappcustomer.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
public interface APIService {
//
    Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    APIService apiInterface = new Retrofit.Builder()
            .baseUrl("https://rsapi.goong.io/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(APIService.class);
    @GET("Place/AutoComplete")
    Call<PlaceResponse> getPlace(@Query("api_key") String key,
                                 @Query("input") String input,
                                 @Query("limit") int limit,
                                 @Query("location") String locationBias,
                                 @Query("radius") String locationRestriction
    );

    @GET("DistanceMatrix")

    Call<DistanceResult> getDistance(@Query("api_key") String key,
                                     @Query("origins") String origins,
                                     @Query("destinations") String destinations,
                                     @Query("vehicle") String vehicle
    );
}

