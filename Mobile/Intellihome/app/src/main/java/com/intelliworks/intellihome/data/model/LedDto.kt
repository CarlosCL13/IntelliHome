package com.intelliworks.intellihome.data.model

/**
 * Respuesta del backend para el estado de los LEDs
 * El backend devuelve: { "estado": { "Sala": "encendido", "Cocina": "apagado", ... } }
 */
data class EstadoLedsDto(
    val estado: Map<String, String>
) {
    /**
     * Convierte los estados de String ("encendido"/"apagado") a Boolean
     */
    fun toBoolean(): Map<String, Boolean> {
        return estado.mapValues { it.value == "encendido" }
    }
}
