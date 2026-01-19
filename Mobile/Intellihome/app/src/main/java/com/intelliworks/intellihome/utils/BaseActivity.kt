package com.intelliworks.intellihome.utils

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
import com.intelliworks.intellihome.R // Asegúrate de importar tu R
import com.intelliworks.intellihome.SettingsActivity // Importa tu SettingsActivity

abstract class BaseActivity : AppCompatActivity() {

    // Variable para rastrear el idioma actual de esta instancia
    protected var lastLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicamos el tema ANTES de super.onCreate y setContentView
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = prefs.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
    }
    // Crea esta función para que Login, Register y Settings la usen
    fun applyAppAppearance(rootView: View) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = prefs.getBoolean("dark_mode", false)
        val bgColor = prefs.getInt("bg_color", Color.WHITE)

        val adaptedColor = if (darkMode) {
            // Oscurecemos el color un 40% si es modo noche
            Color.argb(
                Color.alpha(bgColor),
                (Color.red(bgColor) * 0.6f).toInt(),
                (Color.green(bgColor) * 0.6f).toInt(),
                (Color.blue(bgColor) * 0.6f).toInt()
            )
        } else {
            bgColor
        }
        rootView.setBackgroundColor(adaptedColor)
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currentLanguage = prefs.getString("language", "es")

        // Si el idioma cambió mientras la actividad estaba en pausa, recreamos
        if (lastLanguage != null && lastLanguage != currentLanguage) {
            recreate()
        }

        // Actualizamos el registro del idioma actual
        lastLanguage = currentLanguage
    }
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE)
        val language = prefs.getString("language", "es") ?: "es"
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }

    // Usamos esta firma que es la que usa ViewBinding
    override fun setContentView(view: View?) {
        // 1. Inflamos el layout base (el que tiene el botón)
        val rootLayout = layoutInflater.inflate(R.layout.activity_base, null) as ViewGroup
        val container = rootLayout.findViewById<FrameLayout>(R.id.activity_content_container)

        // 2. Añadimos la vista de la Activity (Login, etc) al contenedor
        view?.let {
            container.addView(it)
        }

        // 3. LLAMADA CRUCIAL: Usamos super.setContentView para evitar bucles
        super.setContentView(rootLayout)

        // 4. Configuramos el botón una vez que la vista ya está en pantalla
        setupBaseMenu()
    }

    private fun setupBaseMenu() {
        val btnMenu = findViewById<ImageButton>(R.id.btnBaseMenu)
        btnMenu?.setOnClickListener {
            showMenu(it)
        }
    }
    fun showSettingsButton(show: Boolean) {
        val btnMenu = findViewById<ImageButton>(R.id.btnBaseMenu)
        btnMenu?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_settings, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}