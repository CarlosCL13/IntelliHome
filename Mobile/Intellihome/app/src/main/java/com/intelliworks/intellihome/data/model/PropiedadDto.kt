package com.intelliworks.intellihome.data.model

data class PropiedadDetalleDto(
    val id: Int,
    val usuario: String?,
    val usuario_nombre_completo: String?,
    val usuario_telefono: String?,
    val usuario_imagen_perfil: String?,
    val tipo_casa: String?,
    val latitud: Double,
    val longitud: Double,
    val titulo_publicacion: String,
    val descripcion_publicacion: String,
    val precio_noche: Double,
    val huespedes: Int,
    val habitaciones: Int,
    val camas: Int,
    val banos: Int,
    val cocina: Boolean,
    val reglas_uso: String?,
    val vehiculos: Int?,
    val estado: String,
    val fotos: List<String>,
    val amenidades: List<AmenidadDto>,
    val hobbies: List<HobbyDto>
)
