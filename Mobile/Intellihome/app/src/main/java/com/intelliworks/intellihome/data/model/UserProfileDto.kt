package com.intelliworks.intellihome.data.model

data class UserProfileDto(
    val nombre_completo: String,
    val nombre_usuario: String,
    val correo: String,
    val domicilio: String,
    val imagen: String?,
    val hobbies_ids: List<Int>,
    val preferencias_ids: List<Int>
)