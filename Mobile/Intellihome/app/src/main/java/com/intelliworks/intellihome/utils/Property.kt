package com.intelliworks.intellihome.utils

import java.util.UUID

/**
 * Modelo de dominio que representa una propiedad inmobiliaria.
 * @property userId Identificador del usuario propietario del inmueble.
 */
data class Property(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val titulo: String,
    val precio: String,
    val direccion: String,
    val tipo: String,
    val capacidad: String,
    val imagenUri: String
)