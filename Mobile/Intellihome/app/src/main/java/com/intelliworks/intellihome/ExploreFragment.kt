package com.intelliworks.intellihome

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import java.text.Normalizer
import java.util.regex.Pattern

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: PropertyAdapter
    private lateinit var listaGlobal: List<Property>

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

        // 1. CARGAR DATOS (Inicial)
        cargarDatosYFiltrarRentadas()

        // Inicializar adaptador
        adapter = PropertyAdapter(listaGlobal)
        recycler.adapter = adapter

        // Listener de Búsqueda
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtroTexto = s.toString()
                aplicarFiltros()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Listener de Filtro
        searchLayout.setEndIconOnClickListener {
            mostrarFiltros()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos al volver (por si se rentó una casa recientemente)
        cargarDatosYFiltrarRentadas()
        if (::adapter.isInitialized) {
            aplicarFiltros()
        }
    }

    /**
     * Carga todas las propiedades y EXCLUYE las que ya están alquiladas.
     */
    private fun cargarDatosYFiltrarRentadas() {
        val todasLasPropiedades = PropertyRepository.getProperties(requireContext())

        // FILTRO CLAVE: Solo propiedades DISPONIBLES (rentedByUserId == null)
        listaGlobal = todasLasPropiedades.filter { it.rentedByUserId == null }

        // Recalcular límites para los sliders basados solo en lo disponible
        calcularLimitesDeDatos(listaGlobal)
    }

    private fun calcularLimitesDeDatos(lista: List<Property>) {
        if (lista.isEmpty()) {
            globalMaxPrice = 1000f
            globalMaxGuests = 10f
            return
        }

        val maxPrecioEncontrado = lista.maxOfOrNull { limpiarPrecio(it.precio) }?.toFloat() ?: 0f
        globalMaxPrice = maxPrecioEncontrado

        val maxHuespedesEncontrado = lista.maxOfOrNull { limpiarCapacidad(it.capacidad) }?.toFloat() ?: 0f
        globalMaxGuests = maxHuespedesEncontrado

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
                val direccionNorm = prop.direccion.normalizar()
                tituloNorm.contains(queryNormalizada) || direccionNorm.contains(queryNormalizada)
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

    private fun limpiarPrecio(precioStr: String): Double {
        val soloNumeros = precioStr.replace("[^\\d.]".toRegex(), "")
        return soloNumeros.toDoubleOrNull() ?: 0.0
    }

    private fun limpiarCapacidad(capacidadStr: String): Int {
        val match = "\\d+".toRegex().find(capacidadStr)
        return match?.value?.toIntOrNull() ?: 0
    }

    private fun String.normalizar(): String {
        val nfdNormalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase()
    }
}