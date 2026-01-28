package com.intelliworks.intellihome.utils

import android.content.Context
import android.location.Geocoder
import com.intelliworks.intellihome.R
import com.intelliworks.intellihome.data.model.PropiedadResumenDto
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Centraliza la lógica de negocio relacionada con la presentación de datos en la UI.
 */
object PropertyUtils {

    private const val TAG = "PropertyUtils"

    /**
     * Convierte un objeto de transferencia de datos [PropiedadResumenDto] (Backend)
     * a un objeto de modelo de UI [Property].
     *
     * @param dto El objeto DTO recibido de la API.
     * @param currentUserId El ID del usuario que está usando la app (solo para referencia, no para asignar propiedad).
     * @param context Contexto necesario para recursos.
     */
    fun mapDtoToProperty(dto: PropiedadResumenDto, currentUserId: String, context: Context): Property {

        // Formateo de datos auxiliares
        val rawCapacity = "${dto.huespedes},${dto.habitaciones},${dto.camas},${dto.banos}"
        val capacityFormatted = getFormattedCapacity(context, rawCapacity)
        val locationString = getAddressFromCoordinates(context, dto.latitud, dto.longitud)
        val imagesList = if (!dto.imagen.isNullOrEmpty()) listOf(dto.imagen) else emptyList()

        // Mapeo al objeto visual Property
        return Property(
            id = dto.id.toString(),
            userId = dto.usuarioId.toString(),
            titulo = dto.titulo,
            precio = dto.precio.toString(),
            direccion = locationString,
            tipo = "Propiedad",
            capacidad = capacityFormatted,
            imagenes = imagesList,
            // Nota: Si PropiedadResumenDto no tiene descripción, usa un default
            descripcion = "Ver detalles para más información.",
            actividades = "",
            comodidades = "",
            reglas = "",
            rentedByUserId = null,
            latitud = dto.latitud ?: 0.0,
            longitud = dto.longitud ?: 0.0,
            fechaInicio = dto.fechaInicio,
            fechaFin = dto.fechaFin
        )
    }

    /**
     * Procesa una cadena de capacidad en formato CSV y devuelve texto legible.
     */
    fun getFormattedCapacity(context: Context, rawCapacity: String): String {
        val parts = rawCapacity.split(",")
        // Validación básica: esperamos 4 partes numéricas
        if (parts.size == 4 && parts.all { it.all { char -> char.isDigit() } }) {
            return try {
                context.getString(
                    R.string.fmt_capacity_full,
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt(),
                    parts[3].toInt()
                )
            } catch (e: Exception) {
                rawCapacity
            }
        }
        return rawCapacity
    }

    /**
     * Realiza una geocodificación inversa (Lat/Lon -> Dirección).
     */
    fun getAddressFromCoordinates(context: Context, lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "Ubicación sin definir"

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // Pedimos solo 1 resultado
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: ""
                val state = address.adminArea ?: ""

                if (city.isNotEmpty() && state.isNotEmpty()) {
                    "$city, $state"
                } else {
                    city + state
                }
            } else {
                // Fallback a coordenadas si no encuentra dirección
                "Lat: %.2f, Lon: %.2f".format(lat, lon)
            }
        } catch (e: Exception) {
            // Fallback en caso de error (ej: sin internet)
            "Lat: %.2f, Lon: %.2f".format(lat, lon)
        }
    }
    fun formatearFechaLocal(context: Context, fechaString: String?): String {
        if (fechaString.isNullOrEmpty()) return ""

        try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatoEntrada.timeZone = TimeZone.getTimeZone("UTC")

            val fechaObj = formatoEntrada.parse(fechaString) ?: return fechaString

            val localeActual = context.resources.configuration.locales[0]
            val formatoSalida = DateFormat.getDateInstance(DateFormat.MEDIUM, localeActual)

            formatoSalida.timeZone = TimeZone.getTimeZone("UTC")

            return formatoSalida.format(fechaObj)
        } catch (e: Exception) {
            return fechaString
        }
    }
}