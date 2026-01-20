package com.intelliworks.intellihome.data.model

data class UsuarioRegistroResponseDto(
    val mensaje: String?,
    val errores: List<String>? = null
)
