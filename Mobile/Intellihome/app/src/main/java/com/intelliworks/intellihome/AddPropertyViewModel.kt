package com.intelliworks.intellihome

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AddPropertyViewModel : ViewModel() {
    val tipoPropiedad = MutableLiveData<String>()

    val esTipoValido = tipoPropiedad.map { !it.isNullOrEmpty() }
    // 1. Set para guardar múltiples actividades (evita duplicados)
    val actividadesSeleccionadas = MutableLiveData<MutableSet<String>>(mutableSetOf())

    // 2. Función para agregar o quitar actividades (Toggle)
    fun toggleActividad(actividad: String) {
        val actual = actividadesSeleccionadas.value ?: mutableSetOf()
        if (actual.contains(actividad)) {
            actual.remove(actividad)
        } else {
            actual.add(actividad)
        }
        actividadesSeleccionadas.value = actual // Notificar cambio
    }

    // 3. Validar si hay al menos una seleccionada (para habilitar el botón)
    val hayActividades = actividadesSeleccionadas.map { it.isNotEmpty() }

    // Datos de ubicación
    val direccionTexto = MutableLiveData<String>()
    val latitud = MutableLiveData<Double>()
    val longitud = MutableLiveData<Double>()

    // El botón Siguiente se habilita si hay una dirección seleccionada
    val esDireccionValida = direccionTexto.map { !it.isNullOrEmpty() }
    val titulo = MutableLiveData<String>("")
    val precio = MutableLiveData<String>("") // String para manejar el input del EditText facil

    // Contadores (Valores iniciales según mockup)
    val huespedes = MutableLiveData<Int>(4)
    val habitaciones = MutableLiveData<Int>(2)
    val camas = MutableLiveData<Int>(2)
    val banos = MutableLiveData<Int>(1)

    // Validación: El botón siguiente se activa si hay título y precio
    val sonDetallesValidos = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        fun validar() {
            val t = titulo.value
            val p = precio.value
            value = !t.isNullOrEmpty() && !p.isNullOrEmpty()
        }
        addSource(titulo) { validar() }
        addSource(precio) { validar() }
    }

    val comodidadesSeleccionadas = MutableLiveData<MutableSet<String>>(mutableSetOf())

    fun toggleComodidad(comodidad: String) {
        val actual = comodidadesSeleccionadas.value ?: mutableSetOf()
        if (actual.contains(comodidad)) {
            actual.remove(comodidad)
        } else {
            actual.add(comodidad)
        }
        // Forzamos la actualización del LiveData
        comodidadesSeleccionadas.value = actual
    }
    // --- SECCIÓN FOTOS ---
    val fotosSeleccionadas = MutableLiveData<List<Uri>>(emptyList())

    fun agregarFotos(nuevasUris: List<Uri>) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        // Sumamos las listas
        fotosSeleccionadas.value = actual + nuevasUris
    }

    fun eliminarFoto(uri: Uri) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        fotosSeleccionadas.value = actual - uri
    }
}