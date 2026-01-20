package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del servidor al solicitar la pregunta de seguridad.
 * Mapea directamente el diccionario retornado por 'obtener_pregunta_recuperacion' en Python.
 */
data class PreguntaResponseDto(
    @SerializedName("identificador")
    val identificador: String?,

    @SerializedName("pregunta_id")
    val preguntaId: Int?,

    // Cambiamos el nombre aquí para que coincida con tu código Kotlin
    @SerializedName("pregunta")
    val pregunta: String?,

    @SerializedName("errores")
    val errores: Map<String, String>? = null
)
/**
 * Objeto enviado al servidor para procesar el cambio de contraseña.
 * Debe coincidir con los parámetros de 'restablecer_contrasena' en el servicio de Python.
 */
data class RestablecerRequestDto(
    @SerializedName("identificador")
    val identificador: String,

    @SerializedName("nueva_contrasena")
    val nuevaContrasena: String,

    @SerializedName("respuesta_recuperacion")
    val respuestaRecuperacion: String
)

/**
 * Respuesta genérica para operaciones exitosas o con errores de validación.
 */
data class RecoveryResultDto(
    @SerializedName("mensaje")
    val mensaje: String?,

    @SerializedName("errores")
    val errores: Map<String, String>? = null
)