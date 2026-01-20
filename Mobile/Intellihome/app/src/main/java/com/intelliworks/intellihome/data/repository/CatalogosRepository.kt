package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.model.HobbyDto
import com.intelliworks.intellihome.data.model.PreguntasRecuperacionDto
import com.intelliworks.intellihome.data.model.TipoCasaDto
import retrofit2.Response

class CatalogosRepository(private val api: CatalogosApi) {
    suspend fun getHobbies(): Response<List<HobbyDto>> = api.getHobbies()
    suspend fun getTiposCasa(): Response<List<TipoCasaDto>> = api.getTiposCasa()
    suspend fun getPreguntasRecuperacion(): Response<List<PreguntasRecuperacionDto>> = api.getPreguntasRecuperacion()
}
