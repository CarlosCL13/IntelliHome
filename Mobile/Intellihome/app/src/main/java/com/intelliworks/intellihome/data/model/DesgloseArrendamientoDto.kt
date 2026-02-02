package com.intelliworks.intellihome.data.model
import com.google.gson.annotations.SerializedName
/**
 * DTO para el desglose del último arrendamiento de un usuario en una propiedad.
 */
data class DesgloseArrendamientoDto(
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("iva") val iva: Double,
    @SerializedName("comision") val comision: Double,
    @SerializedName("total") val total: Double,
    @SerializedName("noches") val noches: Int
)
