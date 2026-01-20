package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.UsuarioApi
import com.intelliworks.intellihome.data.model.RestablecerRequestDto
import com.intelliworks.intellihome.databinding.ActivityPasswordRecoveryBinding
import com.intelliworks.intellihome.utils.BaseActivity
import kotlinx.coroutines.launch

class PasswordRecoveryActivity : BaseActivity() {

    private lateinit var enlace: ActivityPasswordRecoveryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = ActivityPasswordRecoveryBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        configurarVisibilidadContrasena(enlace.etNewPassword, enlace.btnShowNewPass)
        configurarVisibilidadContrasena(enlace.etConfirmNewPassword, enlace.btnShowConfirmPass)

        val api = RetrofitInstance.retrofit.create(UsuarioApi::class.java)

        // Fase 1: Pedir pregunta al SERVIDOR
        enlace.btnGetQuestion.setOnClickListener {
            val identificador = enlace.etRecoveryUser.text.toString().trim()

            if (identificador.isEmpty()) {
                Toast.makeText(this, "Ingrese su usuario o correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val respuesta = api.obtenerPregunta(identificador)
                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        val data = respuesta.body()!!

                        enlace.tvQuestionText.text = data.pregunta
                        enlace.tvQuestionText.visibility = View.VISIBLE
                        enlace.etRecoveryAnswer.visibility = View.VISIBLE
                        enlace.layoutNewPassword.visibility = View.VISIBLE
                        enlace.btnGetQuestion.isEnabled = false
                    } else {
                        Toast.makeText(this@PasswordRecoveryActivity, "Perfil no encontrado en el servidor", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PasswordRecoveryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Fase 2: Enviar nueva clave al SERVIDOR
        enlace.btnResetPassword.setOnClickListener {
            val usuario = enlace.etRecoveryUser.text.toString().trim()
            val respuestaSec = enlace.etRecoveryAnswer.text.toString().trim()
            val nuevaClave = enlace.etNewPassword.text.toString().trim()
            val confirmacion = enlace.etConfirmNewPassword.text.toString().trim()

            if (!validarFormatoClave(nuevaClave)) {
                enlace.etNewPassword.error = "Mínimo 8 caracteres, alfanumérica"
                return@setOnClickListener
            }

            if (nuevaClave != confirmacion) {
                enlace.etConfirmNewPassword.error = "No coincide"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Ahora enviamos los campos por separado como espera tu Python Form(...)
                    val result = api.restablecerContrasena(usuario, nuevaClave, respuestaSec)

                    if (result.isSuccessful) {
                        Toast.makeText(this@PasswordRecoveryActivity, "Contraseña actualizada", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // Manejo de errores específicos del servidor
                        Toast.makeText(this@PasswordRecoveryActivity, "Validación fallida", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PasswordRecoveryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validarFormatoClave(clave: String): Boolean {
        val patronClave = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$".toRegex()
        return clave.matches(patronClave)
    }

    private fun configurarVisibilidadContrasena(campoTexto: EditText, botonAccion: ImageButton) {
        var visible = false
        botonAccion.setOnClickListener {
            visible = !visible
            campoTexto.inputType = if (visible) {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            botonAccion.setImageResource(if (visible) R.drawable.ic_open_eye else R.drawable.ic_close_eye)
            campoTexto.setSelection(campoTexto.text.length)
        }
    }
}