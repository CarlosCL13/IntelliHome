package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.EstadoLedsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CasaApi {
    
    /**
     * Cambia el estado de un LED específico o todos los LEDs
     * El backend devuelve el estado actualizado de todos los LEDs
     */
    @POST("casa/led")
    suspend fun cambiarLed(
        @Query("propiedad_id") propiedadId: Int,
        @Query("habitacion") habitacion: String? = null,
        @Query("accion") accion: String
    ): Response<EstadoLedsDto>
    
    /**
     * Obtiene el estado actual de todos los LEDs
     */
    @GET("casa/estado_leds")
    suspend fun obtenerEstadoLeds(
        @Query("propiedad_id") propiedadId: Int
    ): Response<EstadoLedsDto>
    /**
     * Controla la puerta del garaje
     * URL: POST /casa/garaje?propiedad_id=X&accion=abrir
     */
    @POST("casa/led")
    suspend fun controlarGaraje(
        @Query("propiedad_id") propiedadId: Int,
        @Query("accion") accion: String
    ): Response<Void>
}
