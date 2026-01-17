package com.intelliworks.intellihome

import androidx.biometric.BiometricManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.intelliworks.intellihome.databinding.ActivityRegisterBinding

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        binding.signupButton.setOnClickListener {
            val username = binding.signupUsername.text.toString()
            val password = binding.signupPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.checkboxFingerprint.isChecked) {

                if (canUseBiometric()) {
                    showBiometricPrompt {
                        registerUser(username, password, true)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Fingerprint not available on this device",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Registramos SIN huella
                    registerUser(username, password, false)
                }

            } else {
                registerUser(username, password, false)
            }
        }
    }

    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun registerUser(
        username: String,
        password: String,
        fingerprintEnabled: Boolean
    ) {
        val result = databaseHelper.insertUser(username, password, fingerprintEnabled)

        if (result != -1L) {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Login::class.java))
            finish()
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
        }
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
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@Register, errString, Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm fingerprint")
            .setSubtitle("Enable fingerprint login")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
