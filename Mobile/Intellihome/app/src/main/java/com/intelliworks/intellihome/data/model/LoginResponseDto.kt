package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    val id: Int?,
    val username: String?,
    val correo: String?,
    val telefono: String?,
    val nombre: String?,
    val apellidos: String?,

    @SerializedName("rol_id")
    val rolId: Int?,

    @SerializedName("estado_cuenta")
    val estadoCuenta: String?,

    // --- CAMBIO AQUÍ: Agregamos el campo de la imagen ---
    // Asegúrate de que "imagen_perfil" sea el nombre exacto que devuelve tu API en Python
    @SerializedName("imagen_perfil")
    val imagenPerfil: String?,

    val errores: Map<String, String>?,
    val mensaje: String?
)