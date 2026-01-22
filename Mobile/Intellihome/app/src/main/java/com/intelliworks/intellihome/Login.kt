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
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.desencriptarClave
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.desencriptarToken

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

        // 4. AUX 1: Función para llamar al endpoint de login biométrico
        fun loginPorToken(tokenPublico: String) {
            val api = RetrofitInstance.retrofit.create(UsuarioApi::class.java)
            val repo = UsuarioRepository(api)

            lifecycleScope.launch {
                try {
                    val response = repo.buscarPorToken(tokenPublico)
                    if (response.isSuccessful) {
                        val loginData = response.body()
                        if (loginData != null && loginData.username != null) {
                            // Mensaje de bienvenida
                            Toast.makeText(this@Login, "Bienvenido ${loginData.nombre}", Toast.LENGTH_SHORT).show()
                            navegarAMain(loginData)
                        } else {
                            Toast.makeText(this@Login, "Token biométrico inválido", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@Login, "Token biométrico inválido", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@Login, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 4. AUX 2: Función para manejar el login con huella digital
        fun loginConHuella() {
            // Recupera el token encriptado y el IV de SharedPreferences
            val prefs = getSharedPreferences("biometric_prefs", MODE_PRIVATE)
            val tokenEncriptado = prefs.getString("token_encriptado", null)
            val tokenIv = prefs.getString("token_iv", null)

            if (tokenEncriptado.isNullOrEmpty() || tokenIv.isNullOrEmpty()) {
                Toast.makeText(this, "No hay datos biométricos registrados", Toast.LENGTH_LONG).show()
                return
            }

            try {
                // Desencripta el token usando la clave biométrica
                val cifrado = desencriptarClave(android.util.Base64.decode(tokenIv, android.util.Base64.DEFAULT))
                val tokenPublico = desencriptarToken(tokenEncriptado, tokenIv, cifrado)

                // Llama al endpoint de login biométrico con el token público
                loginPorToken(tokenPublico)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al desencriptar token biométrico: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // 4. HUELLA DIGITAL
        enlace.fingerprintLogin.setOnClickListener {
            desplegarAutenticacionBiometrica {
                // Si la autenticación biométrica es exitosa, intentamos login automático con token biométrico
                loginConHuella()
            }
        }

        // 5. REDIRECCIÓN A REGISTRO
        enlace.signupRedirect.setOnClickListener {
            verificarServidorYIrARegistro()
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

    private fun verificarServidorYIrARegistro() {
        val catalogosApi = RetrofitInstance.retrofit.create(
            com.intelliworks.intellihome.data.api.CatalogosApi::class.java
        )
        val catalogosRepo = com.intelliworks.intellihome.data.repository.CatalogosRepository(catalogosApi)

        lifecycleScope.launch {
            try {
                val response = catalogosRepo.getHobbies()
                if (response.isSuccessful) {
                    // ✅ Servidor disponible → navegar
                    startActivity(Intent(this@Login, Register::class.java))
                } else {
                    Toast.makeText(
                        this@Login,
                        getString(R.string.error_network),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@Login,
                    getString(R.string.error_network),
                    Toast.LENGTH_LONG
                ).show()
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