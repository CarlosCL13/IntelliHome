package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.PropiedadDetalleDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PropiedadApi {
    
    // Obtiene los detalles completos de una propiedad por su ID.
    @GET("propiedades/{propiedad_id}")
    suspend fun obtenerPropiedadPorId(
        @Path("propiedad_id") propiedadId: Int
    ): Response<PropiedadDetalleDto>
}
