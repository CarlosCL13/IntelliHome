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
import android.content.SharedPreferences
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.ArrendamientoRepository
import com.intelliworks.intellihome.data.repository.UsuarioRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var usarTarjetaGuardada = true
    private var currentProperty: Property? = null
    private lateinit var arrendamientoRepository: ArrendamientoRepository
    private lateinit var usuarioRepository: UsuarioRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var fechaInicioSeleccionada: Long? = null
    private var fechaFinSeleccionada: Long? = null
    private var precioPorNoche: Double = 0.0
    private var propiedadId: Int = 0
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val backendDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        arrendamientoRepository = ArrendamientoRepository()
        usuarioRepository = UsuarioRepository(RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.UsuarioApi::class.java))
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        precioPorNoche = intent.getDoubleExtra("PROPERTY_PRICE", 0.0)
        propiedadId = intent.getIntExtra("PROPERTY_ID", 0)

        configurarCheckbox()
        configurarBotonPagar()
        configurarSelectorFecha()
        configurarSelectorFechasAlquiler()
        cargarDatosTarjetaGuardada()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    private fun configurarSelectorFechasAlquiler() {
        binding.btnSeleccionarFechas.setOnClickListener {
            mostrarSelectorRangoFechas()
        }
    }

    private fun mostrarSelectorRangoFechas() {

        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar fechas de alquiler")
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            // Validar que no sean el mismo día
            if (startDate == endDate) {
                Toast.makeText(
                    this,
                    "La fecha de inicio y fin no pueden ser el mismo día",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnPositiveButtonClickListener
            }

            // Se debe de obtener fechas bloqueadas que vienen de RentPropertyActivity y validar con validarFechasDisponibles()
            // Por ahora solo guardar las fechas seleccionadas
            fechaInicioSeleccionada = startDate
            fechaFinSeleccionada = endDate
            mostrarFechasSeleccionadas()
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun validarFechasDisponibles(inicio: Long, fin: Long, fechasBloqueadas: List<Pair<Long, Long>>): Boolean {
        for (bloqueo in fechasBloqueadas) {
            val bloqueoInicio = bloqueo.first
            val bloqueoFin = bloqueo.second

            // Verificar si hay superposición de fechas
            if (inicio <= bloqueoFin && fin >= bloqueoInicio) {
                return false
            }
        }
        return true
    }

    // Muestra las fechas seleccionadas en la UI
    private fun mostrarFechasSeleccionadas() {
        if (fechaInicioSeleccionada != null && fechaFinSeleccionada != null) {
            val fechaInicio = dateFormat.format(Date(fechaInicioSeleccionada!!))
            val fechaFin = dateFormat.format(Date(fechaFinSeleccionada!!))

            val textoFechas = getString(R.string.payment_dates_selected, fechaInicio, fechaFin)
            binding.tvFechasSeleccionadas.text = textoFechas
            binding.tvFechasSeleccionadas.visibility = View.VISIBLE

            val noches = calcularNoches(fechaInicioSeleccionada!!, fechaFinSeleccionada!!)
            binding.tvTotalNoches.text = getString(R.string.payment_total_nights, noches)
            binding.tvTotalNoches.visibility = View.VISIBLE

            val totalPagar = precioPorNoche * noches
            binding.tvTotalPagar.text = "Total a pagar: ₡%.2f".format(totalPagar)
            binding.tvTotalPagar.visibility = View.VISIBLE
        }
    }

    private fun calcularNoches(inicio: Long, fin: Long): Int {
        val diferenciaMs = fin - inicio
        return (diferenciaMs / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun configurarCheckbox() {
        binding.cbConfirmarTarjeta.setOnCheckedChangeListener { _, isChecked ->
            usarTarjetaGuardada = isChecked
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
        val userId = sharedPreferences.getInt("usuario_id", 0)

        if (userId == 0) {
            return
        }

        lifecycleScope.launch {
            try {
                val response = usuarioRepository.obtenerUltimos4Tarjeta(userId)

                if (response.isSuccessful) {
                    val ultimos4 = response.body()?.ultimos_4

                    if (!ultimos4.isNullOrEmpty()) {
                        binding.cbConfirmarTarjeta.text = "Usar tarjeta guardada (**** $ultimos4)"
                    } else {
                        // No hay tarjeta guardada, ocultar la opción
                        binding.cbConfirmarTarjeta.isChecked = false
                        binding.cbConfirmarTarjeta.visibility = View.GONE
                        binding.contenedorTajetaGuardada.visibility = View.GONE
                        binding.contenedorTarjetaNueva.visibility = View.VISIBLE
                        usarTarjetaGuardada = false
                    }
                }
            } catch (e: Exception) {
                binding.cbConfirmarTarjeta.isChecked = false
                binding.cbConfirmarTarjeta.visibility = View.GONE
                binding.contenedorTajetaGuardada.visibility = View.GONE
                binding.contenedorTarjetaNueva.visibility = View.VISIBLE
                usarTarjetaGuardada = false
            }
        }
    }

    private fun configurarBotonPagar() {
        binding.btnProcesarPago.setOnClickListener {
            if (validarDatosPago()) {
                procesarPago()
            }
        }
    }

    private fun validarDatosPago(): Boolean {

        if (fechaInicioSeleccionada == null || fechaFinSeleccionada == null) {
            Toast.makeText(this, getString(R.string.payment_error_no_dates), Toast.LENGTH_SHORT).show()
            return false
        }

        if (usarTarjetaGuardada) {

            val cvv = binding.etCvv.text.toString()
            if (cvv.isEmpty() || cvv.length < 3) {
                binding.contenedorCvv.error = "Ingrese un CVV válido"
                return false
            }
            binding.contenedorCvv.error = null
            return true

        } else {

            var esValido = true

            val numeroTarjeta = binding.etNumeroNuevaTarjeta.text.toString()
            if (numeroTarjeta.isEmpty() || numeroTarjeta.length < 16) {
                binding.contenedorNumeroNuevaTarjeta.error = "Ingrese un número de tarjeta válido"
                esValido = false
            } else {
                binding.contenedorNumeroNuevaTarjeta.error = null
            }

            val nombreTitular = binding.etTitularNuevaTarjeta.text.toString()
            if (nombreTitular.isEmpty()) {
                binding.contenedorTitularNuevaTarjeta.error = "Ingrese el nombre del titular"
                esValido = false
            } else {
                binding.contenedorTitularNuevaTarjeta.error = null
            }

            val fechaExpiracion = binding.etFechaNuevaTarjeta.text.toString()
            if (fechaExpiracion.isEmpty() || fechaExpiracion.length < 5) {
                binding.contenedorFechaNuevaTarjeta.error = "Ingrese fecha válida (MM/AA)"
                esValido = false
            } else {
                binding.contenedorFechaNuevaTarjeta.error = null
            }

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

    private fun procesarPago() {

        val inquilinoId = sharedPreferences.getInt("usuario_id", 0)

        if (inquilinoId == 0) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        if (propiedadId == 0) {
            Toast.makeText(this, "Error: Propiedad no identificada", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaInicio = backendDateFormat.format(Date(fechaInicioSeleccionada!!))
        val fechaFin = backendDateFormat.format(Date(fechaFinSeleccionada!!))

        binding.btnProcesarPago.isEnabled = false
        binding.btnProcesarPago.text = "Procesando..."

        lifecycleScope.launch {
            try {
                val response = arrendamientoRepository.registrarArrendamiento(
                    propiedadId = propiedadId,
                    inquilinoId = inquilinoId,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin
                )

                if (response.isSuccessful) {
                    val resultado = response.body()
                    Toast.makeText(
                        this@PaymentActivity,
                        resultado?.mensaje ?: "Arrendamiento registrado exitosamente",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navegar a pantalla principal o de confirmación
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        this@PaymentActivity,
                        "Error al procesar: ${errorBody ?: "Error desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnProcesarPago.isEnabled = true
                    binding.btnProcesarPago.text = "Procesar Pago"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PaymentActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnProcesarPago.isEnabled = true
                binding.btnProcesarPago.text = "Procesar Pago"
            }
        }
    }

    /*
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
    */
}