package com.intelliworks.intellihome.data.model

data class PropiedadRegistroResponseDto(
    val mensaje: String?,
    val propiedad_id: Int?, // Si tu backend retorna el ID creado
    val errores: Any?       // Puede ser String o Lista
)