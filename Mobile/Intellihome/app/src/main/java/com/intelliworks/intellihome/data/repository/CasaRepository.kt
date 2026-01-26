package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.CasaApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.EstadoLedsDto
import retrofit2.Response

class CasaRepository {
    
    private val casaApi: CasaApi = RetrofitInstance.retrofit.create(CasaApi::class.java)

    // Se define un ID de propiedad fijo para pruebas
    // Esto debe de cambiarse según sea el caso (usando SharedPreferences, parametros o inicializar este repositorio con el ID dinamico)
    companion object {
        private const val PROPIEDAD_ID = 1
    }
    
    /**
     * Cambia el estado de un LED de una habitación específica
     */
    suspend fun cambiarLedHabitacion(habitacion: String, encender: Boolean): Response<EstadoLedsDto> {
        val accion = if (encender) "encender" else "apagar"
        return casaApi.cambiarLed(
            propiedadId = PROPIEDAD_ID,
            habitacion = habitacion,
            accion = accion
        )
    }
    
    /**
     * Cambia el estado de todos los LEDs
     */
    suspend fun cambiarTodosLosLeds(encender: Boolean): Response<EstadoLedsDto> {
        val accion = if (encender) "todos_encender" else "todos_apagar"
        return casaApi.cambiarLed(
            propiedadId = PROPIEDAD_ID,
            habitacion = null,
            accion = accion
        )
    }
    
    /**
     * Obtiene el estado actual de todos los LEDs
     */
    suspend fun obtenerEstadoLeds(): Response<EstadoLedsDto> {
        return casaApi.obtenerEstadoLeds(PROPIEDAD_ID)
    }
}
