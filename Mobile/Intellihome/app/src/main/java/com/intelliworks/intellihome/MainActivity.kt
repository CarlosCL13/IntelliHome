package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.data.model.LoginResponseDto
import com.google.gson.Gson

class MainActivity : BaseActivity() {

    private lateinit var enlace: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = ActivityMainBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        enlace.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        // Recuperar datos del Intent (enviados desde Login)
        val datosUsuarioJson = intent.getStringExtra("user_data")

        if (!datosUsuarioJson.isNullOrEmpty()) {
            try {
                val usuario = Gson().fromJson(datosUsuarioJson, LoginResponseDto::class.java)
                desplegarInformacionServidor(usuario)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al procesar datos del servidor")
            }
        } else {
            Log.e("MainActivity", "No se recibieron datos del servidor")
        }
    }

    private fun desplegarInformacionServidor(u: LoginResponseDto) {
        enlace.apply {
            // El nombre y apellido se muestran igual en cualquier idioma
            txtNombre.text = "${u.nombre ?: ""} ${u.apellidos ?: ""}".trim()

            // --- Uso de recursos traducibles con parámetros ---

            // @string/label_username -> "Usuario: @%1$s" o "Username: @%1$s"
            txtUsername.text = getString(R.string.label_username, u.username ?: "")

            // @string/label_user_id -> "ID de Usuario: %1$d" o "User ID: %1$d"
            txtUserId.text = getString(R.string.label_user_id, u.id ?: 0)

            // @string/label_email -> "Correo: %1$s" o "Email: %1$s"
            txtCorreo.text = getString(R.string.label_email, u.correo ?: getString(R.string.data_not_available))

            // @string/label_phone -> "Teléfono: %1$s" o "Phone: %1$s"
            txtTelefono.text = getString(R.string.label_phone, u.telefono ?: getString(R.string.data_not_available))

            // Estado de cuenta
            val estado = u.estadoCuenta?.uppercase() ?: getString(R.string.data_not_available)
            txtEstadoCuenta.text = getString(R.string.label_status, estado)

            // --- Traducción lógica del Rol ---
            val nombreRol = when (u.rolId) {
                1 -> getString(R.string.role_admin)
                2 -> getString(R.string.role_user)
                3 -> getString(R.string.role_tech)
                else -> getString(R.string.role_guest)
            }

            // @string/label_role -> "Rol: %1$s" o "Role: %1$s"
            txtRol.text = getString(R.string.label_role, nombreRol)
        }
    }
    private fun cerrarSesion() {
        // Limpiar preferencias de login (remember me)
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Ir a Login y limpiar back stack
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }


    override fun onResume() {
        super.onResume()
        applyAppAppearance(enlace.root)
    }
}