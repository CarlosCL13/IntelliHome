package com.intelliworks.intellihome.data.repository

import android.content.Context
import android.net.Uri
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.model.PropiedadDetalleDto
import com.intelliworks.intellihome.data.model.PropiedadRegistroResponseDto
import com.intelliworks.intellihome.data.model.PropiedadResumenDto
import com.intelliworks.intellihome.utils.FileUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class PropiedadRepository(private val api: PropiedadApi) {

    suspend fun obtenerDetalle(id: Int): Response<PropiedadDetalleDto> {
        return api.obtenerDetallePropiedad(id)
    }

    suspend fun obtenerTodas(): Response<List<PropiedadResumenDto>> {
        return api.obtenerTodasPropiedades()
    }

    suspend fun registrarPropiedad(
        context: Context,
        usuarioId: Int,
        tipoCasaId: Int,
        hobbiesIds: List<Int>,
        amenidadesIds: List<Int>, // Necesitarás mapear esto en tu ViewModel
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
    ): Response<PropiedadRegistroResponseDto> {

        // 1. Preparar campos de texto (RequestBody)
        val textType = "text/plain".toMediaTypeOrNull()

        // Conversión de listas a String separado por comas: "1,2,3"
        val hobbiesStr = hobbiesIds.joinToString(",")
        val amenidadesStr = amenidadesIds.joinToString(",")

        val rbUsuarioId = usuarioId.toString().toRequestBody(textType)
        val rbTipoCasa = tipoCasaId.toString().toRequestBody(textType)
        val rbHobbies = hobbiesStr.toRequestBody(textType)
        val rbAmenidades = amenidadesStr.toRequestBody(textType)
        val rbLatitud = latitud.toString().toRequestBody(textType)
        val rbLongitud = longitud.toString().toRequestBody(textType)
        val rbTitulo = titulo.toRequestBody(textType)
        val rbDescripcion = descripcion.toRequestBody(textType)
        val rbPrecio = precio.toString().toRequestBody(textType)
        val rbHuespedes = huespedes.toString().toRequestBody(textType)
        val rbHabitaciones = habitaciones.toString().toRequestBody(textType)
        val rbCamas = camas.toString().toRequestBody(textType)
        val rbBanos = banos.toString().toRequestBody(textType)
        // Cocina en Python es bool, enviamos "true" o "false" (o "1"/"0")
        val rbCocina = cocina.toString().toRequestBody(textType)
        val rbReglas = reglas.toRequestBody(textType)
        val rbVehiculos = vehiculos.toString().toRequestBody(textType)
        val rbEstado = "disponible".toRequestBody(textType)

        // 2. Preparar Imágenes (MultipartBody.Part)
        val partesFotos = mutableListOf<MultipartBody.Part>()

        for (uri in fotosUris) {
            // Usamos un helper para obtener el archivo real desde la URI
            val file = FileUtils.getFileFromUri(context, uri)
            if (file != null && file.exists()) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                // IMPORTANTE: El nombre del campo debe coincidir con el backend: "fotos_propiedad"
                val part = MultipartBody.Part.createFormData("fotos_propiedad", file.name, requestFile)
                partesFotos.add(part)
            }
        }

        return api.registrarPropiedad(
            usuarioId = rbUsuarioId,
            tipoCasaId = rbTipoCasa,
            hobbiesIds = rbHobbies,
            amenidadesIds = rbAmenidades,
            latitud = rbLatitud,
            longitud = rbLongitud,
            titulo = rbTitulo,
            descripcion = rbDescripcion,
            precio = rbPrecio,
            huespedes = rbHuespedes,
            habitaciones = rbHabitaciones,
            camas = rbCamas,
            banos = rbBanos,
            cocina = rbCocina,
            reglas = rbReglas,
            vehiculos = rbVehiculos,
            estado = rbEstado,
            fotos_propiedad = partesFotos
        )
    }
}