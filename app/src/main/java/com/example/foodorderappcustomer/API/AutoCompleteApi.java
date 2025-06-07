package com.example.foodorderappcustomer.API;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AutoCompleteApi {
    Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    AutoCompleteApi apiInterface = new Retrofit.Builder()
            .baseUrl("https://rsapi.goong.io/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AutoCompleteApi.class);
    @GET("Place/AutoComplete")
    Call<PlaceResponse> getPlace(@Query("api_key") String key,
                                 @Query("input") String input,
                                 @Query("limit") int limit,
                                 @Query("location") String locationBias,
                                 @Query("radius") String locationRestriction
    );

}