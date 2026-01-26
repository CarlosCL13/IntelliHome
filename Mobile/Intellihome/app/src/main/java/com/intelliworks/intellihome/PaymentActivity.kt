package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.databinding.ActivityPaymentBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.RegisterHelper
import com.intelliworks.intellihome.utils.SessionManager

class PaymentActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var usarTarjetaGuardada = true
    private var currentProperty: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val propertyJson = intent.getStringExtra("property_data")
        if (propertyJson != null) {
            currentProperty = Gson().fromJson(propertyJson, Property::class.java)
            // CORREGIDO: Usa el string formateado "Pay $XXXX"
            val precio = currentProperty?.precio ?: "0"
            binding.tvTituloAlquilar.text = getString(R.string.title_pay_amount, precio)
        } else {
            // CORREGIDO: Error genérico
            Toast.makeText(this, getString(R.string.error_loading_property_data), Toast.LENGTH_SHORT).show()
            finish()
        }

        configurarCheckbox()
        configurarBotonPagar()
        configurarSelectorFecha()
        cargarDatosTarjetaGuardada()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    private fun configurarCheckbox() {
        binding.cbConfirmarTarjeta.setOnCheckedChangeListener { _, isChecked ->
            usarTarjetaGuardada = isChecked
            if (isChecked) {
                binding.contenedorTajetaGuardada.visibility = View.VISIBLE
                binding.contenedorTarjetaNueva.visibility = View.GONE
            } else {
                binding.contenedorTajetaGuardada.visibility = View.GONE
                binding.contenedorTarjetaNueva.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarSelectorFecha() {
        binding.etFechaNuevaTarjeta.isFocusable = false
        binding.etFechaNuevaTarjeta.isClickable = true
        binding.etFechaNuevaTarjeta.setOnClickListener {
            RegisterHelper.mostrarSelectorMesAnioTarjeta(this) { mesStr, anioStr ->
                binding.etFechaNuevaTarjeta.setText("$mesStr/$anioStr")
            }
        }
    }

    private fun cargarDatosTarjetaGuardada() {
        // Simulación
    }

    private fun configurarBotonPagar() {
        binding.btnProcesarPago.setOnClickListener {
            if (validarDatosPago()) {
                procesarPago()
            }
        }
    }

    private fun validarDatosPago(): Boolean {
        if (usarTarjetaGuardada) {
            val cvv = binding.etCvv.text.toString()
            if (cvv.isEmpty() || cvv.length < 3) {
                // Podrías agregar un string específico para esto, o usar uno genérico
                binding.contenedorCvv.error = "Invalid CVV"
                return false
            }
            binding.contenedorCvv.error = null
            return true
        } else {
            var esValido = true

            val numero = binding.etNumeroNuevaTarjeta.text.toString()
            if (numero.length < 16) {
                binding.contenedorNumeroNuevaTarjeta.error = "Invalid Card"
                esValido = false
            } else binding.contenedorNumeroNuevaTarjeta.error = null

            if (binding.etTitularNuevaTarjeta.text.isNullOrEmpty()) {
                binding.contenedorTitularNuevaTarjeta.error = getString(R.string.error_empty_fields)
                esValido = false
            } else binding.contenedorTitularNuevaTarjeta.error = null

            if (binding.etFechaNuevaTarjeta.text.isNullOrEmpty()) {
                binding.contenedorFechaNuevaTarjeta.error = getString(R.string.error_empty_fields)
                esValido = false
            } else binding.contenedorFechaNuevaTarjeta.error = null

            if (binding.etCvvNuevaTarjeta.text.toString().length < 3) {
                binding.contenedorCvvNuevaTarjeta.error = "Invalid CVV"
                esValido = false
            } else binding.contenedorCvvNuevaTarjeta.error = null

            return esValido
        }
    }

    private fun procesarPago() {
        binding.btnProcesarPago.isEnabled = false
        // CORREGIDO: String "Processing..."
        binding.btnProcesarPago.text = getString(R.string.status_processing)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                realizarRenta()
            } catch (e: Exception) {
                // CORREGIDO: Error genérico
                Toast.makeText(this, getString(R.string.error_payment_processing), Toast.LENGTH_SHORT).show()
                binding.btnProcesarPago.isEnabled = true
                binding.btnProcesarPago.text = getString(R.string.btn_process_payment)
            }
        }, 2000)
    }

    private fun realizarRenta() {
        val propiedad = currentProperty ?: return
        val currentUserId = SessionManager.obtenerUserId(this)

        if (currentUserId.isEmpty()) {
            // CORREGIDO: Mensaje de error de usuario
            Toast.makeText(this, getString(R.string.error_user_unknown), Toast.LENGTH_LONG).show()
            return
        }

        val propiedadRentada = propiedad.copy(
            rentedByUserId = currentUserId
        )

        PropertyRepository.saveProperty(this, propiedadRentada)

        // CORREGIDO: Textos del diálogo de éxito
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_payment_success))
            .setMessage(getString(R.string.msg_payment_success))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_go_home)) { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .show()
    }
}