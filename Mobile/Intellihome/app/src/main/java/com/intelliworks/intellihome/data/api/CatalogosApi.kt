package com.intelliworks.intellihome.data.api

import retrofit2.Response
import retrofit2.http.GET
import com.intelliworks.intellihome.data.model.HobbyDto
import com.intelliworks.intellihome.data.model.TipoCasaDto
import com.intelliworks.intellihome.data.model.PreguntasRecuperacionDto
import com.intelliworks.intellihome.data.model.AmenidadDto

interface CatalogosApi {

    @GET("catalogos/hobbies")
    suspend fun getHobbies(): Response<List<HobbyDto>>

    @GET("catalogos/tipos-casa")
    suspend fun getTiposCasa(): Response<List<TipoCasaDto>>

    @GET("catalogos/amenidades")
    suspend fun getAmenidades(): Response<List<AmenidadDto>>

    @GET("catalogos/preguntas-recuperacion")
    suspend fun getPreguntasRecuperacion(): Response<List<PreguntasRecuperacionDto>>
}