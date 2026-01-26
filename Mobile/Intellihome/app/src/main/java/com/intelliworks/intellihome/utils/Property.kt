package com.intelliworks.intellihome.utils

import java.util.UUID

data class Property(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,          // El id del dueño (Host)
    val nombreUsuario: String,   // Nombre del dueño
    val titulo: String,
    val precio: String,
    val direccion: String,
    val tipo: String,
    val capacidad: String,
    val imagenes: List<String>,
    val descripcion: String,
    val actividades: String,
    val comodidades: String,
    val reglas: String,
    var rentedByUserId: String? = null
)