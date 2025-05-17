package com.aubank.loanapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val baseUrl = "http://202.157.67.169:8081/api/"
    //private const val baseUrl = "http://202.157.67.169:8080/api/"
    private const val GEOCODER_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/"


    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            .let { client ->
                Retrofit.Builder()
                    .baseUrl(baseUrl) // Replace with your base URL
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            }
    }

    val api by lazy {
        retrofit.create(ApiService::class.java)
    }

    val geoApi: GeocodingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEOCODER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApiService::class.java)
    }

}