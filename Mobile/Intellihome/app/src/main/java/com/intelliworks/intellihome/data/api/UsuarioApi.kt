package com.intelliworks.intellihome.data.api

import com.intelliworks.intellihome.data.model.UsuarioRegistroResponseDto
import com.intelliworks.intellihome.data.model.LoginResponseDto
import com.intelliworks.intellihome.data.model.PreguntaResponseDto
import com.intelliworks.intellihome.data.model.RecoveryResultDto
import com.intelliworks.intellihome.data.model.Ultimos4TarjetaDto
import com.intelliworks.intellihome.data.model.UserProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

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
        @Part("fecha_expiracion") fechaExpiracion: RequestBody,
        @Part("token_publico") tokenPublico: RequestBody
    ): Response<UsuarioRegistroResponseDto>

    @FormUrlEncoded
    @POST("usuarios/login")
    suspend fun loginUsuario(
        @Field("identificador") identificador: String,
        @Field("contrasena") contrasena: String
    ): Response<LoginResponseDto>

    /**
     * Obtiene la pregunta de seguridad.
     * Sincronizado con @router.post("/recuperar-contrasena") en Python.
     */
    @FormUrlEncoded
    @POST("usuarios/recuperar-contrasena")
    suspend fun obtenerPregunta(
        @Field("identificador") identificador: String
    ): Response<PreguntaResponseDto>

    /**
     * Restablece la contraseña.
     * Sincronizado con @router.post("/restablecer-contrasena") y Form(...) en Python.
     */
    @FormUrlEncoded
    @POST("usuarios/restablecer-contrasena")
    suspend fun restablecerContrasena(
        @Field("identificador") identificador: String,
        @Field("nueva_contrasena") nuevaContrasena: String,
        @Field("respuesta_recuperacion") respuestaRecuperacion: String
    ): Response<RecoveryResultDto>

    /**
     * Busca un usuario por su token público.
     * Sincronizado con @router.post("/buscar-por-token") en Python.
     */
    @FormUrlEncoded
    @POST("usuarios/buscar-por-token")
    suspend fun buscarPorToken(
        @Field("token_publico") tokenPublico: String
    ): Response<LoginResponseDto>

    /**
     * Obtiene los últimos 4 dígitos de la tarjeta de un usuario
     * Sincronizado con @router.get("/tarjeta/ultimos4/{user_id}") en Python.
     */
    @GET("usuarios/tarjeta/ultimos4/{user_id}")
    suspend fun obtenerUltimos4Tarjeta(
        @Path("user_id") userId: Int
    ): Response<Ultimos4TarjetaDto>

    /**
     * Obtiene los datos del perfil logeado.
     * Sincronizado con @router.post("/perfil/{user_id}) en Python.
     */
    @GET("usuarios/perfil/{user_id}")
    suspend fun obtenerPerfilUsuario(
        @Path("user_id") userId: Int
    ): Response<UserProfileDto>
}