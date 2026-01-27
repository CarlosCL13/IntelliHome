package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.PropiedadAlquiladaDto
import com.intelliworks.intellihome.data.model.PropiedadDetalleDto
import com.intelliworks.intellihome.data.model.PropiedadRegistroResponseDto
import com.intelliworks.intellihome.data.model.PropiedadResumenDto // Importar el nuevo DTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PropiedadApi {

    @GET("arrendamientos/alquiladas/{user_id}")
    suspend fun obtenerPropiedadesAlquiladas(
        @Path("user_id") userId: Int
    ): Response<List<PropiedadAlquiladaDto>>


    @GET("propiedades/{id}")
    suspend fun obtenerDetallePropiedad(@Path("id") id: Int): Response<PropiedadDetalleDto>

    @GET("propiedades/todas")
    suspend fun obtenerTodasPropiedades(): Response<List<PropiedadResumenDto>>

    @Multipart
    @POST("propiedades/registro-propiedad")
    suspend fun registrarPropiedad(
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("tipo_casa_id") tipoCasaId: RequestBody,
        @Part("hobbies_ids") hobbiesIds: RequestBody,
        @Part("amenidades_ids") amenidadesIds: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part("titulo_publicacion") titulo: RequestBody,
        @Part("descripcion_publicacion") descripcion: RequestBody,
        @Part("precio_noche") precio: RequestBody,
        @Part("huespedes") huespedes: RequestBody,
        @Part("habitaciones") habitaciones: RequestBody,
        @Part("camas") camas: RequestBody,
        @Part("banos") banos: RequestBody,
        @Part("cocina") cocina: RequestBody,
        @Part("reglas_uso") reglas: RequestBody,
        @Part("vehiculos") vehiculos: RequestBody,
        @Part("estado") estado: RequestBody,
        // Lista de fotos
        @Part fotos_propiedad: List<MultipartBody.Part>
    ): Response<PropiedadRegistroResponseDto>
}