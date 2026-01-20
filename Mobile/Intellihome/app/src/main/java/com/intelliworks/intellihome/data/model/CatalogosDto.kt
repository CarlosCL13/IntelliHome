package com.intelliworks.intellihome.data.model

// Crea los DTOs para los catálogos (id, nombre)
data class HobbyDto(
    val id: Int,
    val nombre: String
)

// Crea los DTOs para los catálogos (id, nombre)
data class TipoCasaDto(
    val id: Int,
    val nombre: String
)

// Crea los DTOs para los catálogos (id, texto)
data class PreguntasRecuperacionDto(
    val id: Int,
    val texto: String
)
