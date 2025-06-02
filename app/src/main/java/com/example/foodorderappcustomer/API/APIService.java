package com.example.foodorderappcustomer.API;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
public interface APIService {
//    @GET("/Place/AutoComplete")
//    Call<AutoCompleteResponse> getAutoComplete(
//            @Query("input") String input,
//            @Query("api_key") String apikey
//    );

    // @GET("/Place/Detail")
    // Call<PlaceDetailResponse> getPlaceDetail(
    //         @Query("place_id") String placeId,
    //         @Query("api_key") String apikey
    // );

    // GET INFOR VIETNAM PLACES
    @GET("/api/province")
    Call<ProvincePlacesResponse> getListProvinces();
    @GET("/api/province/district/{id}")
    Call<DistrictPlacesResponse> getDistrictById(@Path("id") String id);
//    @GET("/api/province/ward/{id}")
//    Call<WardPlacesReponse> getWardById(@Path("id") String id);
}

