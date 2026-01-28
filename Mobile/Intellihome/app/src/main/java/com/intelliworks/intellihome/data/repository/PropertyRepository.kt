package com.intelliworks.intellihome.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intelliworks.intellihome.utils.Property

/**
 * Singleton encargado de la persistencia de datos de propiedades.
 * Utiliza SharedPreferences para almacenar la lista en formato JSON.
 */
object PropertyRepository {
    private const val PREFS_NAME = "intelli_prefs"
    private const val KEY_PROPERTIES = "saved_properties"
    private val gson = Gson()

    // Guarda una nueva propiedad al inicio de la lista existente
    fun saveProperty(context: Context, property: Property) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        // 1. Obtener lista actual
        val properties = getProperties(context).toMutableList()

        // 2. BUSCAR Y ELIMINAR la versión vieja si existe (Para evitar duplicados)
        val index = properties.indexOfFirst { it.id == property.id }
        if (index != -1) {
            properties[index] = property // Reemplazar (Actualizar)
        } else {
            properties.add(property) // Agregar nueva
        }

        // 3. Guardar lista actualizada
        val editor = sharedPreferences.edit()
        val jsonString = gson.toJson(properties)
        editor.putString(KEY_PROPERTIES, jsonString)
        editor.apply()
    }

    // Recupera todas las propiedades almacenadas
    fun getProperties(context: Context): List<Property> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_PROPERTIES, null)
        return if (json != null) {
            val type = object : TypeToken<List<Property>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveList(context: Context, list: List<Property>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(list)
        editor.putString(KEY_PROPERTIES, json)
        editor.apply()
    }
}