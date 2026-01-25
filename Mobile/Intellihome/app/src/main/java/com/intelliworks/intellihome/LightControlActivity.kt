package com.intelliworks.intellihome

import android.os.Bundle
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.widget.Button
import com.intelliworks.intellihome.databinding.ActivityLightControlBinding
import com.intelliworks.intellihome.utils.BaseActivity

class LightControlActivity : BaseActivity() {

    private lateinit var binding: ActivityLightControlBinding
    
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

        configurarBotones()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
        // Mantener el color del contenedor del plano independientemente de las preferencias
        binding.housePlanContainer.setBackgroundColor(
            ContextCompat.getColor(this, R.color.light_off_darker)
        )
    }

    // Método para enviar al backend si una luz está encendida o apagada
    private fun enviarEstadoLuz(luz: String, encendida: Boolean) {
        // Aquí iría la lógica para enviar el estado al backend
        // Por ejemplo, una llamada HTTP
    }

    // Método para enviar el estado de todas las luces
    private fun enviarEstadoTodasLasLuces(encendidas: Boolean) {
        configurarBotonLuces()
    }

    /**
     * Configura todos los botones de luces con su funcionalidad de toggle
     */
    private fun configurarBotones() {
        configurarBotonLuz(binding.btnBanio1) { luzBanio1 = !luzBanio1; luzBanio1 }
        configurarBotonLuz(binding.btnBanio2) { luzBanio2 = !luzBanio2; luzBanio2 }
        configurarBotonLuz(binding.btnCocina) { luzCocina = !luzCocina; luzCocina }
        configurarBotonLuz(binding.btnSala) { luzSala = !luzSala; luzSala }
        configurarBotonLuz(binding.btnGaraje) { luzGaraje = !luzGaraje; luzGaraje }
        configurarBotonLuz(binding.btnHabita1) { luzHabitacion1 = !luzHabitacion1; luzHabitacion1 }
        configurarBotonLuz(binding.btnHabita2) { luzHabitacion2 = !luzHabitacion2; luzHabitacion2 }
        configurarBotonLuz(binding.btnHabita3) { luzHabitacion3 = !luzHabitacion3; luzHabitacion3 }
        configurarBotonLuces()
    }

    /**
     * Configura un botón de luz para el cambio de la confirmación visual y el estado en el backend
     */
    private fun configurarBotonLuz(boton: Button, cambiarEstado: () -> Boolean) {
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
        }
    }

    /**
     * Configura el botón para encender/apagar todas las luces
     * Si todas están encendidas -> apaga todas
     * Si alguna está apagada -> enciende todas
     */
    private fun configurarBotonLuces() {
        binding.btnLuces.setOnClickListener {
            // Verificar si todas las luces están encendidas
            val todasEncendidas = luzBanio1 && luzBanio2 && luzCocina && luzSala && 
                                   luzGaraje && luzHabitacion1 && luzHabitacion2 && luzHabitacion3
            
            // Alternar: si todas están encendidas, apagar; si no, encender todas
            val nuevoEstado = !todasEncendidas
            
            // Actualizar estados de todas las luces
            luzBanio1 = nuevoEstado
            luzBanio2 = nuevoEstado
            luzCocina = nuevoEstado
            luzSala = nuevoEstado
            luzGaraje = nuevoEstado
            luzHabitacion1 = nuevoEstado
            luzHabitacion2 = nuevoEstado
            luzHabitacion3 = nuevoEstado
            
            // Determinar el color según el nuevo estado
            val nuevoColor = if (nuevoEstado) {
                ContextCompat.getColor(this, R.color.light_on)
            } else {
                ContextCompat.getColor(this, R.color.light_off_darker)
            }
            
            // Actualizar el texto del botón según el nuevo estado
            binding.btnLuces.text = if (nuevoEstado) {
                getString(R.string.btn_turn_off_all_lights)
            } else {
                getString(R.string.btn_turn_on_all_lights)
            }
            
            // Aplicar color a todos los botones
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
        }
    }
}
