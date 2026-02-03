package com.intelliworks.intellihome

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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

class AddPropertyViewModel(private val state: SavedStateHandle) : ViewModel() {

    // --- CONEXIÓN CON EL SERVIDOR ---
    private val catalogosApi = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
    private val repoCatalogos = CatalogosRepository(catalogosApi)

    // --- LISTAS DISPONIBLES ---
    private val _listaTipos = MutableLiveData<List<TipoCasaDto>>()
    val listaTipos: LiveData<List<TipoCasaDto>> = _listaTipos

    private val _listaHobbies = MutableLiveData<List<HobbyDto>>()
    val listaHobbies: LiveData<List<HobbyDto>> = _listaHobbies

    private val _listaAmenidades = MutableLiveData<List<AmenidadDto>>()
    val listaAmenidades: LiveData<List<AmenidadDto>> = _listaAmenidades

    val diasSeleccionados = state.getLiveData<ArrayList<String>>("dias_disponibles", arrayListOf())

    // --- SELECCIONES DEL USUARIO (CON PERSISTENCIA) ---

    // TIPO DE PROPIEDAD
    val tipoSeleccionadoId = state.getLiveData<Int>("tipo_id")
    val esTipoValido = tipoSeleccionadoId.map { it != null && it > 0 }

    // ACTIVIDADES (HOBBIES)
    val hobbiesSeleccionadosIds = state.getLiveData<ArrayList<Int>>("hobbies_ids", arrayListOf())

    val hayActividades = hobbiesSeleccionadosIds.map { it.isNotEmpty() }

    // COMODIDADES (AMENIDADES)
    val amenidadesSeleccionadasIds = state.getLiveData<ArrayList<Int>>("amenidades_ids", arrayListOf())

    // --- INICIALIZACIÓN ---
    init {
        cargarCatalogos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            try {
                _listaTipos.value = repoCatalogos.obtenerTipos()
                _listaHobbies.value = repoCatalogos.obtenerHobbies()
                _listaAmenidades.value = repoCatalogos.obtenerAmenidades()
            } catch (e: Exception) {
                // Manejar error de red si es necesario
                e.printStackTrace()
            }
        }
    }

    // --- FUNCIONES DE SELECCIÓN ---

    fun setTipo(id: Int) {
        tipoSeleccionadoId.value = id // Al asignar valor, se guarda en state automáticamente
    }

    fun toggleDia(dia: String, isSelected: Boolean) {
        val currentList = diasSeleccionados.value ?: arrayListOf()

        if (isSelected) {
            if (!currentList.contains(dia)) currentList.add(dia)
        } else {
            currentList.remove(dia)
        }

        diasSeleccionados.value = ArrayList(currentList)
    }

    fun getDiasComoStringCSV(): String {
        val list = diasSeleccionados.value ?: emptyList()
        return list.joinToString(",") // Retorna "Lunes,Martes,Viernes"
    }

    fun toggleHobby(id: Int) {
        val currentList = hobbiesSeleccionadosIds.value ?: arrayListOf()

        if (currentList.contains(id)) {
            currentList.remove(id)
        } else {
            currentList.add(id)
        }
        hobbiesSeleccionadosIds.value = ArrayList(currentList)
    }

    fun toggleAmenidad(id: Int) {
        val currentList = amenidadesSeleccionadasIds.value ?: arrayListOf()
        if (currentList.contains(id)) {
            currentList.remove(id)
        } else {
            currentList.add(id)
        }
        amenidadesSeleccionadasIds.value = ArrayList(currentList)
    }

    // --- UBICACIÓN (PERSISTENTE) ---
    val direccionTexto = state.getLiveData<String>("direccion", "")
    val latitud = state.getLiveData<Double>("latitud", 9.9333)
    val longitud = state.getLiveData<Double>("longitud", -84.0833)
    val esDireccionValida = direccionTexto.map { !it.isNullOrEmpty() }

    // --- DETALLES DE LA PUBLICACIÓN (PERSISTENTE) ---
    // Definimos valores por defecto en el segundo parámetro
    val titulo = state.getLiveData<String>("titulo", "")
    val precio = state.getLiveData<String>("precio", "")
    val descripcion = state.getLiveData<String>("descripcion", "")
    val reglas = state.getLiveData<String>("reglas", "")

    val huespedes = state.getLiveData<Int>("huespedes", 4)
    val habitaciones = state.getLiveData<Int>("habitaciones", 2)
    val camas = state.getLiveData<Int>("camas", 2)
    val banos = state.getLiveData<Int>("banos", 1)

    // Validación
    val sonDetallesValidos = MediatorLiveData<Boolean>().apply {
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

    // --- FOTOS (PERSISTENTE) ---
    private val _fotosUrisStrings = state.getLiveData<ArrayList<String>>("fotos_uris", arrayListOf())

    val fotosSeleccionadas: LiveData<List<Uri>> = _fotosUrisStrings.map { listaStrings ->
        listaStrings.map { Uri.parse(it) }
    }

    fun agregarFotos(nuevasUris: List<Uri>) {
        val listaActual = _fotosUrisStrings.value ?: arrayListOf()
        listaActual.addAll(nuevasUris.map { it.toString() })
        _fotosUrisStrings.value = ArrayList(listaActual)
    }

    fun eliminarFoto(uri: Uri) {
        val listaActual = _fotosUrisStrings.value ?: arrayListOf()
        val uriString = uri.toString()
        if (listaActual.contains(uriString)) {
            listaActual.remove(uriString)
            _fotosUrisStrings.value = ArrayList(listaActual)
        }
    }

    // --- HELPER PARA OBTENER NOMBRES ---
    fun getNombreTipoSeleccionado(): String {
        val id = tipoSeleccionadoId.value ?: return "No seleccionado"
        return _listaTipos.value?.find { it.id == id }?.nombre ?: "Desconocido"
    }

    fun getNombresHobbiesSeleccionados(): String {
        val ids = hobbiesSeleccionadosIds.value ?: emptyList()
        val lista = _listaHobbies.value ?: emptyList()
        return lista.filter { ids.contains(it.id) }.joinToString(", ") { it.nombre }
    }

    fun getNombresAmenidadesSeleccionadas(): String {
        val ids = amenidadesSeleccionadasIds.value ?: emptyList()
        val lista = _listaAmenidades.value ?: emptyList()
        return lista.filter { ids.contains(it.id) }.joinToString(", ") { it.nombre }
    }
}