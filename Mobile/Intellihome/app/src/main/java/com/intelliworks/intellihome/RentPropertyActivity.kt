package com.intelliworks.intellihome

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.databinding.ActivityRentPropertyBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.ImagePagerAdapter
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyUtils
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity encargada de mostrar el detalle completo de una propiedad para renta.
 * Gestiona la visualización de imágenes, mapa, detalles del anfitrión,
 * reglas, amenidades y la lógica de bloqueo de fechas/días antes de alquilar.
 */
class RentPropertyActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRentPropertyBinding
    private lateinit var currentProperty: Property
    private var map: GoogleMap? = null

    // Variable para guardar las fechas específicas ya reservadas (bloqueadas) que vienen del server
    private var blockedDatesJson: String = "[]"

    // Lista de días de la semana permitidos para renta (ej: ["Lunes", "Viernes"])
    private var allowedDaysList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar datos enviados desde la actividad anterior
        val propertyJson = intent.getStringExtra("property_data")
        val isRentalActive = intent.getBooleanExtra("is_rental_active", false)

        if (propertyJson != null) {
            currentProperty = Gson().fromJson(propertyJson, Property::class.java)

            // Inicializar mapa
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapFragment) as SupportMapFragment
            mapFragment.getMapAsync(this)

            // Configurar UI inicial
            configurarVista(currentProperty, isRentalActive)

            // Cargar datos frescos del servidor (incluyendo bloqueos y disponibilidad)
            cargarDetallesCompletos(currentProperty.id.toIntOrNull() ?: 0)

            determinarRolYConfigurarBotones(currentProperty, isRentalActive, null)

            // Listener para abrir Google Maps externo
            binding.clickOverlay.setOnClickListener {
                abrirNavegacionExterna(currentProperty.latitud, currentProperty.longitud)
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.error_loading_property_data),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    /**
     * Callback cuando el mapa está listo. Configura marcadores y cámara.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isMapToolbarEnabled = false
        actualizarMapa(currentProperty.latitud, currentProperty.longitud)
    }

    /**
     * Mueve la cámara del mapa a la ubicación de la propiedad y pone un marcador.
     */
    private fun actualizarMapa(lat: Double, lon: Double) {
        if (map != null && lat != 0.0 && lon != 0.0) {
            val ubicacion = LatLng(lat, lon)
            map?.clear()
            map?.addMarker(MarkerOptions().position(ubicacion).title(currentProperty.titulo))
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f))
        }
    }

    /**
     * Abre la aplicación externa de mapas (Google Maps / Waze) para navegar.
     */
    private fun abrirNavegacionExterna(lat: Double, lon: Double) {
        if (lat == 0.0 || lon == 0.0) return
        try {
            val label = "Ubicación de Propiedad"
            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Realiza la petición al backend para obtener todos los detalles de la propiedad.
     * Incluye: Fotos, Amenidades, Hobbies, Host, Fechas ocupadas y Días permitidos.
     */
    private fun cargarDetallesCompletos(idPropiedad: Int) {
        if (idPropiedad == 0) return
        val api = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
        val repo = PropiedadRepository(api)

        lifecycleScope.launch {
            try {
                val response = repo.obtenerDetalle(idPropiedad)
                if (response.isSuccessful && response.body() != null) {
                    val detalle = response.body()!!

                    // 1. Llenar textos básicos
                    binding.tvDescripcionPropiedad.text = detalle.descripcion ?: "Sin descripción"
                    binding.tvReglasPropiedad.text = detalle.reglas ?: "Sin reglas específicas"

                    val nombreHost = detalle.nombreHost ?: detalle.usuario ?: "Anfitrión"
                    binding.tvPropietario.text = getString(R.string.fmt_host_name, nombreHost)

                    if (!detalle.fotoHost.isNullOrEmpty()) {
                        Glide.with(this@RentPropertyActivity).load(detalle.fotoHost).circleCrop()
                            .into(binding.imgPropietario)
                    }

                    // 2. Mapear y mostrar Amenidades y Hobbies
                    val amenidadesTraducidas =
                        detalle.amenidades?.map { obtenerNombrePorId(it.id, it.nombre, false) }
                    binding.tvComodidadesPropiedad.text =
                        amenidadesTraducidas?.joinToString(", ") ?: getString(R.string.label_none)

                    val hobbiesTraducidos =
                        detalle.hobbies?.map { obtenerNombrePorId(it.id, it.nombre, true) }
                    binding.tvActividadesPropiedad.text =
                        hobbiesTraducidos?.joinToString(", ") ?: getString(R.string.label_none)

                    // 3. CAPTURA DE FECHAS OCUPADAS (CRÍTICO)
                    if (!detalle.futurosArrendamientos.isNullOrEmpty()) {
                        // Serializamos la lista tal cual viene del server para enviarla a PaymentActivity
                        blockedDatesJson = Gson().toJson(detalle.futurosArrendamientos)
                        android.util.Log.d("FECHAS_DEBUG", "Fechas capturadas: $blockedDatesJson")
                    }

                    // 4. CAPTURA Y VISUALIZACIÓN DE DÍAS DISPONIBLES (NUEVO)
                    // Si el backend envía la lista de días permitidos (ej: ["Lunes", "Martes"])
                    if (detalle.diasDisponibles != null) {
                        allowedDaysList = ArrayList(detalle.diasDisponibles)
                        mostrarDiasDisponibles(allowedDaysList)
                    }

                    // 5. Actualización de datos de la Propiedad y Mapa
                    if (detalle.usuarioId != null) {
                        currentProperty = currentProperty.copy(userId = detalle.usuarioId.toString())
                    }
                    if (detalle.latitud != null && detalle.longitud != null) {
                        currentProperty = currentProperty.copy(
                            latitud = detalle.latitud,
                            longitud = detalle.longitud
                        )
                        actualizarMapa(detalle.latitud, detalle.longitud)
                    }
                    if (!detalle.fotos.isNullOrEmpty()) {
                        actualizarSliderFotos(detalle.fotos)
                        currentProperty = currentProperty.copy(imagenes = detalle.fotos)
                    }

                    // 6. Re-evaluar estado de botones (Dueño vs Cliente)
                    val isRentalActive = intent.getBooleanExtra("is_rental_active", false)
                    determinarRolYConfigurarBotones(
                        currentProperty,
                        isRentalActive,
                        detalle.inquilinoActualId
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Muestra visualmente los días de la semana permitidos usando Chips.
     * Los chips son de solo lectura, con fondo gris y texto negro.
     */
    private fun mostrarDiasDisponibles(dias: List<String>) {
        try {
            binding.chipGroupDiasDisponibles.removeAllViews()

            if (dias.isEmpty()) {
                binding.tvLabelDias.visibility = View.GONE
                binding.chipGroupDiasDisponibles.visibility = View.GONE
                return
            }

            binding.tvLabelDias.visibility = View.VISIBLE
            binding.chipGroupDiasDisponibles.visibility = View.VISIBLE

            dias.forEach { dia ->
                val chip = Chip(this)
                chip.text = dia
                chip.isClickable = false
                chip.setEnsureMinTouchTargetSize(false)

                // Configuración visual: Fondo Gris Claro, Texto Negro
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
                chip.setTextColor(Color.BLACK)

                binding.chipGroupDiasDisponibles.addView(chip)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Helper para traducir nombres de hobbies/amenidades según el idioma del dispositivo.
     */
    private fun obtenerNombrePorId(id: Int, nombreOriginal: String, esActividad: Boolean): String {
        return if (esActividad) {
            when (id) {
                1 -> getString(R.string.hobby_tv)
                2 -> getString(R.string.hobby_hiking)
                3 -> getString(R.string.hobby_boardgames)
                4 -> getString(R.string.hobby_snorkel)
                5 -> getString(R.string.hobby_sports)
                else -> nombreOriginal
            }
        } else {
            when (id) {
                1 -> getString(R.string.am_kitchen)
                2 -> getString(R.string.am_ac)
                3 -> getString(R.string.am_heating)
                // ... (resto de casos para brevedad en este ejemplo, mantener los originales) ...
                30 -> getString(R.string.am_internet_high_speed)
                else -> nombreOriginal
            }
        }
    }

    /**
     * Configura el ViewPager para el carrusel de imágenes.
     */
    private fun actualizarSliderFotos(nuevasFotos: List<String>) {
        val adapter = ImagePagerAdapter(
            images = nuevasFotos,
            layoutId = R.layout.item_image_slider
        ) { position ->
            abrirVisorDeFotos(nuevasFotos, position)
        }
        binding.viewPagerImages.adapter = adapter
        binding.tvImageCounter.text = "1/${nuevasFotos.size}"
        binding.tvImageCounter.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    /**
     * Llena los campos de la vista con la información básica recibida en el Intent.
     */
    private fun configurarVista(prop: Property, isRentalActive: Boolean) {
        binding.tvTituloPropiedad.text = prop.titulo
        binding.tvCapacidadPropiedad.text = PropertyUtils.getFormattedCapacity(this, prop.capacidad)
        binding.tvDireccionTexto.text = prop.direccion
        binding.tvPropietario.text = "Cargando..."
        binding.tvDescripcionPropiedad.text = "Cargando..."
        val precioDouble = prop.precio.replace(",", "").toDoubleOrNull() ?: 0.0
        val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CR"))

        if (!isRentalActive) {
            // Mostrar desglose y total pagado
            binding.seccionDesglosePago.visibility = View.VISIBLE
            binding.tvTotalPagado.visibility = View.VISIBLE
            binding.tvPrecioNoche.visibility = View.GONE

            // Obtener desglose si ya es una renta activa/pasada
            val usuarioId = SessionManager.obtenerUserId(this).toIntOrNull() ?: 0
            val propiedadId = prop.id.toIntOrNull() ?: 0
            if (usuarioId != 0 && propiedadId != 0) {
                obtenerYMostrarDesglose(propiedadId, usuarioId, formatoMoneda)
            }
        } else {
            // Modo "Para Alquilar": Mostrar precio por noche
            binding.seccionDesglosePago.visibility = View.GONE
            binding.tvTotalPagado.visibility = View.GONE
            binding.tvPrecioPropiedad.text = formatoMoneda.format(precioDouble)
            binding.tvPrecioNoche.visibility = View.VISIBLE
        }

        if (prop.imagenes.isNotEmpty()) {
            actualizarSliderFotos(prop.imagenes)
        } else {
            binding.tvImageCounter.visibility = View.GONE
        }
    }

    /**
     * Obtiene el desglose financiero de un arrendamiento existente.
     */
    private fun obtenerYMostrarDesglose(propiedadId: Int, usuarioId: Int, formatoMoneda: NumberFormat) {
        val api = RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.ArrendamientoApi::class.java)
        lifecycleScope.launch {
            try {
                val response = api.getDesgloseArrendamiento(propiedadId, usuarioId)
                if (response.isSuccessful && response.body() != null) {
                    val desglose = response.body()!!
                    binding.tvSubtotal.text = getString(R.string.label_subtotal) + ": ${formatoMoneda.format(desglose.subtotal)}"
                    binding.tvIva.text = getString(R.string.label_iva) + ": ${formatoMoneda.format(desglose.iva)}"
                    binding.tvComision.text = getString(R.string.label_comision) + ": ${formatoMoneda.format(desglose.comision)}"
                    binding.tvDesgloseTotalPagado.text = getString(R.string.label_total_paid) + ": ${formatoMoneda.format(desglose.total)}"
                    binding.tvPrecioPropiedad.text = formatoMoneda.format(desglose.total)

                    binding.tvDesglosePrecioNoche.visibility = View.GONE
                    binding.tvNoches.visibility = View.GONE
                } else {
                    binding.seccionDesglosePago.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.seccionDesglosePago.visibility = View.GONE
            }
        }
    }

    /**
     * Determina qué botones mostrar según la relación del usuario con la propiedad.
     * (Dueño vs Inquilino vs Visitante).
     */
    private fun determinarRolYConfigurarBotones(
        prop: Property,
        isRentalActive: Boolean,
        inquilinoActualId: Int?
    ) {
        val currentUserId = SessionManager.obtenerUserId(this)
        val esDueno = (prop.userId == currentUserId)
        val esInquilinoActual =
            (inquilinoActualId != null && inquilinoActualId.toString() == currentUserId)

        if (esDueno || isRentalActive || esInquilinoActual) {
            // Si es dueño o ya alquiló: Mostrar Panel IoT
            binding.btnAlquilarPropiedad.visibility = View.GONE
            binding.btnDomotica.visibility = View.VISIBLE
            binding.btnDomotica.setOnClickListener { irADomotica() }
        } else {
            // Si es visitante: Mostrar botón Alquilar
            binding.btnAlquilarPropiedad.visibility = View.VISIBLE
            binding.btnDomotica.visibility = View.GONE
            binding.btnAlquilarPropiedad.setOnClickListener { procesarAlquiler() }
        }
    }

    private fun abrirVisorDeFotos(imagenes: List<String>, position: Int) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putStringArrayListExtra("images", ArrayList(imagenes))
        intent.putExtra("position", position)
        startActivity(intent)
    }

    /**
     * Prepara los datos y navega a la pantalla de Pago (PaymentActivity).
     * Envía la propiedad, el precio, las fechas bloqueadas y los días permitidos.
     */
    private fun procesarAlquiler() {
        val intent = Intent(this, PaymentActivity::class.java)
        val gson = Gson()
        val propertyJson = gson.toJson(currentProperty)
        intent.putExtra("property_data", propertyJson)

        val precioLimpio =
            currentProperty.precio.replace("₡", "").replace("$", "").replace(",", "").trim()
        val precioDouble = precioLimpio.toDoubleOrNull() ?: 0.0
        val idPropiedad = currentProperty.id.toIntOrNull() ?: 0

        intent.putExtra("PROPERTY_PRICE", precioDouble)
        intent.putExtra("PROPERTY_ID", idPropiedad)

        // Enviar JSON de fechas ocupadas (reservas futuras)
        intent.putExtra("BLOCKED_DATES", blockedDatesJson)

        // NUEVO: Enviar lista de días permitidos (Lunes, Martes...) para bloquear en calendario
        intent.putStringArrayListExtra("ALLOWED_DAYS", allowedDaysList)

        startActivity(intent)
    }

    private fun irADomotica() {
        val intent = Intent(this, LightControlActivity::class.java)
        val idReal = currentProperty.id.toIntOrNull() ?: 0
        intent.putExtra("PROPERTY_ID", idReal)
        startActivity(intent)
    }
}