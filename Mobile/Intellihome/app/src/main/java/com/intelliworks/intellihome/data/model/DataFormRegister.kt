package com.intelliworks.intellihome.data.model

/**
 * Data class para almacenar los datos del formulario de registro
 */
data class DatosFormularioRegistro(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val nombre: String,
    val apellidos: String,
    val correo: String,
    val telefono: String,
    val fechaNacimiento: String,
    val domicilio: String,
    val preguntaRecuperacionId: Int,
    val respuestaRecuperacion: String,
    val fingerprintEnabled: Boolean,
    val nombreTitular: String,
    val numeroEncriptado: String,
    val fechaExpiracion: String,
    val hobbiesIds: List<Int>,
    val tiposCasaIds: List<Int>
)
