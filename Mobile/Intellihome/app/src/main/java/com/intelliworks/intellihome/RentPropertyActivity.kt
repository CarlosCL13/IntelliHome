package com.intelliworks.intellihome

import android.content.Intent
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

class RentPropertyActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRentPropertyBinding
    private lateinit var currentProperty: Property
    private var map: GoogleMap? = null

    // Variable para guardar las fechas bloqueadas que vienen del server
    private var blockedDatesJson: String = "[]"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val propertyJson = intent.getStringExtra("property_data")
        val isRentalActive = intent.getBooleanExtra("is_rental_active", false)

        if (propertyJson != null) {
            currentProperty = Gson().fromJson(propertyJson, Property::class.java)

            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapFragment) as SupportMapFragment
            mapFragment.getMapAsync(this)

            configurarVista(currentProperty, isRentalActive)
            cargarDetallesCompletos(currentProperty.id.toIntOrNull() ?: 0)
            determinarRolYConfigurarBotones(currentProperty, isRentalActive, null)
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isMapToolbarEnabled = false
        actualizarMapa(currentProperty.latitud, currentProperty.longitud)
    }

    private fun actualizarMapa(lat: Double, lon: Double) {
        if (map != null && lat != 0.0 && lon != 0.0) {
            val ubicacion = LatLng(lat, lon)
            map?.clear()
            map?.addMarker(MarkerOptions().position(ubicacion).title(currentProperty.titulo))
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f))
        }
    }

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
        }
    }

    private fun cargarDetallesCompletos(idPropiedad: Int) {
        if (idPropiedad == 0) return
        val api = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
        val repo = PropiedadRepository(api)

        lifecycleScope.launch {
            try {
                val response = repo.obtenerDetalle(idPropiedad)
                if (response.isSuccessful && response.body() != null) {
                    val detalle = response.body()!!

                    // 1. Textos básicos
                    binding.tvDescripcionPropiedad.text = detalle.descripcion ?: "Sin descripción"
                    binding.tvReglasPropiedad.text = detalle.reglas ?: "Sin reglas específicas"
                    val nombreHost = detalle.nombreHost ?: detalle.usuario ?: "Anfitrión"
                    binding.tvPropietario.text = getString(R.string.fmt_host_name, nombreHost)

                    if (!detalle.fotoHost.isNullOrEmpty()) {
                        Glide.with(this@RentPropertyActivity).load(detalle.fotoHost).circleCrop()
                            .into(binding.imgPropietario)
                    }

                    // 2. Amenidades y Hobbies
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

                    // 4. Actualización de Propiedad y Mapa
                    if (detalle.usuarioId != null) {
                        currentProperty =
                            currentProperty.copy(userId = detalle.usuarioId.toString())
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

                    // 5. Re-evaluar botones
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
                4 -> getString(R.string.am_wifi)
                5 -> getString(R.string.am_cable_tv)
                6 -> getString(R.string.am_washer_dryer)
                7 -> getString(R.string.am_pool)
                8 -> getString(R.string.am_garden)
                9 -> getString(R.string.am_bbq)
                10 -> getString(R.string.am_balcony)
                11 -> getString(R.string.am_gym)
                12 -> getString(R.string.am_parking)
                13 -> getString(R.string.am_security)
                14 -> getString(R.string.am_ensuite)
                15 -> getString(R.string.am_outdoor_furniture)
                16 -> getString(R.string.am_microwave)
                17 -> getString(R.string.am_dishwasher)
                18 -> getString(R.string.am_coffee_maker)
                19 -> getString(R.string.am_linens)
                20 -> getString(R.string.am_common_areas)
                21 -> getString(R.string.am_extra_beds)
                22 -> getString(R.string.am_cleaning)
                23 -> getString(R.string.am_public_transport)
                24 -> getString(R.string.am_pets)
                25 -> getString(R.string.am_shops)
                26 -> getString(R.string.am_floor_heating)
                27 -> getString(R.string.am_workspace)
                28 -> getString(R.string.am_entertainment)
                29 -> getString(R.string.am_fireplace)
                30 -> getString(R.string.am_internet_high_speed)
                else -> nombreOriginal
            }
        }
    }

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

            // Obtener usuario actual
            val usuarioId = SessionManager.obtenerUserId(this).toIntOrNull() ?: 0
            val propiedadId = prop.id.toIntOrNull() ?: 0
            if (usuarioId != 0 && propiedadId != 0) {
                obtenerYMostrarDesglose(propiedadId, usuarioId, formatoMoneda)
            }
        } else {
            // Ocultar desglose y mostrar precio por noche
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
                    
                    // Ocultar campos de precio por noche y noches ya que no están en el DTO
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
            binding.btnAlquilarPropiedad.visibility = View.GONE
            binding.btnDomotica.visibility = View.VISIBLE
            binding.btnDomotica.setOnClickListener { irADomotica() }
        } else {
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

        intent.putExtra("PROPERTY_PRICE", precioDouble)
        intent.putExtra("PROPERTY_ID", idPropiedad)

        // --- LOGS DE DEPURACIÓN ---
        android.util.Log.e("DEBUG_FECHAS", "--- Saliendo de RentPropertyActivity ---")
        android.util.Log.e("DEBUG_FECHAS", "JSON a enviar: $blockedDatesJson")
        // --------------------------

        intent.putExtra("BLOCKED_DATES", blockedDatesJson)
        startActivity(intent)
    }

    private fun irADomotica() {
        val intent = Intent(this, LightControlActivity::class.java)
        val idReal = currentProperty.id.toIntOrNull() ?: 0
        intent.putExtra("PROPERTY_ID", idReal)
        startActivity(intent)
    }
}