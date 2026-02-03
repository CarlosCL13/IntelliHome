package com.intelliworks.intellihome

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.SessionManager
import com.intelliworks.intellihome.data.api.TokenDto
import com.google.firebase.messaging.FirebaseMessaging
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.UsuarioApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Controlador principal de la aplicación.
 * Responsabilidades:
 * 1. Gestionar la navegación entre fragmentos.
 * 2. Sincronizar el token FCM del dispositivo con el Backend (Segundo plano).
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    val usuarioApi = RetrofitInstance.retrofit.create(UsuarioApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Habilita el botón de ajustes en la barra superior base
        showSettingsButton(true)

        // Carga la vista inicial por defecto
        if (savedInstanceState == null) {
            cambiarFragmento(ExploreFragment())
        }

        // --- INICIO SINCRONIZACIÓN FCM ---
        // En lugar de solo suscribirnos al topic, verificamos si este dispositivo
        // ya está registrado en la base de datos de este usuario.
        verificarYSincronizarDispositivo()
        // ---------------------------------

        configurarNavegacion()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    /**
     * Lógica inteligente para registrar el dispositivo en el Backend.
     * Solo envía datos a la API si el token ha cambiado o es una instalación nueva.
     * Esto ahorra batería y datos móviles.
     */
    private fun verificarYSincronizarDispositivo() {
        val userId = SessionManager.obtenerUserId(this)

        // Si por alguna razón no hay ID (error raro), abortamos.
        if (userId.isEmpty()) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fallo al obtener token de Firebase local", task.exception)
                return@addOnCompleteListener
            }

            // 1. Obtener el token real actual de Firebase
            val tokenActual = task.result

            // 2. Obtener el último token que guardamos localmente como "Enviado"
            val tokenGuardado = SessionManager.obtenerTokenSincronizado(this)

            // 3. COMPARACIÓN: ¿Son diferentes?
            if (tokenActual != tokenGuardado) {
                Log.d("FCM", "Token nuevo detectado. Iniciando sincronización con Backend...")
                enviarTokenAlBackend(userId, tokenActual)
            } else {
                Log.d("FCM", "El token ya está sincronizado. No se requiere acción.")
            }
        }
    }

    /**
     * Envía el token a la API usando Coroutines (IO Thread) para no bloquear la UI.
     */
    private fun enviarTokenAlBackend(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Preparamos el objeto para enviar
                val request = TokenDto(fcm_token = token)

                // Llamada a la API (PUT /usuarios/{id}/dispositivos)
                val response = usuarioApi.registrarDispositivo(userId, request)
                if (response.isSuccessful) {
                    Log.i("FCM", "Dispositivo registrado exitosamente en el servidor.")

                    // ¡CRUCIAL! Guardamos en caché que ya lo enviamos.
                    // Así, la próxima vez que abras la app, no entrará a este bloque.
                    SessionManager.guardarTokenSincronizado(this@MainActivity, token)
                } else {
                    Log.e("FCM", "Error del servidor al guardar token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error de conexión al sincronizar token", e)
            }
        }
    }

    /**
     * Configura los listeners del BottomNavigationView para intercambiar fragmentos.
     */
    private fun configurarNavegacion() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    cambiarFragmento(ExploreFragment())
                    true
                }
                R.id.nav_rentals -> {
                    cambiarFragmento(RentalsFragment())
                    true
                }
                R.id.nav_profile -> {
                    cambiarFragmento(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun cambiarFragmento(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragmento)
            .commit()
    }
}