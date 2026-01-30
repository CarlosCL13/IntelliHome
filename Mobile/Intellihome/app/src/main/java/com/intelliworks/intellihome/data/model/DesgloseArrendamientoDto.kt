package com.intelliworks.intellihome.data.model

/**
 * DTO para el desglose del último arrendamiento de un usuario en una propiedad.
 */
data class DesgloseArrendamientoDto(
    val subtotal: Double,
    val iva: Double,
    val comision: Double,
    val total: Double
)
