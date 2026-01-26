package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.PropiedadDetalleDto
import retrofit2.Response

class PropiedadRepository(private val propiedadId: Int = 1) {
    
    private val propiedadApi: PropiedadApi = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
    
    // Obtiene los detalles completos de la propiedad actual
    suspend fun obtenerDetallePropiedad(): Response<PropiedadDetalleDto> {
        return propiedadApi.obtenerPropiedadPorId(propiedadId)
    }
}
