package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.intelliworks.intellihome.databinding.ActivityLoginBinding

class Login : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var databaseHelper: DatabaseHelper
    private var lastLanguage: String? = null

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currentLanguage = prefs.getString("language", "es")

        if (lastLanguage != null && lastLanguage != currentLanguage) {
            recreate()
        }

        lastLanguage = currentLanguage
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val darkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bgColor = prefs.getInt("bg_color", Color.TRANSPARENT)
        if (bgColor != Color.TRANSPARENT) {
            binding.root.setBackgroundColor(bgColor)
        }

        databaseHelper = DatabaseHelper(this)

        binding.loginButton.setOnClickListener {
            val username = binding.loginUsername.text.toString()
            val password = binding.loginPassword.text.toString()

            if (databaseHelper.readUser(username, password)) {
                loginSuccess()
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMenu.setOnClickListener {
            showMenu(it)
        }

        binding.fingerprintLogin.setOnClickListener {
            val username = binding.loginUsername.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Enter username first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (databaseHelper.isFingerprintEnabled(username)) {
                showBiometricPrompt { loginSuccess() }
            } else {
                Toast.makeText(
                    this,
                    "Fingerprint not enabled for this user",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.signupRedirect.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
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
                R.id.menu_help -> true
                else -> false
            }
        }
        popup.show()
    }

    private fun loginSuccess() {
        val username = binding.loginUsername.text.toString()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish()
    }


    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Login")
            .setSubtitle("Authenticate to continue")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
