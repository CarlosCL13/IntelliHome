package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.ArrendamientoResponseDto
import com.intelliworks.intellihome.data.model.PropiedadAlquiladaDto
import com.intelliworks.intellihome.data.model.CotizacionArrendamientoDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


interface ArrendamientoApi {
    // 4. Obtener desglose del último arrendamiento de un usuario en una propiedad
    @GET("arrendamientos/desglose/{propiedad_id}/{usuario_id}")
    suspend fun getDesgloseArrendamiento(
        @Path("propiedad_id") propiedadId: Int,
        @Path("usuario_id") usuarioId: Int
    ): Response<com.intelliworks.intellihome.data.model.DesgloseArrendamientoDto>

    // 1. Pagar / Reservar
    @FormUrlEncoded
    @POST("arrendamientos/arrendamiento")
    suspend fun registrarArrendamiento(
        @Field("propiedad_id") propiedadId: Int,
        @Field("inquilino_id") inquilinoId: Int,
        @Field("fecha_inicio") fechaInicio: String,
        @Field("fecha_fin") fechaFin: String,
        @Field("subtotal") subtotal: Double,
        @Field("iva") iva: Double,
        @Field("comision") comision: Double
    ): Response<ArrendamientoResponseDto>

    // 3. Cotizar arrendamiento sin guardar
    @FormUrlEncoded
    @POST("arrendamientos/cotizar")
    suspend fun cotizarArrendamiento(
        @Field("propiedad_id") propiedadId: Int,
        @Field("fecha_inicio") fechaInicio: String,
        @Field("fecha_fin") fechaFin: String
    ): Response<CotizacionArrendamientoDto>

    // 2. Ver mis alquileres (ESTE SE QUEDA AQUÍ porque la URL es /arrendamientos/...)
    @GET("arrendamientos/alquiladas/{user_id}")
    suspend fun obtenerAlquiladasPorUsuario(
        @Path("user_id") userId: Int
    ): Response<List<PropiedadAlquiladaDto>>
}