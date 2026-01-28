package com.intelliworks.intellihome.utils

import com.intelliworks.intellihome.data.model.DatosFormularioRegistro

/**
 * Validador para los datos del formulario de registro
 */
class RegistroValidator {

    companion object {
        // Patrón de validación de email compatible con tests unitarios
        private val EMAIL_PATTERN = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )
    }

    /**
     * Valida todos los datos del formulario
     * @return Mensaje de error si hay alguno, null si todo es válido
     */
    fun validarFormularioCompleto(
        datos: DatosFormularioRegistro,
        terminosAceptados: Boolean,
        imagenSeleccionada: Boolean
    ): String? {
        
        // Verifica si se aceptaron los términos y condiciones
        if (!terminosAceptados) {
            return "Debes aceptar los términos y condiciones"
        }

        // Valida el formato del correo electrónico
        if (!EMAIL_PATTERN.matches(datos.correo)) {
            return "Ingresa un correo electrónico válido"
        }

        // Valida cada campo individualmente con mensaje específico
        if (datos.username.isEmpty()) 
            return "El nombre de usuario es requerido"
        if (datos.password.isEmpty()) 
            return "La contraseña es requerida"
        if (datos.confirmPassword.isEmpty()) 
            return "Debes confirmar la contraseña"
        if (datos.nombre.isEmpty()) 
            return "El nombre es requerido"
        if (datos.apellidos.isEmpty()) 
            return "Los apellidos son requeridos"
        if (datos.correo.isEmpty()) 
            return "El correo electrónico es requerido"
        if (datos.telefono.isEmpty()) 
            return "El teléfono es requerido"
        if (datos.fechaNacimiento.isEmpty()) 
            return "La fecha de nacimiento es requerida"
        if (datos.domicilio.isEmpty()) 
            return "El domicilio es requerido"
        if (datos.respuestaRecuperacion.isEmpty()) 
            return "La respuesta de recuperación es requerida"
        if (datos.nombreTitular.isEmpty()) 
            return "El nombre del titular de la tarjeta es requerido"
        if (datos.numeroEncriptado.isEmpty()) 
            return "El número de tarjeta es requerido"
        if (datos.fechaExpiracion.isEmpty()) 
            return "La fecha de vencimiento de la tarjeta es requerida"
        if (datos.hobbiesIds.isEmpty()) 
            return "Debes seleccionar al menos un hobby"
        if (datos.tiposCasaIds.isEmpty()) 
            return "Debes seleccionar al menos un tipo de casa"
        
        // Valida que las contraseñas coincidan
        if (datos.password != datos.confirmPassword) {
            return "Las contraseñas no coinciden"
        }
        
        // Verifica que se haya seleccionado una imagen
        if (!imagenSeleccionada) {
            return "Selecciona una imagen de perfil"
        }

        return null
    }
}
