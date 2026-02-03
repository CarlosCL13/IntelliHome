package com.intelliworks.intellihome

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.os.Bundle
import android.os.VibrationEffect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.intelliworks.intellihome.data.repository.CasaRepository
import com.intelliworks.intellihome.databinding.ActivityLightControlBinding
import com.intelliworks.intellihome.utils.BaseActivity
import kotlinx.coroutines.launch

/**
 * Actividad principal de control domótico y centro de gestión de emergencias.
 *
 * Características principales:
 * 1. UI Optimista para control de luces y garaje (feedback instantáneo).
 * 2. Sistema de renderizado de emergencias (Incendio/Sismo) basado en persistencia y broadcasts.
 * 3. Gestión del ciclo de vida para recuperación de estado ante reinicios.
 */
class LightControlActivity : BaseActivity() {

    private lateinit var binding: ActivityLightControlBinding
    private val casaRepository = CasaRepository()

    // Identificador de la propiedad actual
    private var currentPropertyId: Int = 0

    // Estado local del garaje
    private var garageAbierto = false

    // Cache local de estados de iluminación
    private var luzBanio1 = false; private var luzBanio2 = false; private var luzCocina = false
    private var luzSala = false; private var luzGaraje = false; private var luzHabitacion1 = false
    private var luzHabitacion2 = false; private var luzHabitacion3 = false

    // Controladores de animación
    private var animadorSismo: ObjectAnimator? = null
    private var animadorFuego: ObjectAnimator? = null

    // Mapa de traducción UI <-> Backend
    private val habitacionesMap = mapOf(
        "banio1" to "Bano 1", "banio2" to "Bano 2", "cocina" to "Cocina",
        "sala" to "Sala", "garaje" to "Garaje", "habitacion1" to "Habitacion 1",
        "habitacion2" to "Habitacion 2", "habitacion3" to "Habitacion 3"
    )

    // Receptor para eventos en tiempo real (cuando la app está abierta)
    private val alertaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val evento = intent?.getStringExtra("tipo_evento") ?: ""
            if (evento.isNotEmpty()) {
                manejarEmergencia(evento)
            }
        }
    }

    // ==========================================
    // CICLO DE VIDA
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showSettingsButton(false)
        currentPropertyId = intent.getIntExtra("PROPERTY_ID", 0)

        // Inicialización de componentes
        configurarOverlays()
        configurarBotonGaraje()

        // Configuración inicial del botón de alerta (inactivo por defecto)
        binding.imgFuegoAlerta.setOnClickListener {
            mostrarDialogoResolverEmergencia()
        }
        binding.imgFuegoAlerta.isClickable = false

        // Verificación de apertura via Notificación
        checkIntentForEmergency(intent)

        // Sincronización con backend
        cargarEstadoInicial()
    }

    /**
     * Maneja el intent cuando la actividad ya existe en segundo plano y es traída al frente.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForEmergency(intent)
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)

        // 1. Escuchar eventos en vivo
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(alertaReceiver, IntentFilter("EVENTO_EMERGENCIA_IOT"))

        // 2. Revisar persistencia (por si ocurrió un evento con la app cerrada)
        verificarEstadoPersistente()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alertaReceiver)
    }

    // ==========================================
    // LÓGICA DE EMERGENCIAS
    // ==========================================

    private fun checkIntentForEmergency(intent: Intent?) {
        if (intent == null || intent.extras == null) return

        val eventoAutomatico = intent.getStringExtra("auto_event")

        if (!eventoAutomatico.isNullOrEmpty()) {
            manejarEmergencia(eventoAutomatico)
            intent.removeExtra("auto_event")
        }
    }

    private fun verificarEstadoPersistente() {
        val prefs = getSharedPreferences("IntelliHome_Emergencia", Context.MODE_PRIVATE)
        val eventoPendiente = prefs.getString("evento_activo", null)

        if (!eventoPendiente.isNullOrEmpty()) {
            manejarEmergencia(eventoPendiente)
        }
    }

    private fun manejarEmergencia(evento: String) {
        when (evento.lowercase()) {
            "sismo" -> activarModoSismo()
            "incendio" -> activarModoIncendio()
        }
    }

    private fun activarModoSismo() {
        runOnUiThread {
            if (animadorSismo?.isRunning == true) return@runOnUiThread

            val pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
            val pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 10f, -10f, 10f, -10f, 5f, -5f, 0f)

            animadorSismo = ObjectAnimator.ofPropertyValuesHolder(binding.housePlanContainer, pvhX, pvhY).apply {
                duration = 1000
                repeatCount = 10
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            Toast.makeText(this, "⚠️ ALERTA DE SISMO DETECTADA", Toast.LENGTH_LONG).show()
        }
    }

    private fun activarModoIncendio() {
        runOnUiThread {
            // Verificación de estado para evitar reinicio de animaciones
            if (binding.imgFuegoAlerta.visibility == View.VISIBLE && binding.imgFuegoAlerta.isClickable) {
                return@runOnUiThread
            }

            binding.imgFuegoAlerta.setImageResource(R.drawable.ic_fire_on)
            binding.imgFuegoAlerta.isClickable = true
            binding.imgFuegoAlerta.visibility = View.VISIBLE

            animadorFuego = ObjectAnimator.ofFloat(binding.imgFuegoAlerta, "alpha", 1f, 0.2f, 1f).apply {
                duration = 500
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
            Toast.makeText(this, "🔥 INCENDIO DETECTADO - Toque para resolver", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoResolverEmergencia() {
        AlertDialog.Builder(this)
            .setTitle("Resolver Emergencia")
            .setMessage("¿La situación ha sido controlada? Esto detendrá las alertas visuales.")
            .setPositiveButton("Sí, resuelto") { _, _ -> resolverProblema() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Resuelve la emergencia: Limpia persistencia, detiene animaciones y resetea UI.
     */
    private fun resolverProblema() {
        // DETENER VIBRACIÓN (Técnica de sobrescritura)
        detenerVibracionForzada()

        // Cancelar la notificación de la barra de estado
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(999)

        // Limpiar persistencia
        val prefs = getSharedPreferences("IntelliHome_Emergencia", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Resetear UI
        animadorFuego?.cancel()
        binding.imgFuegoAlerta.alpha = 1f
        binding.imgFuegoAlerta.setImageResource(R.drawable.ic_fire_off)
        binding.imgFuegoAlerta.isClickable = false

        animadorSismo?.cancel()
        binding.housePlanContainer.translationX = 0f
        binding.housePlanContainer.translationY = 0f

        Toast.makeText(this, "Emergencia resuelta. Vibración detenida.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Detiene la vibración usando técnica de sobrescritura de ALARMA.
     * Esto es necesario porque el servicio inició la vibración con USAGE_ALARM,
     * y un cancel() simple no tiene autoridad para detenerla.
     */
    private fun detenerVibracionForzada() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            // 1. Intento estándar
            vibrator.cancel()

            // 2. Sobrescritura de Prioridad (Android 8+)
            // Enviamos una vibración "vacía" con prioridad ALARMA para matar la anterior
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                val stopAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM) // CLAVE: Misma prioridad que el servicio
                    .build()

                // Vibrar 1ms con amplitud mínima
                val silentEffect = VibrationEffect.createOneShot(1L, 1)

                vibrator.vibrate(silentEffect, stopAttributes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // CONTROL DOMÓTICO (Luces y Garaje)
    // ==========================================

    private fun configurarBotonGaraje() {
        binding.btnGarageDoor.setOnClickListener {
            garageAbierto = !garageAbierto
            actualizarVisualGaraje(garageAbierto)
            enviarComandoGaraje(garageAbierto)
        }
    }

    private fun enviarComandoGaraje(abrir: Boolean) {
        lifecycleScope.launch {
            try {
                val response = casaRepository.cambiarEstadoGaraje(currentPropertyId, abrir)
                if (!response.isSuccessful) revertirEstadoGaraje()
            } catch (e: Exception) {
                revertirEstadoGaraje()
            }
        }
    }

    private fun revertirEstadoGaraje() {
        garageAbierto = !garageAbierto
        actualizarVisualGaraje(garageAbierto)
    }

    private fun actualizarVisualGaraje(abierto: Boolean) {
        if (abierto) {
            binding.housePlanContainer.setBackgroundResource(R.drawable.casa_open)
            binding.btnGarageDoor.setIconResource(R.drawable.ic_garage_open)
            binding.btnGarageDoor.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_on)

        } else {
            binding.housePlanContainer.setBackgroundResource(R.drawable.casa_closed)
            binding.btnGarageDoor.setIconResource(R.drawable.ic_garage_closed)
            binding.btnGarageDoor.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_off_darker)
        }

        // Quitamos el texto para que sea solo icono
        binding.btnGarageDoor.text = ""
    }

    private fun cargarEstadoInicial() {
        lifecycleScope.launch {
            try {
                val response = casaRepository.obtenerEstadoLeds(currentPropertyId)
                response.body()?.let { estadoDto -> actualizarEstadoLucesUI(estadoDto.toBoolean()) }
            } catch (e: Exception) {
            }
        }
    }

    private fun actualizarEstadoLucesUI(estados: Map<String, Boolean>) {
        val mapaInverso = habitacionesMap.entries.associate { (key, value) -> value to key }

        estados.forEach { (habitacionBackend, encendida) ->
            when (mapaInverso[habitacionBackend]) {
                "banio1" -> { luzBanio1 = encendida; actualizarOverlayHabitacion(binding.overlayBanio1, encendida) }
                "banio2" -> { luzBanio2 = encendida; actualizarOverlayHabitacion(binding.overlayBanio2, encendida) }
                "cocina" -> { luzCocina = encendida; actualizarOverlayHabitacion(binding.overlayCocina, encendida) }
                "sala" -> { luzSala = encendida; actualizarOverlayHabitacion(binding.overlaySala, encendida) }
                "garaje" -> { luzGaraje = encendida; actualizarOverlayHabitacion(binding.overlayGaraje, encendida) }
                "habitacion1" -> { luzHabitacion1 = encendida; actualizarOverlayHabitacion(binding.overlayHabita1, encendida) }
                "habitacion2" -> { luzHabitacion2 = encendida; actualizarOverlayHabitacion(binding.overlayHabita2, encendida) }
                "habitacion3" -> { luzHabitacion3 = encendida; actualizarOverlayHabitacion(binding.overlayHabita3, encendida) }
            }
        }
        actualizarBotonTodasLasLuces()
    }

    private fun actualizarOverlayHabitacion(overlay: View, encendida: Boolean) {
        overlay.setBackgroundResource(
            if (encendida) R.drawable.luz_radial
            else R.drawable.luz_apagada
        )    }

    private fun actualizarBotonTodasLasLuces() {
        val todasEncendidas = luzBanio1 && luzBanio2 && luzCocina && luzSala && luzGaraje && luzHabitacion1 && luzHabitacion2 && luzHabitacion3

        // Siempre dice "Todas"
        binding.btnLuces.text = "Todas"

        if (todasEncendidas) {
            binding.btnLuces.setIconResource(R.drawable.ic_bombilla_on)
            binding.btnLuces.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_on)
        } else {
            binding.btnLuces.setIconResource(R.drawable.ic_bombilla_off)
            binding.btnLuces.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_off_darker)
        }
    }

    private fun enviarEstadoLuz(key: String, on: Boolean) {
        val name = habitacionesMap[key] ?: return
        lifecycleScope.launch {
            try { casaRepository.cambiarLedHabitacion(currentPropertyId, name, on) } catch (_: Exception) {}
        }
    }

    private fun enviarEstadoTodasLasLuces(on: Boolean) {
        lifecycleScope.launch {
            try { casaRepository.cambiarTodosLosLeds(currentPropertyId, on) } catch (_: Exception) {}
        }
    }

    private fun configurarOverlays() {
        configurarClickOverlay(binding.overlayBanio1, "banio1") { luzBanio1 = !luzBanio1; luzBanio1 }
        configurarClickOverlay(binding.overlayBanio2, "banio2") { luzBanio2 = !luzBanio2; luzBanio2 }
        configurarClickOverlay(binding.overlayCocina, "cocina") { luzCocina = !luzCocina; luzCocina }
        configurarClickOverlay(binding.overlaySala, "sala") { luzSala = !luzSala; luzSala }
        configurarClickOverlay(binding.overlayGaraje, "garaje") { luzGaraje = !luzGaraje; luzGaraje }
        configurarClickOverlay(binding.overlayHabita1, "habitacion1") { luzHabitacion1 = !luzHabitacion1; luzHabitacion1 }
        configurarClickOverlay(binding.overlayHabita2, "habitacion2") { luzHabitacion2 = !luzHabitacion2; luzHabitacion2 }
        configurarClickOverlay(binding.overlayHabita3, "habitacion3") { luzHabitacion3 = !luzHabitacion3; luzHabitacion3 }

        binding.btnLuces.setOnClickListener {
            val todas = luzBanio1 && luzBanio2 && luzCocina && luzSala && luzGaraje && luzHabitacion1 && luzHabitacion2 && luzHabitacion3
            val nuevo = !todas
            luzBanio1=nuevo; luzBanio2=nuevo; luzCocina=nuevo; luzSala=nuevo; luzGaraje=nuevo; luzHabitacion1=nuevo; luzHabitacion2=nuevo; luzHabitacion3=nuevo
            val overlays = listOf(binding.overlayBanio1, binding.overlayBanio2, binding.overlayCocina, binding.overlaySala, binding.overlayGaraje, binding.overlayHabita1, binding.overlayHabita2, binding.overlayHabita3)
            overlays.forEach { actualizarOverlayHabitacion(it, nuevo) }
            actualizarBotonTodasLasLuces()
            enviarEstadoTodasLasLuces(nuevo)
        }
    }

    private fun configurarClickOverlay(overlay: View, key: String, change: () -> Boolean) {
        overlay.setOnClickListener {
            val new = change()
            actualizarOverlayHabitacion(overlay, new)
            actualizarBotonTodasLasLuces()
            enviarEstadoLuz(key, new)
        }
    }
}