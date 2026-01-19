package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.intelliworks.intellihome.databinding.ActivityPasswordRecoveryBinding
import com.intelliworks.intellihome.utils.BaseActivity

class PasswordRecoveryActivity : BaseActivity() {

    private lateinit var binding: ActivityPasswordRecoveryBinding
    private lateinit var db: DatabaseHelper

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        // Y en el onCreate las activas:
        setupPasswordVisibility(binding.etNewPassword, binding.btnShowNewPass)
        setupPasswordVisibility(binding.etConfirmNewPassword, binding.btnShowConfirmPass)

        // PASO 1: Buscar la pregunta del usuario
        binding.btnGetQuestion.setOnClickListener {
            val user = binding.etRecoveryUser.text.toString()
            val pregunta = db.getRecoveryQuestion(user)

            if (pregunta != null) {
                binding.tvQuestionText.text = pregunta
                binding.tvQuestionText.visibility = View.VISIBLE
                binding.etRecoveryAnswer.visibility = View.VISIBLE
                binding.layoutNewPassword.visibility = View.VISIBLE
                binding.btnGetQuestion.isEnabled = false // Evitar cambios accidentales
            } else {
                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
            }
        }

        // PASO 2: Validar respuesta y actualizar
        binding.btnResetPassword.setOnClickListener {
            val user = binding.etRecoveryUser.text.toString().trim()
            val answer = binding.etRecoveryAnswer.text.toString().trim()
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmNewPassword.text.toString().trim()

            // 1. Validar que no haya campos vacíos
            if (newPass.isEmpty()) {
                binding.etNewPassword.error = "Escribe una nueva contraseña"
                return@setOnClickListener
            }

            // 2. VALIDACIÓN ALFANUMÉRICA Y LONGITUD
            if (!validatePassword(newPass)) {
                binding.etNewPassword.error = "Debe tener al menos 8 caracteres, letras y números"
                binding.etNewPassword.requestFocus()
                return@setOnClickListener
            }

            // 3. Validar coincidencia
            if (newPass != confirmPass) {
                binding.etConfirmNewPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // 4. Procesar en Base de Datos
            if (db.verifyRecoveryAnswer(user, answer)) {
                if (db.updatePassword(user, newPass)) {

                    // OPCIONAL: Limpiar el "Recuérdame" del Login ya que la clave cambió
                    val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    Toast.makeText(this, "Contraseña actualizada exitosamente", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Respuesta de seguridad incorrecta", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun validatePassword(password: String): Boolean {
        // Explicación del Regex:
        // ^(?=.*[a-zA-Z]) -> Debe contener al menos una letra
        // (?=.*[0-9])     -> Debe contener al menos un número
        // .{8,}           -> Debe tener mínimo 8 caracteres
        val passwordPattern = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$".toRegex()
        return password.matches(passwordPattern)
    }
    // Función reutilizable para alternar visibilidad
    private fun setupPasswordVisibility(editText: EditText, button: ImageButton) {
        var isVisible = false
        button.setOnClickListener {
            isVisible = !isVisible
            val tf = editText.typeface
            if (isVisible) {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                button.setImageResource(R.drawable.ic_open_eye)
            } else {
                editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                button.setImageResource(R.drawable.ic_close_eye)
            }
            editText.typeface = tf
            editText.setSelection(editText.text.length)
        }
    }
}