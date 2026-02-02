package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class PropiedadDetalleDto(
    val id: Int,
    @SerializedName("usuario_id") val usuarioId: Int?,
    val usuario: String?,
    @SerializedName("usuario_nombre_completo") val nombreHost: String?,
    @SerializedName("usuario_imagen_perfil") val fotoHost: String?,
    @SerializedName("titulo_publicacion") val titulo: String,
    @SerializedName("descripcion_publicacion") val descripcion: String?,
    @SerializedName("precio_noche") val precio: Double,
    @SerializedName("dias_disponibles") val diasDisponibles: List<String>,
    val huespedes: Int,
    val habitaciones: Int,
    val camas: Int,
    val banos: Int,
    @SerializedName("reglas_uso") val reglas: String?,
    val fotos: List<String>?,
    val amenidades: List<CatalogoItemDto>?,
    val hobbies: List<CatalogoItemDto>?,
    val latitud: Double?,
    val longitud: Double?,
    @SerializedName("inquilino_actual_id") val inquilinoActualId: Int?,
    @SerializedName("futuros_arrendamientos") val futurosArrendamientos: List<ArrendamientoFechaDto>?)

data class CatalogoItemDto(val id: Int, val nombre: String)

// Clase auxiliar para las fechas
data class ArrendamientoFechaDto(
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String
)