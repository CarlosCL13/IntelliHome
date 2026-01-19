package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import androidx.biometric.BiometricManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.intelliworks.intellihome.databinding.ActivityRegisterBinding

/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar/ocultar contraseña
        var passwordVisible = false
        binding.btnMostrarPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                binding.etContrasena.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                binding.etContrasena.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword.setImageResource(android.R.drawable.ic_menu_view)
            }
            // Mantener el cursor al final
            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }

        // Mostrar/ocultar confirmar contraseña
        var confirmPasswordVisible = false
        binding.btnMostrarPassword2.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            if (confirmPasswordVisible) {
                binding.etConfirmarContrasena.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword2.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                binding.etConfirmarContrasena.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword2.setImageResource(android.R.drawable.ic_menu_view)
            }
            binding.etConfirmarContrasena.setSelection(binding.etConfirmarContrasena.text?.length ?: 0)
        }

        // Base de datos de prueba
        databaseHelper = DatabaseHelper(this)

        // Registrar usuario se guarda en la base de datos y se redirige al inicio de sesión
        binding.btnRegistrar.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etContrasena.text.toString()

            // Se valida que los campos no esten vacios
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Favor llenar todos los campos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Se valida si se marcó la casilla de huella digital, sino se registra el usuario sin huella
            if (binding.cbHuellaDigital.isChecked) {
                // Si el dispositivo puede usar huella digital, se muestra el prompt de huella digital, sino se muestra un mensaje de aviso
                if (canUseBiometric()) {
                    showBiometricPrompt {
                        registerUser(username, password, true) // Se registra el usuario CON huella
                    }
                } else {
                    Toast.makeText(this, "Huella digital no disponible en este dispositivo", Toast.LENGTH_LONG).show()
                    registerUser(username, password, false) // Se registra el usuario SIN huella
                }
            } else {
                registerUser(username, password, false)
            }

        }

        // Redirigir al login al tocar el texto correspondiente
        binding.btnLoginRedirigir.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Método para validar si el dispositivo puede usar huella digital
    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Método para registrar el usuario en la base de datos
    private fun registerUser(username: String, password: String, fingerprintEnabled: Boolean) {
        val result = databaseHelper.insertUser(username, password, fingerprintEnabled)

        // Si el registro fue exitoso, se muestra un mensaje y se redirige al inicio de sesión, sino se muestra un mensaje de error
        if (result != -1L) {
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, Login::class.java))
            finish()
        } else {
            Toast.makeText(this, "El registro falló", Toast.LENGTH_LONG).show()
        }
    }

    // Metodo para mostrar el prompt de huella digital
    private fun showBiometricPrompt(onSuccess: () -> Unit) {

        // Ejecutor, esta variable nos permite ejecutar el prompt en el hilo principal de la aplicación
        val executor = ContextCompat.getMainExecutor(this)

        // Configuración del prompt
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@Register, errString, Toast.LENGTH_LONG).show()
                }
            })

        // Texto a mostrar en el prompt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar huella digital")
            .setSubtitle("Habilitar el inicio de sesión con huella digital")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
