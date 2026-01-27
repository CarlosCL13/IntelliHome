package com.intelliworks.intellihome

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.utils.FilterBottomSheet
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.regex.Pattern

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: PropertyAdapter
    private var listaGlobal: List<Property> = emptyList()

    // ESTADO DE FILTROS
    private var filtroTexto: String = ""
    private var globalMaxPrice: Float = 0f
    private var globalMaxGuests: Float = 0f

    private var selectedMaxPrice: Float? = null
    private var selectedMinGuests: Float? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerExplore)
        val searchLayout = view.findViewById<TextInputLayout>(R.id.searchLayout)
        val etSearch = view.findViewById<TextInputEditText>(R.id.etSearch)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Inicializamos adaptador vacío
        adapter = PropertyAdapter(emptyList())
        recycler.adapter = adapter

        // CARGAR DATOS DESDE EL SERVIDOR
        cargarDatosDesdeServidor()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtroTexto = s.toString()
                aplicarFiltros()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchLayout.setEndIconOnClickListener {
            mostrarFiltros()
        }
    }

    override fun onResume() {
        super.onResume()
        // Opcional: Recargar al volver si quieres datos frescos siempre
        if (listaGlobal.isEmpty()) {
            cargarDatosDesdeServidor()
        }
    }

    private fun cargarDatosDesdeServidor() {
        val api = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
        val repo = PropiedadRepository(api)

        lifecycleScope.launch {
            try {
                val response = repo.obtenerTodas()
                if (response.isSuccessful) {
                    val listaDto = response.body() ?: emptyList()

                    // --- MAPEO DE DTO A PROPERTY ---
                    // Convertimos lo que llega del servidor al objeto interno de la App
                    listaGlobal = listaDto.map { dto ->
                        // Construimos el string CSV para que PropertyUtils lo formatee
                        val capacidadCsv = "${dto.huespedes},${dto.habitaciones},${dto.camas},${dto.banos}"

                        // Lista de imágenes (manejamos nulos)
                        val imagenes = if (dto.imagen != null) listOf(dto.imagen) else emptyList()

                        Property(
                            id = dto.id.toString(),
                            userId = "0", // Dato no disponible en resumen
                            nombreUsuario = "Anfitrión", // Dato no disponible en resumen
                            titulo = dto.titulo,
                            precio = dto.precio.toString(), // Convertimos double a string
                            direccion = "Ubicación en mapa", // Dato no disponible en resumen
                            tipo = "Casa", // Valor por defecto
                            capacidad = capacidadCsv, // "6,3,3,2"
                            imagenes = imagenes,
                            descripcion = "Ver detalles para más información.",
                            actividades = "",
                            comodidades = "",
                            reglas = "",
                            rentedByUserId = null // Asumimos disponible para Explorar
                        )
                    }

                    // Calculamos límites para los filtros y mostramos
                    calcularLimitesDeDatos(listaGlobal)
                    aplicarFiltros()

                } else {
                    Toast.makeText(requireContext(), "Error al cargar propiedades", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calcularLimitesDeDatos(lista: List<Property>) {
        if (lista.isEmpty()) {
            globalMaxPrice = 100000f // Valor base por defecto
            globalMaxGuests = 10f
            return
        }

        val maxPrecioEncontrado = lista.maxOfOrNull { limpiarPrecio(it.precio) }?.toFloat() ?: 0f
        globalMaxPrice = if (maxPrecioEncontrado > 0) maxPrecioEncontrado else 100000f

        val maxHuespedesEncontrado = lista.maxOfOrNull { limpiarCapacidad(it.capacidad) }?.toFloat() ?: 0f
        globalMaxGuests = if (maxHuespedesEncontrado > 0) maxHuespedesEncontrado else 10f

        if (selectedMaxPrice == null) selectedMaxPrice = globalMaxPrice
        if (selectedMinGuests == null) selectedMinGuests = 1f
    }

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

    private fun aplicarFiltros() {
        val maxP = selectedMaxPrice ?: globalMaxPrice
        val minG = selectedMinGuests ?: 1f

        val queryNormalizada = filtroTexto.normalizar()

        val listaFiltrada = listaGlobal.filter { prop ->
            // 1. Texto
            val coincideTexto = if (filtroTexto.isEmpty()) true else {
                val tituloNorm = prop.titulo.normalizar()
                // La dirección en el resumen es genérica, así que buscamos principalmente por título
                tituloNorm.contains(queryNormalizada)
            }

            // 2. Precio
            val precioProp = limpiarPrecio(prop.precio)
            val coincidePrecio = precioProp <= maxP

            // 3. Huéspedes
            val capProp = limpiarCapacidad(prop.capacidad)
            val coincideHuespedes = capProp >= minG

            coincideTexto && coincidePrecio && coincideHuespedes
        }

        adapter.updateList(listaFiltrada)
    }

    // --- Helpers ---

    private fun limpiarPrecio(precioStr: String): Double {
        val soloNumeros = precioStr.replace("[^\\d.]".toRegex(), "")
        return soloNumeros.toDoubleOrNull() ?: 0.0
    }

    private fun limpiarCapacidad(capacidadStr: String): Int {
        // Extraemos el primer número del CSV "6,3,3,2" -> "6"
        val primerNumero = capacidadStr.split(",").firstOrNull()
        return primerNumero?.toIntOrNull() ?: 0
    }

    private fun String.normalizar(): String {
        val nfdNormalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase()
    }
}