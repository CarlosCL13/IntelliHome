package com.intelliworks.intellihome.utils

import android.content.Context

object SessionManager {
    // Estas son las "etiquetas" para guardar datos en la memoria del teléfono
    private const val PREF_NAME = "intellihome_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHOTO = "user_photo" // Nueva constante para la foto

    /**
     * Guarda los datos del usuario al iniciar sesión.
     */
    fun iniciarSesion(context: Context, userId: String, nombre: String, fotoUrl: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, nombre)
        editor.putString(KEY_USER_PHOTO, fotoUrl) // Guardamos la URL de la imagen
        editor.apply()
    }

    /**
     * Recupera la URL de la foto de perfil guardada.
     */
    fun obtenerFotoPerfil(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_PHOTO, null)
    }

    /**
     * Recupera el Nombre del usuario.
     */
    fun obtenerNombreUsuario(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    }

    /**
     * Recupera el ID del usuario.
     */
    fun obtenerUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    /**
     * Borra todos los datos (Cerrar sesión).
     */
    fun cerrarSesion(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}