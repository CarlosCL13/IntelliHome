package com.intelliworks.intellihome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.intelliworks.intellihome.data.model.PropiedadAlquiladaDto
import com.intelliworks.intellihome.data.repository.ArrendamientoRepository
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.PropertyUtils
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragmento "Mis Rentas".
 * Muestra las propiedades alquiladas activas, con dirección geocodificada y capacidad formateada.
 */
class RentalsFragment : Fragment(R.layout.fragment_rentals) {

    private val TAG = "RentalsFragment"

    // UI Components
    private lateinit var adapter: PropertyAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var recycler: RecyclerView

    // Data Repository
    private lateinit var repo: ArrendamientoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ArrendamientoRepository()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recyclerRentals)
        emptyState = view.findViewById(R.id.emptyState)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PropertyAdapter(emptyList()) { property ->
            navigateToPropertyDetails(property)
        }
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadUserRentals()
    }

    private fun loadUserRentals() {
        // Capturamos el contexto de forma segura antes de la corrutina
        val context = requireContext()
        val myUserIdStr = SessionManager.obtenerUserId(context)
        val myUserId = myUserIdStr.toIntOrNull()

        if (myUserId == null || myUserId == 0) {
            showEmptyState()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Llamada a la API
                val response = repo.obtenerAlquiladasPorUsuario(myUserId)

                if (response.isSuccessful) {
                    val rentalsDto = response.body() ?: emptyList()

                    if (rentalsDto.isNotEmpty()) {

                        // 2. Procesamiento pesado en hilo secundario (IO)
                        val rentalsUi = withContext(Dispatchers.IO) {
                            rentalsDto.map { dto ->
                                convertirDtoAProperty(dto, myUserIdStr, context)
                            }
                        }

                        // 3. Actualizar UI en hilo principal
                        recycler.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                        adapter.updateList(rentalsUi)
                    } else {
                        showEmptyState()
                    }
                } else {
                    // CORRECCIÓN AQUÍ: No usamos 'e', usamos response.code()
                    Log.e(TAG, "Error Servidor: ${response.code()}")
                    showEmptyState()
                }
            } catch (e: Exception) {
                // Aquí 'e' SÍ existe porque estamos en el bloque catch
                Log.e(TAG, "Excepción de Red: ${e.message}", e)
                showEmptyState()
            }
        }
    }

    /**
     * Helper para convertir el DTO usando PropertyUtils (Dirección real + Capacidad formateada).
     */
    private fun convertirDtoAProperty(
        dto: PropiedadAlquiladaDto,
        userId: String,
        context: Context
    ): Property {

        // A) Formatear Capacidad
        val rawCapacity = "${dto.huespedes},${dto.habitaciones},${dto.camas},${dto.banos}"
        val formattedCapacity = PropertyUtils.getFormattedCapacity(context, rawCapacity)

        // B) Obtener Dirección Real
        val formattedAddress = PropertyUtils.getAddressFromCoordinates(context, dto.latitud, dto.longitud)

        // C) Imágenes
        val imagesList = if (!dto.imagen.isNullOrEmpty()) listOf(dto.imagen) else emptyList()

        return Property(
            id = dto.id.toString(),
            userId = "0",
            titulo = dto.titulo ?: "Sin título",
            precio = dto.precio?.toString() ?: "0.0",
            direccion = formattedAddress,
            tipo = "Alquiler Activo",
            capacidad = formattedCapacity,
            imagenes = imagesList,
            descripcion = "Propiedad en alquiler. Toca para ver controles.",
            actividades = "",
            comodidades = "",
            reglas = "",
            rentedByUserId = userId,

            // --- AQUÍ ESTABA EL ERROR: FALTABA ASIGNAR LAS FECHAS ---
            fechaInicio = dto.fechaInicio,
            fechaFin = dto.fechaFin
            // --------------------------------------------------------
        )
    }

    private fun navigateToPropertyDetails(property: Property) {
        val intent = Intent(requireContext(), RentPropertyActivity::class.java)
        val gson = Gson()
        intent.putExtra("property_data", gson.toJson(property))
        intent.putExtra("is_rental_active", false)
        startActivity(intent)
    }

    private fun showEmptyState() {
        if (isAdded) { // Verificación de seguridad
            recycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        }
    }
}