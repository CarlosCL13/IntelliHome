package com.intelliworks.intellihome

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.utils.BaseActivity

/**
 * Controlador principal de la aplicación post-login.
 * Gestiona la navegación inferior y la inyección de fragmentos.
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

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

        configurarNavegacion()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
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