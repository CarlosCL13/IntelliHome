package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.intelliworks.intellihome.databinding.ActivitySettingsBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

/**
 * Actividad encargada de la gestión de preferencias del sistema.
 * Permite al usuario personalizar el tema visual, el color de fondo de la interfaz
 * y la localización (idioma) de la aplicación.
 */
class SettingsActivity : BaseActivity() {

    private lateinit var enlace: ActivitySettingsBinding
    private lateinit var preferencias: SharedPreferences

    override fun onResume() {
        super.onResume()
        // Sincroniza la apariencia visual con el estado actual de las preferencias
        applyAppAppearance(enlace.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enlace = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        // Oculta el botón de ajustes dentro de su propia pantalla para evitar redundancia
        showSettingsButton(false)

        preferencias = getSharedPreferences("settings", MODE_PRIVATE)

        // ==========================================
        // CARGA INICIAL DE PREFERENCIAS
        // ==========================================
        val modoOscuroActivo = preferencias.getBoolean("dark_mode", false)
        val colorFondoBase = preferencias.getInt("bg_color", Color.WHITE)
        val codigoIdioma = preferencias.getString("language", "es") ?: "es"

        aplicarModoVisual(modoOscuroActivo)
        aplicarColorDeFondo(colorFondoBase, modoOscuroActivo)
        actualizarIndicadorIdioma(codigoIdioma)

        // ==========================================
        // GESTIÓN DE TEMA OSCURO
        // ==========================================
        enlace.switchTheme.isChecked = modoOscuroActivo
        enlace.switchTheme.setOnCheckedChangeListener { _, estaChequeado ->
            preferencias.edit().putBoolean("dark_mode", estaChequeado).apply()
            aplicarModoVisual(estaChequeado)

            // Re-calcula la adaptación del color de fondo ante el cambio de tema
            val colorActual = preferencias.getInt("bg_color", Color.WHITE)
            aplicarColorDeFondo(colorActual, estaChequeado)
        }

        // ==========================================
        // SELECTOR DE COLOR PERSONALIZADO
        // ==========================================
        enlace.btnColorPicker.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle(getString(R.string.select_color))
                .setPositiveButton(
                    getString(android.R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(
                            sobre: ColorEnvelope,
                            desdeUsuario: Boolean
                        ) {
                            val colorSeleccionado = sobre.color
                            preferencias.edit().putInt("bg_color", colorSeleccionado).apply()

                            val esOscuro = preferencias.getBoolean("dark_mode", false)
                            aplicarColorDeFondo(colorSeleccionado, esOscuro)
                        }
                    }
                )
                .setNegativeButton(getString(android.R.string.cancel)) { dialogo, _ ->
                    dialogo.dismiss()
                }
                .show()
        }

        // ==========================================
        // CAMBIO DE LOCALIZACIÓN
        // ==========================================
        enlace.cardSpanish.setOnClickListener {
            cambiarIdiomaSistema("es")
        }

        enlace.cardEnglish.setOnClickListener {
            cambiarIdiomaSistema("en")
        }
    }

    // ==========================================
    // MÉTODOS DE SOPORTE Y LÓGICA DE NEGOCIO
    // ==========================================

    /**
     * Actualiza la preferencia de idioma y reinicia la actividad para aplicar cambios.
     */
    private fun cambiarIdiomaSistema(codigoIso: String) {
        preferencias.edit().putString("language", codigoIso).apply()
        reiniciarActividadActual()
    }

    /**
     * Realiza un refresco de la instancia actual para aplicar configuraciones de recursos.
     */
    private fun reiniciarActividadActual() {
        val intentoReinicio = intent
        finish()
        startActivity(intentoReinicio)
    }

    /**
     * Delega al delegado de compatibilidad el cambio de modo noche a nivel sistema.
     */
    private fun aplicarModoVisual(modoOscuro: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Aplica el color procesado al fondo de la vista raíz.
     */
    private fun aplicarColorDeFondo(color: Int, modoOscuro: Boolean) {
        val colorAdaptado = adaptarColorSegunTema(color, modoOscuro)
        enlace.root.setBackgroundColor(colorAdaptado)
    }

    /**
     * Algoritmo de ajuste de luminancia: reduce el brillo del color un 40% si
     * el modo oscuro está activo para evitar fatiga visual y mantener contraste.
     */
    private fun adaptarColorSegunTema(color: Int, modoOscuro: Boolean): Int {
        return if (modoOscuro) {
            Color.argb(
                Color.alpha(color),
                (Color.red(color) * 0.6f).toInt(),
                (Color.green(color) * 0.6f).toInt(),
                (Color.blue(color) * 0.6f).toInt()
            )
        } else {
            color
        }
    }

    /**
     * Actualiza la retroalimentación visual (borde) de las tarjetas de selección de idioma.
     */
    private fun actualizarIndicadorIdioma(codigoIso: String) {
        val colorBorde = getColor(R.color.green)
        val grosorBorde = resources.getDimensionPixelSize(R.dimen.language_border)

        if (codigoIso == "es") {
            enlace.cardSpanish.strokeColor = colorBorde
            enlace.cardSpanish.strokeWidth = grosorBorde
            enlace.cardEnglish.strokeWidth = 0
        } else {
            enlace.cardEnglish.strokeColor = colorBorde
            enlace.cardEnglish.strokeWidth = grosorBorde
            enlace.cardSpanish.strokeWidth = 0
        }
    }
}