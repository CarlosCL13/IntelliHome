package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

/**
 * DTO para el listado general de propiedades (Explore).
 */
data class PropiedadResumenDto(
    val id: Int,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("titulo_publicacion")
    val titulo: String,
    @SerializedName("precio_noche")
    val precio: Double,
    @SerializedName("descripcion_publicacion")
    val descripcion: String?, // Puede ser null
    val huespedes: Int,
    val habitaciones: Int,
    val camas: Int,
    val banos: Int,
    val imagen: String?, // Puede ser null
    val latitud: Double?,
    val longitud: Double?,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null
)