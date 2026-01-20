package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.UsuarioApi
import com.intelliworks.intellihome.data.model.LoginResponseDto
import com.intelliworks.intellihome.data.repository.UsuarioRepository
import com.intelliworks.intellihome.databinding.ActivityLoginBinding
import com.intelliworks.intellihome.utils.BaseActivity
import kotlinx.coroutines.launch

class Login : BaseActivity() {

    private lateinit var enlace: ActivityLoginBinding
    private var contrasenaCargadaDesdePreferencias = false

    override fun onResume() {
        super.onResume()
        applyAppAppearance(enlace.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        val preferenciasLogin = getSharedPreferences("login_prefs", MODE_PRIVATE)

        // 1. CARGAR CREDENCIALES RECORDADAS
        if (preferenciasLogin.getBoolean("is_remembered", false)) {
            enlace.loginUsername.setText(preferenciasLogin.getString("saved_user", ""))
            enlace.loginPassword.setText(preferenciasLogin.getString("saved_pass", ""))
            enlace.cbRememberMe.isChecked = true
            contrasenaCargadaDesdePreferencias = true
        }

        // 2. BOTÓN LOGIN (API)
        enlace.loginButton.setOnClickListener {
            val identificador = enlace.loginUsername.text.toString().trim()
            val clave = enlace.loginPassword.text.toString().trim()

            if (identificador.isNotEmpty() && clave.isNotEmpty()) {
                ejecutarLoginApi(identificador, clave)
            } else {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            }
        }

        // 3. RECUPERAR CONTRASEÑA
        enlace.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, PasswordRecoveryActivity::class.java))
        }

        // 4. HUELLA DIGITAL
        enlace.fingerprintLogin.setOnClickListener {
            desplegarAutenticacionBiometrica {
                // Si la huella es válida, podrías intentar un login automático
                // o mostrar un mensaje. Por ahora, lanzamos el aviso de éxito:
                Toast.makeText(this, "Autenticación biométrica exitosa", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. REDIRECCIÓN A REGISTRO
        enlace.signupRedirect.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        configurarVisibilidadContrasena()
    }

    private fun ejecutarLoginApi(identificador: String, clave: String) {
        val api = RetrofitInstance.retrofit.create(UsuarioApi::class.java)
        val repo = UsuarioRepository(api)

        lifecycleScope.launch {
            try {
                val response = repo.loginUsuario(identificador, clave)

                if (response.isSuccessful) {
                    val loginData = response.body()
                    if (loginData != null && loginData.username != null) {
                        gestionarRecordatorio(identificador, clave)

                        // Mensaje de bienvenida traducido (asumiendo que tienes welcome_user en strings)
                        Toast.makeText(this@Login, "${getString(R.string.welcome_user)} ${loginData.nombre}", Toast.LENGTH_SHORT).show()

                        navegarAMain(loginData)
                    }
                } else {
                    Toast.makeText(this@Login, getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@Login, getString(R.string.error_network), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navegarAMain(loginData: LoginResponseDto) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_data", Gson().toJson(loginData))
        startActivity(intent)
        finish()
    }

    private fun gestionarRecordatorio(id: String, pass: String) {
        val editor = getSharedPreferences("login_prefs", MODE_PRIVATE).edit()
        if (enlace.cbRememberMe.isChecked) {
            editor.putString("saved_user", id)
            editor.putString("saved_pass", pass)
            editor.putBoolean("is_remembered", true)
        } else {
            editor.clear()
        }
        editor.apply()
    }

    private fun configurarVisibilidadContrasena() {
        var claveVisible = false
        enlace.btnMostrarPasswordLogin.setOnClickListener {
            claveVisible = !claveVisible
            enlace.loginPassword.inputType = if (claveVisible) {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            enlace.btnMostrarPasswordLogin.setImageResource(if (claveVisible) R.drawable.ic_open_eye else R.drawable.ic_close_eye)
            enlace.loginPassword.setSelection(enlace.loginPassword.text?.length ?: 0)
        }
    }

    private fun desplegarAutenticacionBiometrica(alExito: () -> Unit) {
        val ejecutor = ContextCompat.getMainExecutor(this)
        val avisoBiometrico = BiometricPrompt(this, ejecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(resultado)
                alExito()
            }
        })

        val configuracionAviso = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(android.R.string.cancel))
            .build()

        avisoBiometrico.authenticate(configuracionAviso)
    }
}