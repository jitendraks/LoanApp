package com.aubank.loanapp.api

import com.aubank.loanapp.data.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {
    @GET("json")
    suspend fun getAddress(
        @Query("latlng") latLng: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}
