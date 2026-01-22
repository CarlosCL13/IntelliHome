package com.intelliworks.intellihome.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object RetrofitInstance {
    // Cambia esta URL por la IP y puerto de tu backend cuando lo necesites
    private const val BASE_URL = "http://192.168.100.7:8000/" // Ejemplo local

    private val client = OkHttpClient.Builder().build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
