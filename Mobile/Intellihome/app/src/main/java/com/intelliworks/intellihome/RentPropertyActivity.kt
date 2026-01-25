package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import com.intelliworks.intellihome.databinding.ActivityRentPropertyBinding
import com.intelliworks.intellihome.utils.BaseActivity

class RentPropertyActivity : BaseActivity() {

    private lateinit var binding: ActivityRentPropertyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarVista()
        configurarBotonAlquilar()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    /**
     * Configura la vista con los datos de la propiedad (GETS)
     */
    private fun configurarVista() {
        // Aquí se cargarían los datos de la propiedad desde el backend
    }

    /**
     * Configura el botón de alquilar
     */
    private fun configurarBotonAlquilar() {
        binding.btnAlquilarPropiedad.setOnClickListener {
            procesarAlquiler()
        }
    }

    /**
     * Navega a la pantalla de pago
     */
    private fun procesarAlquiler() {
        // Navegar a la pantalla de pago
        val intent = Intent(this, PaymentActivity::class.java)
        startActivity(intent)
    }
}
