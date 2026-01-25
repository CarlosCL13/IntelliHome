package com.intelliworks.intellihome.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton responsable de la gestión de la sesión del usuario.
 * Encapsula el acceso a SharedPreferences para persistir datos críticos
 * del usuario activo durante el ciclo de vida de la aplicación.
 */
object SessionManager {
    private const val PREFS_NAME = "intelli_user_session"
    private const val KEY_USER_ID = "active_user_id"
    private const val KEY_USER_NAME = "active_user_name"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    /**
     * Inicializa la sesión del usuario tras una autenticación exitosa.
     * @param userId Identificador único del usuario (proveniente del backend).
     * @param nombre Nombre para mostrar en la interfaz.
     */
    fun iniciarSesion(context: Context, userId: String, nombre: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, nombre)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Retorna el ID del usuario actualmente autenticado.
     * @return String con el ID o cadena vacía si no hay sesión.
     */
    fun obtenerUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    /**
     * Retorna el nombre del usuario para visualización en UI.
     */
    fun obtenerNombreUsuario(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    }

    /**
     * Verifica si existe una sesión activa.
     */
    fun estaLogueado(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Limpia los datos de sesión (Logout).
     */
    fun cerrarSesion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}