package com.intelliworks.intellihome.data.model

data class CotizacionArrendamientoDto(
    val subtotal: Double,
    val iva: Double,
    val comision: Double,
    val total: Double,
    val noches: Int,
    val precio_noche: Double
)
