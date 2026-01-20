package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.UsuarioRegistroResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UsuarioApi {
    @Multipart
    @POST("usuarios/registro")
    suspend fun registrarUsuario(
        @Part("nombre") nombre: RequestBody,
        @Part("apellidos") apellidos: RequestBody,
        @Part("username") username: RequestBody,
        @Part("correo") correo: RequestBody,
        @Part("telefono") telefono: RequestBody,
        @Part("fecha_nacimiento") fechaNacimiento: RequestBody,
        @Part("domicilio") domicilio: RequestBody,
        @Part("contrasena") contrasena: RequestBody,
        @Part imagen_perfil: MultipartBody.Part,
        @Part("hobbies_ids") hobbiesIds: RequestBody,
        @Part("tipos_casa_ids") tiposCasaIds: RequestBody,
        @Part("pregunta_recuperacion_id") preguntaRecuperacionId: RequestBody,
        @Part("respuesta_recuperacion") respuestaRecuperacion: RequestBody,
        @Part("permitir_huella") permitirHuella: RequestBody,
        @Part("nombre_titular") nombreTitular: RequestBody,
        @Part("numero_tarjeta") numeroTarjeta: RequestBody,
        @Part("fecha_expiracion") fechaExpiracion: RequestBody
    ): Response<UsuarioRegistroResponseDto>
}
