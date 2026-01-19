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

/**
 * Controlador para la interfaz de acceso de usuarios.
 * Gestiona autenticación estándar, biométrica y persistencia de sesión local.
 */
class Login : BaseActivity() {

    private lateinit var enlace: ActivityLoginBinding
    private lateinit var baseDatos: DatabaseHelper

    // Estado para controlar la integridad de la contraseña recuperada de preferencias
    private var contrasenaCargadaDesdePreferencias = false

    override fun onResume() {
        super.onResume()
        applyAppAppearance(enlace.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enlace = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        applyAppAppearance(enlace.root)

        baseDatos = DatabaseHelper(this)

        // Inicialización de la funcionalidad "Recordarme"
        val preferenciasLogin = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val sesionRecordada = preferenciasLogin.getBoolean("is_remembered", false)

        if (sesionRecordada) {
            val usuarioGuardado = preferenciasLogin.getString("saved_user", "")
            val claveGuardada = preferenciasLogin.getString("saved_pass", "")

            enlace.loginUsername.setText(usuarioGuardado)
            enlace.loginPassword.setText(claveGuardada)
            enlace.cbRememberMe.isChecked = true

            contrasenaCargadaDesdePreferencias = true
        }

        enlace.loginButton.setOnClickListener {
            val identificador = enlace.loginUsername.text.toString()
            val clave = enlace.loginPassword.text.toString()

            if (baseDatos.readUser(identificador, clave)) {
                // Persistencia de credenciales según preferencia del usuario
                val editorPreferencias = preferenciasLogin.edit()
                if (enlace.cbRememberMe.isChecked) {
                    editorPreferencias.putString("saved_user", identificador)
                    editorPreferencias.putString("saved_pass", clave)
                    editorPreferencias.putBoolean("is_remembered", true)
                } else {
                    editorPreferencias.clear()
                }
                editorPreferencias.apply()

                val nombreUsuarioReal = baseDatos.getActualUsername(identificador)
                procesarIngresoExitoso(nombreUsuarioReal)
            } else {
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
        }

        enlace.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, PasswordRecoveryActivity::class.java))
        }

        /**
         * Monitor de cambios en el campo de contraseña.
         * Implementa una política de borrado total si se intenta modificar una clave recordada.
         */
        enlace.loginPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Si se detecta borrado en una contraseña cargada por el sistema, se limpia el campo
                if (contrasenaCargadaDesdePreferencias && before > count) {
                    contrasenaCargadaDesdePreferencias = false
                    enlace.loginPassword.setText("")
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // Valida si el contenido actual difiere del almacenado en preferencias
                if (contrasenaCargadaDesdePreferencias && s?.length ?: 0 > 0 &&
                    s.toString() != preferenciasLogin.getString("saved_pass", "")) {
                    contrasenaCargadaDesdePreferencias = false
                }
            }
        })

        var claveVisible = false
        enlace.btnMostrarPasswordLogin.setOnClickListener {
            // Restricción de visibilidad para contraseñas automáticas por seguridad del usuario
            if (contrasenaCargadaDesdePreferencias) {
                Toast.makeText(this, "Por seguridad, reescriba su clave para visualizarla", Toast.LENGTH_SHORT).show()
            } else {
                claveVisible = !claveVisible
                val tipografia = enlace.loginPassword.typeface
                if (claveVisible) {
                    enlace.loginPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    enlace.btnMostrarPasswordLogin.setImageResource(R.drawable.ic_open_eye)
                } else {
                    enlace.loginPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    enlace.btnMostrarPasswordLogin.setImageResource(R.drawable.ic_close_eye)
                }
                enlace.loginPassword.typeface = tipografia
                enlace.loginPassword.setSelection(enlace.loginPassword.text?.length ?: 0)
            }
        }

        // Gestión de autenticación biométrica
        enlace.fingerprintLogin.setOnClickListener {
            val entrada = enlace.loginUsername.text.toString()
            if (entrada.isEmpty()) {
                Toast.makeText(this, "Ingrese identificador de usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (baseDatos.isFingerprintEnabled(entrada)) {
                desplegarAutenticacionBiometrica {
                    val nombreUsuarioReal = baseDatos.getActualUsername(entrada)
                    procesarIngresoExitoso(nombreUsuarioReal)
                }
            } else {
                Toast.makeText(this, "Autenticación biométrica no habilitada", Toast.LENGTH_SHORT).show()
            }
        }

        enlace.signupRedirect.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    /**
     * Finaliza el flujo de login y transiciona a la actividad principal.
     */
    private fun procesarIngresoExitoso(nombreUsuario: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", nombreUsuario)
        startActivity(intent)
        finish()
    }

    /**
     * Inicializa y despliega el diálogo de hardware para validación de huella digital.
     */
    private fun desplegarAutenticacionBiometrica(alExito: () -> Unit) {
        val ejecutor = ContextCompat.getMainExecutor(this)

        val avisoBiometrico = BiometricPrompt(
            this,
            ejecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(resultado)
                    alExito()
                }
            })

        val configuracionAviso = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso mediante huella")
            .setSubtitle("Autentíquese para continuar")
            .setNegativeButtonText("Cancelar")
            .build()

        avisoBiometrico.authenticate(configuracionAviso)
    }
}