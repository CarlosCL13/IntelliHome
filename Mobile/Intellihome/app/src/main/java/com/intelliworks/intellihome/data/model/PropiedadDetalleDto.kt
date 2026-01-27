package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class PropiedadDetalleDto(
    val id: Int,
    val usuario: String?,
    @SerializedName("usuario_nombre_completo") val nombreHost: String?,
    @SerializedName("usuario_imagen_perfil") val fotoHost: String?,
    @SerializedName("titulo_publicacion") val titulo: String,
    @SerializedName("descripcion_publicacion") val descripcion: String?,
    @SerializedName("precio_noche") val precio: Double,
    val huespedes: Int,
    val habitaciones: Int,
    val camas: Int,
    val banos: Int,
    @SerializedName("reglas_uso") val reglas: String?,
    val fotos: List<String>?,
    val amenidades: List<CatalogoItemDto>?,
    val hobbies: List<CatalogoItemDto>?
)

data class CatalogoItemDto(val id: Int, val nombre: String)