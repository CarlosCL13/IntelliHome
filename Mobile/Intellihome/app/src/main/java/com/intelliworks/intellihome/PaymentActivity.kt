package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import com.intelliworks.intellihome.databinding.ActivityPaymentBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.RegisterHelper

class PaymentActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var usarTarjetaGuardada = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarCheckbox()
        configurarBotonPagar()
        configurarSelectorFecha()
        cargarDatosTarjetaGuardada()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    /**
     * Configura el checkbox para alternar entre tarjeta guardada y nueva tarjeta
     */
    private fun configurarCheckbox() {
        binding.cbConfirmarTarjeta.setOnCheckedChangeListener { _, isChecked ->
            usarTarjetaGuardada = isChecked
            // Si el checkbox está marcado, muestra la tarjeta guardada, si no está marcado, muestra el formulario de nueva tarjeta
            if (isChecked) {
                // Mostrar sección de tarjeta guardada (CVV)
                binding.contenedorTajetaGuardada.visibility = View.VISIBLE
                binding.contenedorTarjetaNueva.visibility = View.GONE
            } else {
                // Mostrar formulario de nueva tarjeta
                binding.contenedorTajetaGuardada.visibility = View.GONE
                binding.contenedorTarjetaNueva.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Configura el selector de fecha de expiración
     */
    private fun configurarSelectorFecha() {
        // Deshabilitar input manual y lo configura como clickable
        binding.etFechaNuevaTarjeta.isFocusable = false
        binding.etFechaNuevaTarjeta.isClickable = true
        // Mostrar selector de fecha al tocar
        binding.etFechaNuevaTarjeta.setOnClickListener {
            RegisterHelper.mostrarSelectorMesAnioTarjeta(this) { mesStr, anioStr ->
                binding.etFechaNuevaTarjeta.setText("$mesStr/$anioStr")
            }
        }
    }

    /**
     * Carga los datos de la tarjeta guardada desde el backend o preferencias
     */
    private fun cargarDatosTarjetaGuardada() {
        // Aquí se cargarían los últimos 4 dígitos de la tarjeta guardada
    }

    /**
     * Configura el botón de pagar
     */
    private fun configurarBotonPagar() {
        binding.btnProcesarPago.setOnClickListener {
            if (validarDatosPago()) {
                procesarPago()
            }
        }
    }

    /**
     * Valida los datos de pago según el método seleccionado
     */
    private fun validarDatosPago(): Boolean {

        // Si se seleccionó usar tarjeta guardada, validar CVV. Si no se seleccionó, validar datos de nueva tarjeta
        if (usarTarjetaGuardada) {

            // Validar que se haya ingresado el CVV
            val cvv = binding.etCvv.text.toString()
            if (cvv.isEmpty() || cvv.length < 3) {
                binding.contenedorCvv.error = "Ingrese un CVV válido"
                return false
            }

            // Si es válido, limpiar el error
            binding.contenedorCvv.error = null
            return true

        } else {

            // Validar datos de nueva tarjeta
            var esValido = true

            // Valida el número de tarjeta, si no esta vacío y cumple con el tamaño mínimo se agrega al formulario, sino se muestra error
            val numeroTarjeta = binding.etNumeroNuevaTarjeta.text.toString()
            if (numeroTarjeta.isEmpty() || numeroTarjeta.length < 16) {
                binding.contenedorNumeroNuevaTarjeta.error = "Ingrese un número de tarjeta válido"
                esValido = false
            } else {
                binding.contenedorNumeroNuevaTarjeta.error = null
            }

            // Valida el nombre del titular, si no está vacío se agrega al formulario, sino se muestra error
            val nombreTitular = binding.etTitularNuevaTarjeta.text.toString()
            if (nombreTitular.isEmpty()) {
                binding.contenedorTitularNuevaTarjeta.error = "Ingrese el nombre del titular"
                esValido = false
            } else {
                binding.contenedorTitularNuevaTarjeta.error = null
            }

            // Valida la fecha de expiración, si no está vacío se agrega al formulario, sino se muestra error
            val fechaExpiracion = binding.etFechaNuevaTarjeta.text.toString()
            if (fechaExpiracion.isEmpty() || fechaExpiracion.length < 5) {
                binding.contenedorFechaNuevaTarjeta.error = "Ingrese fecha válida (MM/AA)"
                esValido = false
            } else {
                binding.contenedorFechaNuevaTarjeta.error = null
            }

            // Valida el CVV, si no está vacío y tiene 3 dígitos se agrega al formulario, sino se muestra error
            val cvv = binding.etCvvNuevaTarjeta.text.toString()
            if (cvv.isEmpty() || cvv.length < 3) {
                binding.contenedorCvvNuevaTarjeta.error = "Ingrese un CVV válido"
                esValido = false
            } else {
                binding.contenedorCvvNuevaTarjeta.error = null
            }

            return esValido
        }
    }

    /**
     * Procesa el pago de la propiedad
     */
    private fun procesarPago() {
        // Aquí iría la lógica para:
        // 1. Crear el objeto de pago con los datos correspondientes
        // 2. Enviar la solicitud al backend
        // 3. Procesar la respuesta (éxito o error)
        // 4. Navegar a pantalla de confirmación o mostrar error
    }
}
