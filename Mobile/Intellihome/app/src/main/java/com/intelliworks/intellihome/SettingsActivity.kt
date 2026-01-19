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

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // ===============================
        // CARGAR PREFERENCIAS
        // ===============================
        val darkMode = prefs.getBoolean("dark_mode", false)
        val bgColor = prefs.getInt("bg_color", Color.WHITE)
        val language = prefs.getString("language", "es") ?: "es"

        applyTheme(darkMode)
        applyBackgroundColor(bgColor, darkMode)
        updateLanguageBorder(language)

        // ===============================
        // TEMA OSCURO
        // ===============================
        binding.switchTheme.isChecked = darkMode
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)

            val color = prefs.getInt("bg_color", Color.WHITE)
            applyBackgroundColor(color, isChecked)
        }

        // ===============================
        // COLOR PICKER
        // ===============================
        binding.btnColorPicker.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle(getString(R.string.select_color))
                .setPositiveButton(
                    getString(android.R.string.ok),
                    object : ColorEnvelopeListener {
                        override fun onColorSelected(
                            envelope: ColorEnvelope,
                            fromUser: Boolean
                        ) {
                            val color = envelope.color
                            prefs.edit().putInt("bg_color", color).apply()

                            val dark = prefs.getBoolean("dark_mode", false)
                            applyBackgroundColor(color, dark)
                        }
                    }
                )
                .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // ===============================
        // IDIOMA
        // ===============================
        binding.cardSpanish.setOnClickListener {
            changeLanguage("es")
        }

        binding.cardEnglish.setOnClickListener {
            changeLanguage("en")
        }
    }

    // ===============================
    // FUNCIONES AUXILIARES
    // ===============================

    private fun changeLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        restartActivity()
    }

    private fun restartActivity() {
        val intent = intent
        finish()
        startActivity(intent)
    }

    private fun applyTheme(darkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun applyBackgroundColor(color: Int, darkMode: Boolean) {
        val adaptedColor = adaptColorForTheme(color, darkMode)
        binding.root.setBackgroundColor(adaptedColor)
    }

    private fun adaptColorForTheme(color: Int, darkMode: Boolean): Int {
        return if (darkMode) {
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

    private fun updateLanguageBorder(lang: String) {
        val borderColor = getColor(R.color.green)
        val strokePx = resources.getDimensionPixelSize(R.dimen.language_border)

        if (lang == "es") {
            binding.cardSpanish.strokeColor = borderColor
            binding.cardSpanish.strokeWidth = strokePx
            binding.cardEnglish.strokeWidth = 0
        } else {
            binding.cardEnglish.strokeColor = borderColor
            binding.cardEnglish.strokeWidth = strokePx
            binding.cardSpanish.strokeWidth = 0
        }
    }
}
