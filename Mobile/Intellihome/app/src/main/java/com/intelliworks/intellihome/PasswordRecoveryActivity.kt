package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.intelliworks.intellihome.databinding.ActivityPasswordRecoveryBinding
import com.intelliworks.intellihome.utils.BaseActivity

/**
 * Controlador para el proceso de recuperación de credenciales.
 * Implementa un flujo de dos pasos: identificación del perfil mediante pregunta de seguridad
 * y validación de nueva clave bajo estándares de complejidad alfanumérica.
 */
class PasswordRecoveryActivity : BaseActivity() {

    private lateinit var enlace: ActivityPasswordRecoveryBinding
    private lateinit var baseDatos: DatabaseHelper

    override fun onResume() {
        super.onResume()
        applyAppAppearance(enlace.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = ActivityPasswordRecoveryBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        baseDatos = DatabaseHelper(this)

        configurarVisibilidadContrasena(enlace.etNewPassword, enlace.btnShowNewPass)
        configurarVisibilidadContrasena(enlace.etConfirmNewPassword, enlace.btnShowConfirmPass)

        // Fase 1: Identificación y recuperación de pregunta de desafío
        enlace.btnGetQuestion.setOnClickListener {
            val identificador = enlace.etRecoveryUser.text.toString()
            val preguntaEscrita = baseDatos.getRecoveryQuestion(identificador)

            if (preguntaEscrita != null) {
                enlace.tvQuestionText.text = preguntaEscrita
                enlace.tvQuestionText.visibility = View.VISIBLE
                enlace.etRecoveryAnswer.visibility = View.VISIBLE
                enlace.layoutNewPassword.visibility = View.VISIBLE
                enlace.btnGetQuestion.isEnabled = false
            } else {
                Toast.makeText(this, "Perfil de usuario no identificado", Toast.LENGTH_SHORT).show()
            }
        }

        // Fase 2: Validación de respuesta y actualización de credenciales
        enlace.btnResetPassword.setOnClickListener {
            val usuario = enlace.etRecoveryUser.text.toString().trim()
            val respuesta = enlace.etRecoveryAnswer.text.toString().trim()
            val nuevaClave = enlace.etNewPassword.text.toString().trim()
            val confirmacionClave = enlace.etConfirmNewPassword.text.toString().trim()

            if (nuevaClave.isEmpty()) {
                enlace.etNewPassword.error = "Campo requerido"
                return@setOnClickListener
            }

            // Cumplimiento de políticas de seguridad alfanumérica
            if (!validarFormatoClave(nuevaClave)) {
                enlace.etNewPassword.error = "Mínimo 8 caracteres, incluyendo letras y números"
                enlace.etNewPassword.requestFocus()
                return@setOnClickListener
            }

            if (nuevaClave != confirmacionClave) {
                enlace.etConfirmNewPassword.error = "La confirmación no coincide"
                return@setOnClickListener
            }

            // Ejecución de cambios en persistencia tras validación de desafío
            if (baseDatos.verifyRecoveryAnswer(usuario, respuesta)) {
                if (baseDatos.updatePassword(usuario, nuevaClave)) {

                    // Invalida sesiones recordadas para forzar re-autenticación
                    val preferencias = getSharedPreferences("login_prefs", MODE_PRIVATE)
                    preferencias.edit().clear().apply()

                    Toast.makeText(this, "Cambio de credenciales exitoso", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Validación de seguridad fallida", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Valida la integridad de la contraseña mediante expresiones regulares.
     * Requisitos: Longitud mínima de 8 caracteres y composición alfanumérica.
     */
    private fun validarFormatoClave(clave: String): Boolean {
        val patronClave = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$".toRegex()
        return clave.matches(patronClave)
    }

    /**
     * Gestiona el cambio dinámico del tipo de entrada para los campos de contraseña.
     */
    private fun configurarVisibilidadContrasena(campoTexto: EditText, botonAccion: ImageButton) {
        var visible = false
        botonAccion.setOnClickListener {
            visible = !visible
            val tipografia = campoTexto.typeface
            if (visible) {
                campoTexto.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                botonAccion.setImageResource(R.drawable.ic_open_eye)
            } else {
                campoTexto.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                botonAccion.setImageResource(R.drawable.ic_close_eye)
            }
            campoTexto.typeface = tipografia
            campoTexto.setSelection(campoTexto.text.length)
        }
    }
}