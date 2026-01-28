package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    val id: Int?,
    val username: String?,
    val correo: String?,
    val telefono: String?,
    val nombre: String?,
    val apellidos: String?,

    val domicilio: String?,

    @SerializedName("rol_id")
    val rolId: Int?,

    @SerializedName("estado_cuenta")
    val estadoCuenta: String?,

    @SerializedName("imagen_perfil")
    val imagenPerfil: String?,

    val token: String?,

    val errores: Map<String, String>?,
    val mensaje: String?
)