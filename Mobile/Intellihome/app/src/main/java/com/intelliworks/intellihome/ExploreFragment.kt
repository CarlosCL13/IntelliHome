package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.utils.FilterBottomSheet
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.PropertyUtils
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Fragmento encargado de listar y filtrar el catálogo general de propiedades.
 *
 * Características principales:
 * 1. Carga asíncrona de datos con transformación DTO -> UI (incluye Geocodificación inversa).
 * 2. Búsqueda en tiempo real por título y ubicación (Ciudad/Provincia).
 * 3. Filtrado avanzado por rango de precios y capacidad de huéspedes.
 * 4. Gestión de estado para filtros persistentes en la vista.
 */
class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private val TAG = "ExploreFragment"

    // Componentes de UI y Datos
    private lateinit var adapter: PropertyAdapter
    private var listaGlobal: List<Property> = emptyList() // Fuente de verdad de datos
    private lateinit var repoPropiedades: PropiedadRepository

    // Variables de estado para los filtros
    private var filtroTexto: String = ""
    private var globalMaxPrice: Float = 0f
    private var globalMaxGuests: Float = 0f

    // Valores seleccionados actualmente por el usuario
    private var selectedMaxPrice: Float? = null
    private var selectedMinGuests: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialización de dependencias de red
        val api = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
        repoPropiedades = PropiedadRepository(api)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vinculación de vistas
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerExplore)
        val searchLayout = view.findViewById<TextInputLayout>(R.id.searchLayout)
        val etSearch = view.findViewById<TextInputEditText>(R.id.etSearch)

        // Configuración del RecyclerView
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = PropertyAdapter(emptyList()) { property ->
            navigateToPropertyDetails(property)
        }
        recycler.adapter = adapter

        // Carga inicial de datos desde el backend
        cargarDatosDesdeServidor()

        // Configuración del buscador en tiempo real (TextWatcher)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtroTexto = s.toString()
                aplicarFiltros() // Re-aplica filtros cada vez que el texto cambia
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configuración del botón de filtros (Icono al final del buscador)
        searchLayout.setEndIconOnClickListener {
            mostrarFiltros()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos si la lista está vacía o para refrescar disponibilidad
        if (listaGlobal.isEmpty()) {
            cargarDatosDesdeServidor()
        }
    }

    /**
     * Realiza la petición al servidor para obtener todas las propiedades.
     * Utiliza [PropertyUtils] en un hilo secundario para procesar direcciones y formatos
     * sin congelar la interfaz de usuario.
     */
    private fun cargarDatosDesdeServidor() {
        val context = requireContext()
        // ID ficticio o del usuario actual, necesario para el modelo Property
        val currentUserId = SessionManager.obtenerUserId(context).ifEmpty { "0" }

        lifecycleScope.launch {
            try {
                // 1. Obtención de datos crudos (Hilo IO implícito en Retrofit)
                val response = repoPropiedades.obtenerTodas()

                if (response.isSuccessful) {
                    val listaDto = response.body() ?: emptyList()

                    // 2. Transformación pesada (Geocoding + Formateo) en Dispatchers.IO
                    val listaProcesada = withContext(Dispatchers.IO) {
                        listaDto.map { dto ->
                            // Uso centralizado de PropertyUtils para mantener consistencia
                            PropertyUtils.mapDtoToProperty(dto, currentUserId, context)
                        }
                    }

                    // 3. Actualización de UI en Hilo Principal
                    listaGlobal = listaProcesada

                    // Recalcular rangos para los filtros basados en los nuevos datos
                    calcularLimitesDeDatos(listaGlobal)

                    // Aplicar filtros vigentes (por si el usuario ya tenía texto escrito)
                    aplicarFiltros()

                } else {
                    Log.e(TAG, "Error del servidor: ${response.code()}")
                    mostrarToast("Error al cargar propiedades")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red", e)
                mostrarToast("Error de conexión")
            }
        }
    }

    /**
     * Navega a la actividad de detalle de propiedad.
     * @param property Objeto UI completo con la información de la propiedad.
     */
    private fun navigateToPropertyDetails(property: Property) {
        val intent = Intent(requireContext(), RentPropertyActivity::class.java)
        val gson = Gson()
        intent.putExtra("property_data", gson.toJson(property))
        intent.putExtra("is_rental_active", false) // Modo exploración: permite iniciar renta
        startActivity(intent)
    }

    /**
     * Analiza la lista de propiedades para determinar dinámicamente los valores máximos
     * de precio y capacidad, ajustando los sliders del filtro.
     */
    private fun calcularLimitesDeDatos(lista: List<Property>) {
        if (lista.isEmpty()) {
            globalMaxPrice = 10000f
            globalMaxGuests = 10f
            return
        }

        // Extraer precio máximo
        val maxPrecioEncontrado = lista.maxOfOrNull { limpiarPrecio(it.precio) }?.toFloat() ?: 0f
        globalMaxPrice = if (maxPrecioEncontrado > 0) maxPrecioEncontrado else 10000f

        // Extraer capacidad máxima
        val maxHuespedesEncontrado = lista.maxOfOrNull { limpiarCapacidad(it.capacidad) }?.toFloat() ?: 0f
        globalMaxGuests = if (maxHuespedesEncontrado > 0) maxHuespedesEncontrado else 10f

        // Inicializar selección si es la primera vez
        if (selectedMaxPrice == null) selectedMaxPrice = globalMaxPrice
        if (selectedMinGuests == null) selectedMinGuests = 1f
    }

    /**
     * Despliega el BottomSheet para seleccionar rango de precios y huéspedes.
     */
    private fun mostrarFiltros() {
        val bottomSheet = FilterBottomSheet(
            absoluteMaxPrice = globalMaxPrice,
            absoluteMaxGuests = globalMaxGuests,
            currentMaxPrice = selectedMaxPrice ?: globalMaxPrice,
            currentMinGuests = selectedMinGuests ?: 1f,
            onApply = { precio, huespedes ->
                selectedMaxPrice = precio
                selectedMinGuests = huespedes
                aplicarFiltros()
            },
            onReset = {
                selectedMaxPrice = globalMaxPrice
                selectedMinGuests = 1f
                aplicarFiltros()
            }
        )
        bottomSheet.show(parentFragmentManager, "FilterBottomSheet")
    }

    /**
     * Aplica la lógica de filtrado combinando:
     * 1. Texto (Título O Dirección/Ciudad).
     * 2. Precio máximo.
     * 3. Huéspedes mínimos.
     */
    private fun aplicarFiltros() {
        val maxP = selectedMaxPrice ?: globalMaxPrice
        val minG = selectedMinGuests ?: 1f
        val queryNormalizada = filtroTexto.normalizar()

        val listaFiltrada = listaGlobal.filter { prop ->
            // Filtro 1: Texto
            // Buscamos coincidencia en Título O en la Dirección (que ahora contiene la Ciudad gracias a PropertyUtils)
            val coincideTexto = if (filtroTexto.isEmpty()) true else {
                val tituloNorm = prop.titulo.normalizar()
                val direccionNorm = prop.direccion.normalizar()
                tituloNorm.contains(queryNormalizada) || direccionNorm.contains(queryNormalizada)
            }

            // Filtro 2: Precio (Menor o igual al seleccionado)
            val precioProp = limpiarPrecio(prop.precio)
            val coincidePrecio = precioProp <= maxP

            // Filtro 3: Capacidad (Mayor o igual al seleccionado)
            val capProp = limpiarCapacidad(prop.capacidad)
            val coincideHuespedes = capProp >= minG

            coincideTexto && coincidePrecio && coincideHuespedes
        }

        adapter.updateList(listaFiltrada)
    }

    // ============================================================================================
    // MÉTODOS AUXILIARES Y DE LIMPIEZA DE DATOS
    // ============================================================================================

    /**
     * Extrae el valor numérico de un string de precio (ej: "$150.00" -> 150.00).
     */
    private fun limpiarPrecio(precioStr: String): Double {
        // Elimina todo lo que no sea dígito o punto decimal
        val soloNumeros = precioStr.replace("[^\\d.]".toRegex(), "")
        return soloNumeros.toDoubleOrNull() ?: 0.0
    }

    /**
     * Extrae el número de huéspedes de la cadena formateada.
     * Soporta formato CSV ("4,2,1,1") o Texto ("4 Huéspedes · 2 Hab...").
     */
    private fun limpiarCapacidad(capacidadStr: String): Int {
        val pattern = Pattern.compile("\\d+")
        val matcher = pattern.matcher(capacidadStr)

        return if (matcher.find()) {
            matcher.group().toIntOrNull() ?: 0
        } else {
            0
        }
    }

    /**
     * Normaliza cadenas de texto eliminando acentos y convirtiendo a minúsculas
     * para facilitar la búsqueda (ej: "San José" -> "san jose").
     */
    private fun String.normalizar(): String {
        val nfdNormalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase()
    }

    /**
     * Muestra un Toast de forma segura verificando si el fragmento está adjunto.
     */
    private fun mostrarToast(mensaje: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }
}