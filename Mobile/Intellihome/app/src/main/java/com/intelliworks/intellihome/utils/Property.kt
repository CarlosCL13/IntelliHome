package com.intelliworks.intellihome.utils

import java.util.UUID
import java.io.Serializable

data class Property(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val titulo: String,
    val precio: String,
    val direccion: String,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val tipo: String,
    val capacidad: String,
    val imagenes: List<String>,
    val descripcion: String,
    val actividades: String,
    val comodidades: String,
    val reglas: String,
    var rentedByUserId: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null
): Serializable