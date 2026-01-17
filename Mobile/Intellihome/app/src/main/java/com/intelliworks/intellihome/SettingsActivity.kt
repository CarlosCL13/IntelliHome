package com.intelliworks.intellihome

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.intelliworks.intellihome.databinding.ActivitySettingsBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        prefs = getSharedPreferences("settings", MODE_PRIVATE)


        val darkMode = prefs.getBoolean("dark_mode", false)
        val savedColor = prefs.getInt("bg_color", Color.WHITE)

        applyTheme(darkMode)
        applyBackgroundColor(savedColor, darkMode)


        binding.switchTheme.isChecked = darkMode

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)

            val color = prefs.getInt("bg_color", Color.WHITE)
            applyBackgroundColor(color, isChecked)
        }

        binding.btnColorPicker.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Selecciona un color")
                .setPositiveButton("OK", object : ColorEnvelopeListener {
                    override fun onColorSelected(
                        envelope: ColorEnvelope,
                        fromUser: Boolean
                    ) {
                        val color = envelope.color
                        prefs.edit().putInt("bg_color", color).apply()

                        val dark = prefs.getBoolean("dark_mode", false)
                        applyBackgroundColor(color, dark)
                    }
                })
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        binding.btnSpanish.setOnClickListener {
            setLanguage("es")
        }

        binding.btnEnglish.setOnClickListener {
            setLanguage("en")
        }
    }

    // ===============================
    // FUNCIONES AUXILIARES
    // ===============================

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
                (Color.red(color) * 0.6).toInt(),
                (Color.green(color) * 0.6).toInt(),
                (Color.blue(color) * 0.6).toInt()
            )
        } else {
            color
        }
    }

    private fun setLanguage(langCode: String) {
        prefs.edit().putString("language", langCode).apply()
        recreate()
    }
}
