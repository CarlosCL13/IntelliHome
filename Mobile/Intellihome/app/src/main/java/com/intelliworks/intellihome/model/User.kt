package com.intelliworks.intellihome.model

data class User(
    val id: Int,
    val rolId: Int,
    val imagenPerfil: String,
    val nombre: String,
    val apellidos: String,
    val correo: String,
    val username: String,
    val contrasena: String,
    val telefono: String,
    val fechaNacimiento: String,
    val domicilio: String,
    val preguntaRecuperacionId: Int,
    val respuestaRecuperacion: String,
    val fingerprintEnabled: Boolean,
    val intentosFallidos: Int,
    val estadoCuenta: String,
    val nombreTitular: String,
    val numeroEncriptado: String,
    val fechaExpiracion: String,
    val marca: String,
    val ultimos4: String
)
