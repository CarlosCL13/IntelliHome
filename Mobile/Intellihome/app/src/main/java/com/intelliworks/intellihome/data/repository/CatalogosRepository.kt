package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.model.AmenidadDto
import com.intelliworks.intellihome.data.model.HobbyDto
import com.intelliworks.intellihome.data.model.TipoCasaDto

class CatalogosRepository(private val api: CatalogosApi) {

    suspend fun obtenerTipos(): List<TipoCasaDto> {
        return try {
            val response = api.getTiposCasa() // Nombre exacto de la interfaz
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerHobbies(): List<HobbyDto> {
        return try {
            val response = api.getHobbies() // <--- AQUÍ SALTA EL ERROR SI NO COINCIDE
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obtenerAmenidades(): List<AmenidadDto> {
        return try {
            val response = api.getAmenidades() // Nombre exacto de la interfaz
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}