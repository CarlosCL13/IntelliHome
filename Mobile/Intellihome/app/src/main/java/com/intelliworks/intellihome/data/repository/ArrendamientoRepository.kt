package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.ArrendamientoApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.ArrendamientoResponseDto
import retrofit2.Response

class ArrendamientoRepository {

    private val arrendamientoApi: ArrendamientoApi = RetrofitInstance.retrofit.create(ArrendamientoApi::class.java)

    /**
     * Registra un nuevo arrendamiento en el backend.
     * @param propiedadId ID de la propiedad a rentar.
     * @param inquilinoId ID del usuario que realiza la renta.
     * @param fechaInicio Fecha de inicio en formato YYYY-MM-DD.
     * @param fechaFin Fecha de fin en formato YYYY-MM-DD.
     */
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

    /**
     * Obtiene el listado de propiedades alquiladas asociadas a un usuario.
     * @param userId ID del usuario inquilino.
     */
    suspend fun obtenerAlquiladasPorUsuario(userId: Int) = arrendamientoApi.obtenerAlquiladasPorUsuario(userId)
}