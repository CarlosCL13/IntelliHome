package com.intelliworks.intellihome

import android.os.Bundle
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.intelliworks.intellihome.databinding.ActivityLightControlBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.data.repository.CasaRepository
import kotlinx.coroutines.launch

class LightControlActivity : BaseActivity() {

    private lateinit var binding: ActivityLightControlBinding
    private val casaRepository = CasaRepository()

    // Mapeo de habitaciones al formato del backend
    private val habitacionesMap = mapOf(
        "banio1" to "Bano 1",
        "banio2" to "Bano 2",
        "cocina" to "Cocina",
        "sala" to "Sala",
        "garaje" to "Garaje",
        "habitacion1" to "Habitacion 1",
        "habitacion2" to "Habitacion 2",
        "habitacion3" to "Habitacion 3"
    )

    // Estado de cada luz (false = apagada, true = encendida)
    private var luzBanio1 = false
    private var luzBanio2 = false
    private var luzCocina = false
    private var luzSala = false
    private var luzGaraje = false
    private var luzHabitacion1 = false
    private var luzHabitacion2 = false
    private var luzHabitacion3 = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showSettingsButton(false)

        configurarBotones()
        cargarEstadoInicial()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)

        // Mantener el color del contenedor del plano independientemente de las preferencias
        binding.housePlanContainer.setBackgroundColor(
            ContextCompat.getColor(this, R.color.light_off_darker)
        )
    }

    // Carga el estado inicial de todas las luces desde el backend
    private fun cargarEstadoInicial() {
        lifecycleScope.launch {
            try {
                val response = casaRepository.obtenerEstadoLeds()
                if (response.isSuccessful) {
                    response.body()?.let { estadoDto ->
                        actualizarEstadoLuces(estadoDto.toBoolean())
                    }
                } else {
                    Toast.makeText(
                        this@LightControlActivity,
                        "Error al cargar estado de las luces",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LightControlActivity,
                    "Error de conexión al cargar estado: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Actualiza el estado de las luces en la UI
    private fun actualizarEstadoLuces(estados: Map<String, Boolean>) {
        // Mapea del nombre del backend a la key local
        val mapaInverso = habitacionesMap.entries.associate { (key, value) -> value to key }

        estados.forEach { (habitacion, encendida) ->
            when (mapaInverso[habitacion]) {
                "banio1" -> actualizarBotonLuz(binding.btnBanio1, encendida).also { luzBanio1 = encendida }
                "banio2" -> actualizarBotonLuz(binding.btnBanio2, encendida).also { luzBanio2 = encendida }
                "cocina" -> actualizarBotonLuz(binding.btnCocina, encendida).also { luzCocina = encendida }
                "sala" -> actualizarBotonLuz(binding.btnSala, encendida).also { luzSala = encendida }
                "garaje" -> actualizarBotonLuz(binding.btnGaraje, encendida).also { luzGaraje = encendida }
                "habitacion1" -> actualizarBotonLuz(binding.btnHabita1, encendida).also { luzHabitacion1 = encendida }
                "habitacion2" -> actualizarBotonLuz(binding.btnHabita2, encendida).also { luzHabitacion2 = encendida }
                "habitacion3" -> actualizarBotonLuz(binding.btnHabita3, encendida).also { luzHabitacion3 = encendida }
            }
        }
        actualizarBotonTodasLasLuces()
    }

    // Actualiza el color de un botón según su estado
    private fun actualizarBotonLuz(boton: Button, encendida: Boolean) {
        val color = if (encendida) {
            ContextCompat.getColor(this, R.color.light_on)
        } else {
            ContextCompat.getColor(this, R.color.light_off_darker)
        }
        boton.backgroundTintList = ColorStateList.valueOf(color)
    }

    // Actualiza el botón de todas las luces según el estado actual
    private fun actualizarBotonTodasLasLuces() {
        val todasEncendidas = luzBanio1 && luzBanio2 && luzCocina && luzSala &&
                luzGaraje && luzHabitacion1 && luzHabitacion2 && luzHabitacion3

        binding.btnLuces.text = if (todasEncendidas) {
            getString(R.string.btn_turn_off_all_lights)
        } else {
            getString(R.string.btn_turn_on_all_lights)
        }
    }

    // Envia al backend si una luz está encendida o apagada
    private fun enviarEstadoLuz(habitacionKey: String, encendida: Boolean) {
        val habitacion = habitacionesMap[habitacionKey] ?: return

        lifecycleScope.launch {
            try {
                val response = casaRepository.cambiarLedHabitacion(habitacion, encendida)
                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@LightControlActivity,
                        "Error al cambiar estado de $habitacion",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LightControlActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Envia el estado de todas las luces
    private fun enviarEstadoTodasLasLuces(encendidas: Boolean) {
        lifecycleScope.launch {
            try {
                val response = casaRepository.cambiarTodosLosLeds(encendidas)
                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@LightControlActivity,
                        "Error al cambiar estado de todas las luces",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LightControlActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //Configura todos los botones de luces con su funcionalidad de toggle
    private fun configurarBotones() {
        configurarBotonLuz(binding.btnBanio1, "banio1") { luzBanio1 = !luzBanio1; luzBanio1 }
        configurarBotonLuz(binding.btnBanio2, "banio2") { luzBanio2 = !luzBanio2; luzBanio2 }
        configurarBotonLuz(binding.btnCocina, "cocina") { luzCocina = !luzCocina; luzCocina }
        configurarBotonLuz(binding.btnSala, "sala") { luzSala = !luzSala; luzSala }
        configurarBotonLuz(binding.btnGaraje, "garaje") { luzGaraje = !luzGaraje; luzGaraje }
        configurarBotonLuz(binding.btnHabita1, "habitacion1") { luzHabitacion1 = !luzHabitacion1; luzHabitacion1 }
        configurarBotonLuz(binding.btnHabita2, "habitacion2") { luzHabitacion2 = !luzHabitacion2; luzHabitacion2 }
        configurarBotonLuz(binding.btnHabita3, "habitacion3") { luzHabitacion3 = !luzHabitacion3; luzHabitacion3 }
        configurarBotonLuces()
    }

    // Configura un botón de luz para el cambio de la confirmación visual y el estado en el backend
    private fun configurarBotonLuz(boton: Button, habitacionKey: String, cambiarEstado: () -> Boolean) {
        boton.setOnClickListener {
            // Alterna el estado de la luz, hace uso de la lambda
            val nuevoEstado = cambiarEstado()

            // Cambia el color del botón según el nuevo estado
            val nuevoColor = if (nuevoEstado) {
                ContextCompat.getColor(this, R.color.light_on)
            } else {
                ContextCompat.getColor(this, R.color.light_off_darker)
            }

            // Cambia el color del botón
            boton.backgroundTintList = ColorStateList.valueOf(nuevoColor)

            // Enviar el estado al backend
            enviarEstadoLuz(habitacionKey, nuevoEstado)
        }
    }

    //Configura el botón para encender/apagar todas las luces
    private fun configurarBotonLuces() {
        binding.btnLuces.setOnClickListener {
            // Verifica si todas las luces están encendidas
            val todasEncendidas = luzBanio1 && luzBanio2 && luzCocina && luzSala &&
                    luzGaraje && luzHabitacion1 && luzHabitacion2 && luzHabitacion3

            // Alterna: si todas están encendidas, apagar; si no, encender todas
            val nuevoEstado = !todasEncendidas

            // Actualiza estados de todas las luces
            luzBanio1 = nuevoEstado
            luzBanio2 = nuevoEstado
            luzCocina = nuevoEstado
            luzSala = nuevoEstado
            luzGaraje = nuevoEstado
            luzHabitacion1 = nuevoEstado
            luzHabitacion2 = nuevoEstado
            luzHabitacion3 = nuevoEstado

            // Determina el color según el nuevo estado
            val nuevoColor = if (nuevoEstado) {
                ContextCompat.getColor(this, R.color.light_on)
            } else {
                ContextCompat.getColor(this, R.color.light_off_darker)
            }

            // Actualiza el texto del botón según el nuevo estado
            binding.btnLuces.text = if (nuevoEstado) {
                getString(R.string.btn_turn_off_all_lights)
            } else {
                getString(R.string.btn_turn_on_all_lights)
            }

            // Aplica color a todos los botones
            val botones = listOf(
                binding.btnBanio1,
                binding.btnBanio2,
                binding.btnCocina,
                binding.btnSala,
                binding.btnGaraje,
                binding.btnHabita1,
                binding.btnHabita2,
                binding.btnHabita3
            )

            botones.forEach { boton ->
                boton.backgroundTintList = ColorStateList.valueOf(nuevoColor)
            }

            // Envia el estado al backend
            enviarEstadoTodasLasLuces(nuevoEstado)
        }
    }
}
