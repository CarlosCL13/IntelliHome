package com.intelliworks.intellihome.data.repository

import android.content.Context
import android.net.Uri
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.utils.FileUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class PropiedadRepository(private val api: PropiedadApi) {
    suspend fun obtenerDetalle(id: Int) = api.obtenerDetallePropiedad(id)

    suspend fun obtenerPorUsuario(userId: Int) = api.obtenerPropiedadesPorUsuario(userId)

    suspend fun obtenerTodas() = api.obtenerTodasPropiedades()

    /**
     * Método Helper para simplificar el envío desde el Fragmento.
     * Convierte los datos primitivos a RequestBody y procesa las imágenes.
     */
    suspend fun registrarPropiedad(
        context: Context, // Necesario para procesar las URIs de las fotos
        usuarioId: Int,
        tipoCasaId: Int,
        hobbiesIds: List<Int>,
        amenidadesIds: List<Int>,
        latitud: Double,
        longitud: Double,
        titulo: String,
        descripcion: String,
        precio: Double,
        huespedes: Int,
        habitaciones: Int,
        camas: Int,
        banos: Int,
        cocina: Boolean,
        reglas: String,
        vehiculos: Int,
        fotosUris: List<Uri>
    ) = api.registrarPropiedad(
        usuarioId = toRequestBody(usuarioId.toString()),
        tipoCasaId = toRequestBody(tipoCasaId.toString()),
        // Convertimos las listas [1, 2] a String "1,2" para enviarlas
        hobbiesIds = toRequestBody(hobbiesIds.joinToString(",")),
        amenidadesIds = toRequestBody(amenidadesIds.joinToString(",")),
        latitud = toRequestBody(latitud.toString()),
        longitud = toRequestBody(longitud.toString()),
        titulo = toRequestBody(titulo),
        descripcion = toRequestBody(descripcion),
        precio = toRequestBody(precio.toString()),
        huespedes = toRequestBody(huespedes.toString()),
        habitaciones = toRequestBody(habitaciones.toString()),
        camas = toRequestBody(camas.toString()),
        banos = toRequestBody(banos.toString()),
        cocina = toRequestBody(if (cocina) "1" else "0"),
        reglas = toRequestBody(reglas),
        vehiculos = toRequestBody(vehiculos.toString()),
        estado = toRequestBody("Disponible"), // Estado por defecto
        fotos_propiedad = prepararFotos(context, fotosUris)
    )

    // Función privada para convertir Strings a RequestBody
    private fun toRequestBody(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    // Función privada para procesar la lista de URIs a MultipartBody.Part
    private fun prepararFotos(context: Context, uris: List<Uri>): List<MultipartBody.Part> {
        val partes = mutableListOf<MultipartBody.Part>()

        uris.forEachIndexed { index, uri ->
            val file = FileUtils.getFileFromUri(context, uri)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("fotos_propiedad", "foto_$index.jpg", requestFile)
                partes.add(part)
            }
        }
        return partes
    }
}