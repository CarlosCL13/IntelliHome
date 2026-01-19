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

    private var isPasswordLoadedFromPrefs = false

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAppAppearance(binding.root)

        databaseHelper = DatabaseHelper(this)

        // --- LÓGICA DE RECUÉRDAME (LECTURA) ---
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val isRemembered = prefs.getBoolean("is_remembered", false)

        if (isRemembered) {
            val savedUser = prefs.getString("saved_user", "")
            val savedPass = prefs.getString("saved_pass", "")

            binding.loginUsername.setText(savedUser)
            binding.loginPassword.setText(savedPass)
            binding.cbRememberMe.isChecked = true

            // Marcamos que la contraseña actual es la guardada
            isPasswordLoadedFromPrefs = true
        }

        binding.loginButton.setOnClickListener {
            val identifier = binding.loginUsername.text.toString()
            val password = binding.loginPassword.text.toString()

            if (databaseHelper.readUser(identifier, password)) {
                // --- LÓGICA DE RECUÉRDAME (ESCRITURA) ---
                val editor = prefs.edit()
                if (binding.cbRememberMe.isChecked) {
                    editor.putString("saved_user", identifier)
                    editor.putString("saved_pass", password)
                    editor.putBoolean("is_remembered", true)
                } else {
                    editor.clear() // Borra todo si se desmarca
                }
                editor.apply()

                val realUsername = databaseHelper.getActualUsername(identifier)
                loginSuccess(realUsername)
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        // --- LÓGICA DE OLVIDÉ MI CONTRASEÑA ---
        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, PasswordRecoveryActivity::class.java)
            startActivity(intent)
        }

        // --- PROTECCIÓN DE CONTRASEÑA RECORDADA ---
        binding.loginPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Si el usuario intenta borrar un solo carácter (before > count)
                // y la contraseña es la cargada de los prefs:
                if (isPasswordLoadedFromPrefs && before > count) {
                    isPasswordLoadedFromPrefs = false // Ya no es la de los prefs
                    binding.loginPassword.setText("") // Borramos todo
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // Si el usuario empieza a escribir algo nuevo, ya no es la de los prefs
                if (isPasswordLoadedFromPrefs && s?.length ?: 0 > 0 && s.toString() != prefs.getString("saved_pass", "")) {
                    isPasswordLoadedFromPrefs = false
                }
            }
        })

        // --- TU LÓGICA DEL OJITO ---
        var passwordVisible = false
        // --- LÓGICA DEL OJITO (MODIFICADA) ---
        binding.btnMostrarPasswordLogin.setOnClickListener {
            // Si la contraseña es la recordada, NO permitimos verla
            if (isPasswordLoadedFromPrefs) {
                Toast.makeText(this, "Por seguridad, escriba la contraseña de nuevo para verla", Toast.LENGTH_SHORT).show()
            } else {
                // Lógica normal del ojito que ya tienes
                passwordVisible = !passwordVisible
                val tf = binding.loginPassword.typeface
                if (passwordVisible) {
                    binding.loginPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    binding.btnMostrarPasswordLogin.setImageResource(R.drawable.ic_open_eye)
                } else {
                    binding.loginPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    binding.btnMostrarPasswordLogin.setImageResource(R.drawable.ic_close_eye)
                }
                binding.loginPassword.typeface = tf
                binding.loginPassword.setSelection(binding.loginPassword.text?.length ?: 0)
            }
        }

        // --- TU LÓGICA DE HUELLA Y REDIRECT ---
        binding.fingerprintLogin.setOnClickListener {
            val input = binding.loginUsername.text.toString()
            if (input.isEmpty()) {
                Toast.makeText(this, "Ingresa tu usuario, correo o teléfono primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (databaseHelper.isFingerprintEnabled(input)) {
                showBiometricPrompt {
                    val realUsername = databaseHelper.getActualUsername(input)
                    loginSuccess(realUsername)
                }
            } else {
                Toast.makeText(this, "La huella no está habilitada para este usuario", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupRedirect.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    private fun loginSuccess(username: String) {
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
