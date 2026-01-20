package com.intelliworks.intellihome.data.api

import retrofit2.Response
import retrofit2.http.GET
import com.intelliworks.intellihome.data.model.HobbyDto
import com.intelliworks.intellihome.data.model.TipoCasaDto
import com.intelliworks.intellihome.data.model.PreguntasRecuperacionDto

interface CatalogosApi {

    // http://192.168.1.45:8000/catalogos/hobbies
    @GET("catalogos/hobbies")
    suspend fun getHobbies(): Response<List<HobbyDto>>

    // http://192.168.1.45:8000/catalogos/tipos-casa
    @GET("catalogos/tipos-casa")
    suspend fun getTiposCasa(): Response<List<TipoCasaDto>>

    // http://192.168.1.45:8000/catalogos/preguntas-recuperacion
    @GET("catalogos/preguntas-recuperacion")
    suspend fun getPreguntasRecuperacion(): Response<List<PreguntasRecuperacionDto>>
}
