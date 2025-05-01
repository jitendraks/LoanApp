package com.aubank.loanapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val baseUrl = "http://202.157.67.169:8081/api/"
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
}