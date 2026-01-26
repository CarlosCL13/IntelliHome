package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class PropiedadResumenDto(
    val id: Int,
    @SerializedName("titulo_publicacion") val titulo: String,
    @SerializedName("precio_noche") val precio: Double,
    val huespedes: Int,
    val habitaciones: Int,
    val camas: Int,
    val banos: Int,
    val imagen: String?
)