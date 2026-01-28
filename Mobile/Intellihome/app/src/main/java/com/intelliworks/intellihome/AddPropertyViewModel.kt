package com.intelliworks.intellihome

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.model.AmenidadDto
import com.intelliworks.intellihome.data.model.HobbyDto
import com.intelliworks.intellihome.data.model.TipoCasaDto
import com.intelliworks.intellihome.data.repository.CatalogosRepository
import kotlinx.coroutines.launch

class AddPropertyViewModel : ViewModel() {

    // --- CONEXIÓN CON EL SERVIDOR ---
    private val catalogosApi = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
    private val repoCatalogos = CatalogosRepository(catalogosApi)

    // --- LISTAS DISPONIBLES (Vienen de la Base de Datos) ---
    private val _listaTipos = MutableLiveData<List<TipoCasaDto>>()
    val listaTipos: LiveData<List<TipoCasaDto>> = _listaTipos

    private val _listaHobbies = MutableLiveData<List<HobbyDto>>()
    val listaHobbies: LiveData<List<HobbyDto>> = _listaHobbies

    private val _listaAmenidades = MutableLiveData<List<AmenidadDto>>()
    val listaAmenidades: LiveData<List<AmenidadDto>> = _listaAmenidades

    // --- SELECCIONES DEL USUARIO (Guardamos IDs para enviar al Backend) ---

    // TIPO DE PROPIEDAD
    val tipoSeleccionadoId = MutableLiveData<Int>()
    val esTipoValido = tipoSeleccionadoId.map { it != null && it > 0 }

    // ACTIVIDADES (HOBBIES)
    val hobbiesSeleccionadosIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())
    val hayActividades = hobbiesSeleccionadosIds.map { it.isNotEmpty() }

    // COMODIDADES (AMENIDADES)
    val amenidadesSeleccionadasIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())

    // --- INICIALIZACIÓN: Descargar listas al abrir ---
    init {
        cargarCatalogos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            // Descargamos las 3 listas en paralelo (o secuencial rápido)
            _listaTipos.value = repoCatalogos.obtenerTipos()
            _listaHobbies.value = repoCatalogos.obtenerHobbies()
            _listaAmenidades.value = repoCatalogos.obtenerAmenidades()
        }
    }

    // --- FUNCIONES DE SELECCIÓN ---

    fun setTipo(id: Int) {
        tipoSeleccionadoId.value = id
    }

    fun toggleHobby(id: Int) {
        val actual = hobbiesSeleccionadosIds.value ?: mutableSetOf()
        if (actual.contains(id)) {
            actual.remove(id)
        } else {
            actual.add(id)
        }
        hobbiesSeleccionadosIds.value = actual // Notifica a la vista
    }

    fun toggleAmenidad(id: Int) {
        val actual = amenidadesSeleccionadasIds.value ?: mutableSetOf()
        if (actual.contains(id)) {
            actual.remove(id)
        } else {
            actual.add(id)
        }
        amenidadesSeleccionadasIds.value = actual
    }

    // --- UBICACIÓN (Se mantiene igual) ---
    val direccionTexto = MutableLiveData<String>()
    val latitud = MutableLiveData<Double>()
    val longitud = MutableLiveData<Double>()
    val esDireccionValida = direccionTexto.map { !it.isNullOrEmpty() }

    // --- DETALLES DE LA PUBLICACIÓN (Se mantiene igual) ---
    val titulo = MutableLiveData<String>("")
    val precio = MutableLiveData<String>("")
    val descripcion = MutableLiveData<String>("")
    val reglas = MutableLiveData<String>("")

    val huespedes = MutableLiveData<Int>(4)
    val habitaciones = MutableLiveData<Int>(2)
    val camas = MutableLiveData<Int>(2)
    val banos = MutableLiveData<Int>(1)

    // Validación para habilitar botón "Siguiente"
    val sonDetallesValidos = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        fun validar() {
            val t = titulo.value
            val p = precio.value
            val d = descripcion.value
            val r = reglas.value
            value = !t.isNullOrEmpty() && !p.isNullOrEmpty() && !d.isNullOrEmpty() && !r.isNullOrEmpty()
        }
        addSource(titulo) { validar() }
        addSource(precio) { validar() }
        addSource(descripcion) { validar() }
        addSource(reglas) { validar() }
    }

    // --- FOTOS (Se mantiene igual) ---
    val fotosSeleccionadas = MutableLiveData<List<Uri>>(emptyList())

    fun agregarFotos(nuevasUris: List<Uri>) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        fotosSeleccionadas.value = actual + nuevasUris
    }

    fun eliminarFoto(uri: Uri) {
        val actual = fotosSeleccionadas.value ?: emptyList()
        fotosSeleccionadas.value = actual - uri
    }

    // --- HELPER PARA OBTENER NOMBRES (Útil para el Resumen Final) ---
    fun getNombreTipoSeleccionado(): String {
        val id = tipoSeleccionadoId.value ?: return "No seleccionado"
        return _listaTipos.value?.find { it.id == id }?.nombre ?: "Desconocido"
    }

    fun getNombresHobbiesSeleccionados(): String {
        val ids = hobbiesSeleccionadosIds.value ?: emptySet()
        val lista = _listaHobbies.value ?: emptyList()
        return lista.filter { ids.contains(it.id) }.joinToString(", ") { it.nombre }
    }

    fun getNombresAmenidadesSeleccionadas(): String {
        val ids = amenidadesSeleccionadasIds.value ?: emptySet()
        val lista = _listaAmenidades.value ?: emptyList()
        return lista.filter { ids.contains(it.id) }.joinToString(", ") { it.nombre }
    }
}