package com.intelliworks.intellihome.utils

import android.content.Context
import com.intelliworks.intellihome.R

/**
 * Objeto de utilidades para la manipulación y formateo de datos de Propiedades.
 * Centraliza la lógica de presentación para garantizar consistencia en la UI.
 */
object PropertyUtils {

    /**
     * Procesa la cadena de capacidad para devolver un texto localizado según el idioma del dispositivo.
     *
     * @param context Contexto necesario para acceder a los recursos de string.
     * @param rawCapacity Cadena almacenada en BD. Puede ser formato CSV ("4,2,2,1") o texto legado.
     * @return Cadena formateada (ej: "4 Huéspedes · 2 Habitaciones...") o el valor original si no tiene formato.
     */
    fun getFormattedCapacity(context: Context, rawCapacity: String): String {
        val parts = rawCapacity.split(",")

        // Validación de estructura CSV: debe tener 4 partes numéricas
        if (parts.size == 4 && parts[0].all { it.isDigit() }) {
            return try {
                context.getString(
                    R.string.fmt_capacity_full,
                    parts[0].toInt(), // Huéspedes
                    parts[1].toInt(), // Habitaciones
                    parts[2].toInt(), // Camas
                    parts[3].toInt()  // Baños
                )
            } catch (e: Exception) {
                rawCapacity
            }
        }
        // Retorno de compatibilidad para datos antiguos
        return rawCapacity
    }
}