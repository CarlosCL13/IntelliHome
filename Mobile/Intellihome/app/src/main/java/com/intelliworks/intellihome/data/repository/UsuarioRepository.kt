package com.intelliworks.intellihome.data.repository

import com.intelliworks.intellihome.data.api.UsuarioApi
import com.intelliworks.intellihome.data.model.UsuarioRegistroResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import com.intelliworks.intellihome.data.model.LoginResponseDto

class UsuarioRepository(private val api: UsuarioApi) {
    suspend fun registrarUsuario(
        nombre: RequestBody,
        apellidos: RequestBody,
        username: RequestBody,
        correo: RequestBody,
        telefono: RequestBody,
        fechaNacimiento: RequestBody,
        domicilio: RequestBody,
        contrasena: RequestBody,
        imagenPerfil: MultipartBody.Part,
        hobbiesIds: RequestBody,
        tiposCasaIds: RequestBody,
        preguntaRecuperacionId: RequestBody,
        respuestaRecuperacion: RequestBody,
        permitirHuella: RequestBody,
        nombreTitular: RequestBody,
        numeroTarjeta: RequestBody,
        fechaExpiracion: RequestBody,
        tokenPublico: RequestBody
    ): Response<UsuarioRegistroResponseDto> =
        api.registrarUsuario(
            nombre,
            apellidos,
            username,
            correo,
            telefono,
            fechaNacimiento,
            domicilio,
            contrasena,
            imagenPerfil,
            hobbiesIds,
            tiposCasaIds,
            preguntaRecuperacionId,
            respuestaRecuperacion,
            permitirHuella,
            nombreTitular,
            numeroTarjeta,
            fechaExpiracion,
            tokenPublico
        )

    suspend fun loginUsuario(identificador: String, contrasena: String): Response<LoginResponseDto> {
        return api.loginUsuario(identificador, contrasena)
    }
    // Función para buscar usuario por token público
    suspend fun buscarPorToken(tokenPublico: String) = api.buscarPorToken(tokenPublico)
}
