package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
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

class RentPropertyActivity : BaseActivity() {

    private lateinit var binding: ActivityRentPropertyBinding
    private lateinit var currentProperty: Property

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val propertyJson = intent.getStringExtra("property_data")
        val isRentalActive = intent.getBooleanExtra("is_rental_active", false)

        if (propertyJson != null) {
            currentProperty = Gson().fromJson(propertyJson, Property::class.java)
            configurarVista(currentProperty)

            // --- ESTA LÍNEA ES CLAVE PARA DESCARGAR LA DESCRIPCIÓN ---
            cargarDetallesCompletos(currentProperty.id.toIntOrNull() ?: 0)

            determinarRolYConfigurarBotones(currentProperty, isRentalActive)
        } else {
            Toast.makeText(this, getString(R.string.error_loading_property_data), Toast.LENGTH_SHORT).show()
            finish()
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

                    // Llenamos los textos que faltaban
                    binding.tvDescripcionPropiedad.text = detalle.descripcion ?: "Sin descripción"
                    binding.tvReglasPropiedad.text = detalle.reglas ?: "Sin reglas específicas"

                    val nombreHost = detalle.nombreHost ?: detalle.usuario ?: "Anfitrión"
                    binding.tvPropietario.text = getString(R.string.fmt_host_name, nombreHost)

                    binding.tvComodidadesPropiedad.text = detalle.amenidades?.joinToString(", ") { it.nombre } ?: "Ninguna"
                    binding.tvActividadesPropiedad.text = detalle.hobbies?.joinToString(", ") { it.nombre } ?: "Ninguna"

                    // Actualizamos las fotos si llegaron nuevas
                    if (!detalle.fotos.isNullOrEmpty()) {
                        actualizarSliderFotos(detalle.fotos)
                        currentProperty = currentProperty.copy(imagenes = detalle.fotos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    // ... (El resto de funciones configurarVista, determinarRol, etc. se mantienen igual)
    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    private fun configurarVista(prop: Property) {
        binding.tvTituloPropiedad.text = prop.titulo
        binding.tvCapacidadPropiedad.text = PropertyUtils.getFormattedCapacity(this, prop.capacidad)
        binding.tvDireccionPropiedad.text = prop.direccion

        // Placeholders mientras carga
        binding.tvPropietario.text = "Cargando..."
        binding.tvDescripcionPropiedad.text = "Cargando..."

        val precioDouble = prop.precio.replace(",", "").toDoubleOrNull() ?: 0.0
        val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        binding.tvPrecioPropiedad.text = formatoMoneda.format(precioDouble)

        if (prop.imagenes.isNotEmpty()) {
            actualizarSliderFotos(prop.imagenes)
        } else {
            binding.tvImageCounter.visibility = View.GONE
        }
    }

    private fun determinarRolYConfigurarBotones(prop: Property, isRentalActive: Boolean) {
        val currentUserId = SessionManager.obtenerUserId(this)
        val esDueno = (prop.userId == currentUserId)

        if (esDueno || isRentalActive) {
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
        startActivity(intent)
    }

    private fun irADomotica() {
        startActivity(Intent(this, LightControlActivity::class.java))
    }
}