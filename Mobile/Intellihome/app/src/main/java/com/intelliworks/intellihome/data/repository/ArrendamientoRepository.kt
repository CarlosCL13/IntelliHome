package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.ArrendamientoApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.ArrendamientoResponseDto
import retrofit2.Response

class ArrendamientoRepository {
    
    private val arrendamientoApi: ArrendamientoApi = RetrofitInstance.retrofit.create(ArrendamientoApi::class.java)
    
    // Registra un nuevo arrendamiento en el backend
    suspend fun registrarArrendamiento(
        propiedadId: Int,
        inquilinoId: Int,
        fechaInicio: String,
        fechaFin: String
    ): Response<ArrendamientoResponseDto> {
        return arrendamientoApi.registrarArrendamiento(
            propiedadId = propiedadId,
            inquilinoId = inquilinoId,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin
        )
    }
}
