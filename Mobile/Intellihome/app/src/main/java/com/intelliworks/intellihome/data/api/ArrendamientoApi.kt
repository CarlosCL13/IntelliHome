package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.ArrendamientoResponseDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ArrendamientoApi {
    
    // Registra un nuevo arrendamiento/alquiler de propiedad
    @FormUrlEncoded
    @POST("arrendamiento")
    suspend fun registrarArrendamiento(
        @Field("propiedad_id") propiedadId: Int,
        @Field("inquilino_id") inquilinoId: Int,
        @Field("fecha_inicio") fechaInicio: String,
        @Field("fecha_fin") fechaFin: String
    ): Response<ArrendamientoResponseDto>
}
