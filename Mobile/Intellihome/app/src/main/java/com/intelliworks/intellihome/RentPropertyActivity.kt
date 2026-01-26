package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.intelliworks.intellihome.databinding.ActivityRentPropertyBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.data.model.PropiedadDetalleDto
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide

class RentPropertyActivity : BaseActivity() {

    private lateinit var binding: ActivityRentPropertyBinding
    private val propiedadRepository = PropiedadRepository(propiedadId = 1)
    private var propiedadActual: PropiedadDetalleDto? = null
    private var currentPhotoIndex = 0
    private var fotosPropiedad = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotonAlquilar()
        configurarNavegacionFotos()
        cargarDetallesPropiedad()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    // Carga los detalles de la propiedad desde el backend
    private fun cargarDetallesPropiedad() {
        lifecycleScope.launch {
            try {
                val response = propiedadRepository.obtenerDetallePropiedad()
                if (response.isSuccessful) {
                    response.body()?.let { propiedad ->
                        propiedadActual = propiedad
                        mostrarDetallesPropiedad(propiedad)
                    }
                } else {
                    Toast.makeText(
                        this@RentPropertyActivity,
                        "Error al cargar la propiedad",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RentPropertyActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Muestra los detalles de la propiedad en la UI
    private fun mostrarDetallesPropiedad(propiedad: PropiedadDetalleDto) {

        // Título
        binding.tvTituloPropiedad.text = propiedad.titulo_publicacion

        // Fotos de la propiedad
        fotosPropiedad = propiedad.fotos
        currentPhotoIndex = 0
        if (fotosPropiedad.isNotEmpty()) {
            mostrarFotoActual()
            actualizarControlesFotos()
        }

        // Característica
        val amenidadesTexto = buildString {
            append("${propiedad.huespedes} huéspedes • ")
            append("${propiedad.habitaciones} habitaciones • ")
            append("${propiedad.camas} camas • ")
            append("${propiedad.banos} baños")
        }
        binding.tvAmenidadesPropiedad.text = amenidadesTexto

        // Propietario
        binding.tvPropietario.text = buildString {
            append("Anfitrión: ${propiedad.usuario_nombre_completo ?: propiedad.usuario ?: "No disponible"}")
            propiedad.usuario_telefono?.let {
                append("\nTeléfono: $it")
            }
        }

        // Imagen del propietario
        propiedad.usuario_imagen_perfil?.let { imagenUrl ->
            Glide.with(this)
                .load(imagenUrl)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .circleCrop()
                .into(binding.imgPropietario)
        }

        // Descripción
        binding.tvDescripcionPropiedad.text = propiedad.descripcion_publicacion

        // Actividades (hobbies)
        val actividadesTexto = if (propiedad.hobbies.isNotEmpty()) {
            "Actividades disponibles:\n" + propiedad.hobbies.joinToString("\n") { "• ${it.nombre}" }
        } else {
            "No hay actividades especificadas"
        }
        binding.tvActividadesPropiedad.text = actividadesTexto

        // Comodidades
        val comodidadesTexto = if (propiedad.amenidades.isNotEmpty()) {
            "Comodidades:\n" + propiedad.amenidades.joinToString("\n") { "• ${it.nombre}" }
        } else {
            "No hay comodidades especificadas"
        }
        binding.tvComodidadesPropiedad.text = comodidadesTexto

        // Reglas de uso
        val reglasTexto = propiedad.reglas_uso ?: "No hay reglas especificadas"
        binding.tvReglasPropiedad.text = "Reglas de la casa:\n$reglasTexto"

        // Actualizar texto del botón con el precio
        binding.btnAlquilarPropiedad.text = "Alquilar por $${propiedad.precio_noche} / noche"
    }

    // Configura el botón de alquilar
    private fun configurarBotonAlquilar() {
        binding.btnAlquilarPropiedad.setOnClickListener {
            procesarAlquiler()
        }
    }

    // Configura los botones de navegación entre fotos
    private fun configurarNavegacionFotos() {
        binding.btnPrevFoto.setOnClickListener {
            if (currentPhotoIndex > 0) {
                currentPhotoIndex--
                mostrarFotoActual()
                actualizarControlesFotos()
            }
        }

        binding.btnNextFoto.setOnClickListener {
            if (currentPhotoIndex < fotosPropiedad.size - 1) {
                currentPhotoIndex++
                mostrarFotoActual()
                actualizarControlesFotos()
            }
        }
    }

    // Muestra la foto actual en el ImageView
    private fun mostrarFotoActual() {
        if (fotosPropiedad.isNotEmpty() && currentPhotoIndex < fotosPropiedad.size) {
            Glide.with(this)
                .load(fotosPropiedad[currentPhotoIndex])
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.imgPropiedad)
        }
    }

    // Actualiza el indicador y la visibilidad de los botones
    private fun actualizarControlesFotos() {
        // Actualizar indicador (ej: "2/5")
        binding.tvIndicadorFotos.text = "${currentPhotoIndex + 1}/${fotosPropiedad.size}"

        // Mostrar/ocultar botones según la posición
        binding.btnPrevFoto.alpha = if (currentPhotoIndex > 0) 0.8f else 0.3f
        binding.btnPrevFoto.isEnabled = currentPhotoIndex > 0

        binding.btnNextFoto.alpha = if (currentPhotoIndex < fotosPropiedad.size - 1) 0.8f else 0.3f
        binding.btnNextFoto.isEnabled = currentPhotoIndex < fotosPropiedad.size - 1
    }

    // Procesa la solicitud de alquiler de la propiedad
    private fun procesarAlquiler() {
        // Navegar a la pantalla de pago
        val intent = Intent(this, PaymentActivity::class.java)
        startActivity(intent)
    }
}
