package com.intelliworks.intellihome.utils

import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.intelliworks.intellihome.R
import com.intelliworks.intellihome.SettingsActivity

/**
 * Clase base abstracta para centralizar la gestión de configuración global,
 * persistencia de temas, localización y lógica de navegación compartida.
 */
abstract class BaseActivity : AppCompatActivity() {

    // Seguimiento del estado del lenguaje para validación en el ciclo de vida
    protected var ultimoIdioma: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicialización del modo nocturno previa a la creación de la instancia
        val preferencias = getSharedPreferences("settings", MODE_PRIVATE)
        val modoOscuroActivo = preferencias.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuroActivo) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
    }

    /**
     * Aplica los parámetros estéticos definidos en las preferencias al fondo de la vista.
     * Calcula la reducción de luminosidad necesaria si el modo nocturno está habilitado.
     */
    fun applyAppAppearance(vistaRaiz: View) {
        val preferencias = getSharedPreferences("settings", MODE_PRIVATE)
        val modoOscuroActivo = preferencias.getBoolean("dark_mode", false)
        val colorFondoBase = preferencias.getInt("bg_color", Color.WHITE)

        val colorAdaptado = if (modoOscuroActivo) {
            // Ajuste de luminancia al 60% para cumplimiento de accesibilidad en modo oscuro
            Color.argb(
                Color.alpha(colorFondoBase),
                (Color.red(colorFondoBase) * 0.6f).toInt(),
                (Color.green(colorFondoBase) * 0.6f).toInt(),
                (Color.blue(colorFondoBase) * 0.6f).toInt()
            )
        } else {
            colorFondoBase
        }
        vistaRaiz.setBackgroundColor(colorAdaptado)
    }

    override fun onResume() {
        super.onResume()

        val preferencias = getSharedPreferences("settings", MODE_PRIVATE)
        val idiomaActual = preferencias.getString("language", "es")

        // Verificación de integridad de localización: recrea la actividad si hubo cambios externos
        if (ultimoIdioma != null && ultimoIdioma != idiomaActual) {
            recreate()
        }

        ultimoIdioma = idiomaActual
    }

    override fun attachBaseContext(nuevoContextoBase: Context) {
        // Inyección de configuración de idioma en el contexto base antes de la inicialización
        val preferencias = nuevoContextoBase.getSharedPreferences("settings", MODE_PRIVATE)
        val idioma = preferencias.getString("language", "es") ?: "es"
        val contexto = LocaleHelper.setLocale(nuevoContextoBase, idioma)
        super.attachBaseContext(contexto)
    }

    /**
     * Implementación de patrón decorador para inyectar layouts secundarios
     * dentro de un contenedor persistente definido en la actividad base.
     */
    override fun setContentView(vista: View?) {
        val layoutRaiz = layoutInflater.inflate(R.layout.activity_base, null) as ViewGroup
        val contenedorContenido = layoutRaiz.findViewById<FrameLayout>(R.id.activity_content_container)

        vista?.let {
            contenedorContenido.addView(it)
        }

        // Delegación del layout final al sistema operativo evitando recursividad
        super.setContentView(layoutRaiz)
        configurarMenuBase()
    }

    /**
     * Inicializa los componentes de navegación global de la interfaz.
     */
    private fun configurarMenuBase() {
        val botonMenu = findViewById<ImageButton>(R.id.btnBaseMenu)
        botonMenu?.setOnClickListener {
            mostrarMenuOpciones(it)
        }
    }

    /**
     * Controla la visibilidad del acceso a configuraciones generales.
     */
    fun showSettingsButton(mostrar: Boolean) {
        val botonMenu = findViewById<ImageButton>(R.id.btnBaseMenu)
        botonMenu?.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    /**
     * Despliega el menú contextual de acciones rápidas del sistema.
     */
    private fun mostrarMenuOpciones(ancla: View) {
        val popupMenu = PopupMenu(this, ancla)
        popupMenu.menuInflater.inflate(R.menu.menu_settings, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.menu_help -> {
                    mostrarVentanaAyuda() // Llamada al nuevo método
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    /**
     * Crea y muestra un diálogo de alerta con los pasos para el registro.
     */
    private fun mostrarVentanaAyuda() {
        val builder = AlertDialog.Builder(this)
        val vistaAyuda = layoutInflater.inflate(R.layout.activity_help, null)

        builder.setView(vistaAyuda)
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}