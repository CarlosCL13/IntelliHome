package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.CasaApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.EstadoLedsDto
import retrofit2.Response

class CasaRepository {
    
    private val casaApi: CasaApi = RetrofitInstance.retrofit.create(CasaApi::class.java)
    
    /**
     * Cambia el estado de un LED de una habitación específica
     */
    suspend fun cambiarLedHabitacion(propiedadId: Int, habitacion: String, encender: Boolean): Response<EstadoLedsDto> {
        val accion = if (encender) "encender" else "apagar"
        return casaApi.cambiarLed(
            propiedadId = propiedadId,
            habitacion = habitacion,
            accion = accion
        )
    }
    
    /**
     * Cambia el estado de todos los LEDs
     */
    suspend fun cambiarTodosLosLeds(propiedadId: Int, encender: Boolean): Response<EstadoLedsDto> {
        val accion = if (encender) "todos_encender" else "todos_apagar"
        return casaApi.cambiarLed(
            propiedadId = propiedadId,
            habitacion = null,
            accion = accion
        )
    }
    
    /**
     * Obtiene el estado actual de todos los LEDs
     */
    suspend fun obtenerEstadoLeds(propiedadId: Int): Response<EstadoLedsDto> {
        return casaApi.obtenerEstadoLeds(propiedadId)
    }

    /**
     * Cambia el estado del Garaje (Abrir/Cerrar)
     */
    suspend fun cambiarEstadoGaraje(propiedadId: Int, abrir: Boolean): Response<Void> {
        val accion = if (abrir) "abrir" else "cerrar"
        return casaApi.controlarGaraje(
            propiedadId = propiedadId,
            accion = accion
        )
    }
}
