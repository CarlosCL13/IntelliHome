package com.intelliworks.intellihome

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AddPropertyViewModel : ViewModel() {

    // --- TIPO DE PROPIEDAD ---
    val tipoPropiedad = MutableLiveData<String>()
    val esTipoValido = tipoPropiedad.map { !it.isNullOrEmpty() }

    // --- ACTIVIDADES ---
    // Se utiliza un Set para evitar duplicados automáticamente al seleccionar/deseleccionar
    val actividadesSeleccionadas = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val hayActividades = actividadesSeleccionadas.map { it.isNotEmpty() }

    fun toggleActividad(actividad: String) {
        val actual = actividadesSeleccionadas.value ?: mutableSetOf()
        if (actual.contains(actividad)) {
            actual.remove(actividad)
        } else {
            actual.add(actividad)
        }
        actividadesSeleccionadas.value = actual
    }

    // --- UBICACIÓN ---
    val direccionTexto = MutableLiveData<String>()
    val latitud = MutableLiveData<Double>()
    val longitud = MutableLiveData<Double>()
    val esDireccionValida = direccionTexto.map { !it.isNullOrEmpty() }

    // --- DETALLES ---
    val titulo = MutableLiveData<String>("")
    val precio = MutableLiveData<String>("")

    val huespedes = MutableLiveData<Int>(4)
    val habitaciones = MutableLiveData<Int>(2)
    val camas = MutableLiveData<Int>(2)
    val banos = MutableLiveData<Int>(1)

    // MediatorLiveData observa cambios en título y precio para habilitar el botón "Siguiente"
    // solo cuando ambos campos contienen texto.
    val sonDetallesValidos = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        fun validar() {
            val t = titulo.value
            val p = precio.value
            value = !t.isNullOrEmpty() && !p.isNullOrEmpty()
        }
        addSource(titulo) { validar() }
        addSource(precio) { validar() }
    }

    // --- COMODIDADES ---
    val comodidadesSeleccionadas = MutableLiveData<MutableSet<String>>(mutableSetOf())

    fun toggleComodidad(comodidad: String) {
        val actual = comodidadesSeleccionadas.value ?: mutableSetOf()
        if (actual.contains(comodidad)) {
            actual.remove(comodidad)
        } else {
            actual.add(comodidad)
        }
        comodidadesSeleccionadas.value = actual
    }

    // --- FOTOS ---
    val fotosSeleccionadas = MutableLiveData<List<Uri>>(emptyList())

    fun agregarFotos(nuevasUris: List<Uri>) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        fotosSeleccionadas.value = actual + nuevasUris
    }

    fun eliminarFoto(uri: Uri) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        fotosSeleccionadas.value = actual - uri
    }
}