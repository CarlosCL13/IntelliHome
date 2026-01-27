package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.GeocodingResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") key: String,
        @Query("language") language: String = "es"
    ): Response<GeocodingResponseDto>
}
