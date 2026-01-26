package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.intelliworks.intellihome.databinding.ActivityRentPropertyBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.ImagePagerAdapter
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyUtils
import com.intelliworks.intellihome.utils.SessionManager
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
            determinarRolYConfigurarBotones(currentProperty, isRentalActive)
        } else {
            Toast.makeText(this, getString(R.string.error_loading_property_data), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    private fun configurarVista(prop: Property) {
        binding.tvTituloPropiedad.text = prop.titulo

        // Formateo dinámico de capacidad usando utilidad centralizada
        binding.tvCapacidadPropiedad.text = PropertyUtils.getFormattedCapacity(this, prop.capacidad)

        binding.tvDireccionPropiedad.text = prop.direccion
        binding.tvPropietario.text = getString(R.string.fmt_host_name, prop.nombreUsuario)
        binding.tvDescripcionPropiedad.text = prop.descripcion
        binding.tvActividadesPropiedad.text = prop.actividades
        binding.tvComodidadesPropiedad.text = prop.comodidades
        binding.tvReglasPropiedad.text = prop.reglas

        val precioDouble = prop.precio.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
        val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        binding.tvPrecioPropiedad.text = formatoMoneda.format(precioDouble)

        if (prop.imagenes.isNotEmpty()) {
            val adapter = ImagePagerAdapter(
                images = prop.imagenes,
                layoutId = R.layout.item_image_slider
            ) { position ->
                abrirVisorDeFotos(prop.imagenes, position)
            }

            binding.viewPagerImages.adapter = adapter
            binding.tvImageCounter.text = "1/${prop.imagenes.size}"

            binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.tvImageCounter.text = "${position + 1}/${prop.imagenes.size}"
                }
            })
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

        // Serialización de la propiedad para pasarla al flujo de pago
        val gson = Gson()
        val propertyJson = gson.toJson(currentProperty)
        intent.putExtra("property_data", propertyJson)

        startActivity(intent)
    }

    private fun irADomotica() {
        startActivity(Intent(this, LightControlActivity::class.java))
    }
}