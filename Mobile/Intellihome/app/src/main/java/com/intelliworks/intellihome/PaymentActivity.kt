package com.intelliworks.intellihome

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.ArrendamientoRepository
import com.intelliworks.intellihome.data.repository.UsuarioRepository
import com.intelliworks.intellihome.databinding.ActivityPaymentBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.RegisterHelper
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

// Clase auxiliar para parsear el JSON de fechas
data class ArrendamientoFechaSimple(val fecha_inicio: String, val fecha_fin: String)

class PaymentActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var usarTarjetaGuardada = true
    private lateinit var arrendamientoRepository: ArrendamientoRepository
    private lateinit var usuarioRepository: UsuarioRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var fechaInicioSeleccionada: Long? = null
    private var fechaFinSeleccionada: Long? = null
    private var precioPorNoche: Double = 0.0
    private var propiedadId: Int = 0

    // Lista donde guardaremos los rangos ocupados en milisegundos UTC
    private val blockedRanges = mutableListOf<Pair<Long, Long>>()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val backendDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        arrendamientoRepository = ArrendamientoRepository()
        usuarioRepository = UsuarioRepository(RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.UsuarioApi::class.java))
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        precioPorNoche = intent.getDoubleExtra("PROPERTY_PRICE", 0.0)
        propiedadId = intent.getIntExtra("PROPERTY_ID", 0)

        // 1. PROCESAR FECHAS ANTES DE MOSTRAR NADA
        procesarFechasBloqueadas()

        configurarCheckbox()
        configurarBotonPagar()
        configurarSelectorFecha()
        configurarSelectorFechasAlquiler()
        cargarDatosTarjetaGuardada()
    }

    private fun procesarFechasBloqueadas() {
        val jsonFechas = intent.getStringExtra("BLOCKED_DATES")
        android.util.Log.d("DEBUG_FECHAS", "PaymentActivity - JSON Recibido: $jsonFechas")

        if (!jsonFechas.isNullOrEmpty()) {
            try {
                val gson = Gson()
                val tipoLista = object : TypeToken<List<ArrendamientoFechaSimple>>() {}.type
                val listaFechas: List<ArrendamientoFechaSimple> = gson.fromJson(jsonFechas, tipoLista)

                blockedRanges.clear()
                for (fecha in listaFechas) {
                    val inicio = backendDateFormat.parse(fecha.fecha_inicio)?.time
                    val fin = backendDateFormat.parse(fecha.fecha_fin)?.time

                    if (inicio != null && fin != null) {
                        blockedRanges.add(Pair(inicio, fin))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        val validatorForward = DateValidatorPointForward.now()
        val validatorBlocked = DateValidatorBlockRanges(blockedRanges)
        val compositeValidator = CompositeDateValidator.allOf(listOf(validatorForward, validatorBlocked))

        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(compositeValidator)

        // Nota: Asegúrate de tener 'selectDates' en tu strings.xml también
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.selectDates))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            if (startDate == endDate) {
                Toast.makeText(this, getString(R.string.payment_min_one_night), Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }

            if (!validarRangoLibre(startDate, endDate)) {
                Toast.makeText(this, getString(R.string.payment_dates_overlap), Toast.LENGTH_LONG).show()
                return@addOnPositiveButtonClickListener
            }

            fechaInicioSeleccionada = startDate
            fechaFinSeleccionada = endDate
            mostrarFechasSeleccionadas()
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun validarRangoLibre(inicioUsuario: Long, finUsuario: Long): Boolean {
        for (bloqueo in blockedRanges) {
            val inicioOcupado = bloqueo.first
            val finOcupado = bloqueo.second
            if (inicioUsuario <= finOcupado && finUsuario >= inicioOcupado) {
                return false
            }
        }
        return true
    }

    private fun mostrarFechasSeleccionadas() {
        if (fechaInicioSeleccionada != null && fechaFinSeleccionada != null) {
            val fechaInicio = dateFormat.format(Date(fechaInicioSeleccionada!!))
            val fechaFin = dateFormat.format(Date(fechaFinSeleccionada!!))
            binding.tvFechasSeleccionadas.text = getString(R.string.payment_dates_selected, fechaInicio, fechaFin)
            binding.tvFechasSeleccionadas.visibility = View.VISIBLE

            val noches = calcularNoches(fechaInicioSeleccionada!!, fechaFinSeleccionada!!)
            binding.tvTotalNoches.text = getString(R.string.payment_total_nights, noches)
            binding.tvTotalNoches.visibility = View.VISIBLE

            val totalPagar = precioPorNoche * noches
            val symbols = DecimalFormatSymbols(Locale.US)
            symbols.groupingSeparator = ' '
            symbols.decimalSeparator = ','
            val formatter = DecimalFormat("#,###.00", symbols)

            // CORREGIDO: Uso de String Resource con formato (%1$s)
            binding.tvTotalPagar.text = getString(R.string.payment_total_pay_fmt, formatter.format(totalPagar))
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
                binding.contenedorTajetaGuardada.visibility = View.VISIBLE
                binding.contenedorTarjetaNueva.visibility = View.GONE
            } else {
                binding.contenedorTajetaGuardada.visibility = View.GONE
                binding.contenedorTarjetaNueva.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarSelectorFecha() {
        binding.etFechaNuevaTarjeta.setOnClickListener {
            RegisterHelper.mostrarSelectorMesAnioTarjeta(this) { mesStr, anioStr ->
                binding.etFechaNuevaTarjeta.setText("$mesStr/$anioStr")
            }
        }
    }

    private fun cargarDatosTarjetaGuardada() {
        val userIdStr = SessionManager.obtenerUserId(this)
        val userId = userIdStr.toIntOrNull() ?: 0

        if (userId == 0) {
            binding.cbConfirmarTarjeta.isChecked = false
            binding.contenedorTajetaGuardada.visibility = View.GONE
            binding.contenedorTarjetaNueva.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val response = usuarioRepository.obtenerUltimos4Tarjeta(userId)
                if (response.isSuccessful) {
                    val ultimos4 = response.body()?.ultimos_4
                    if (!ultimos4.isNullOrEmpty()) {
                        binding.cbConfirmarTarjeta.text = "${getString(R.string.payment_saved_card_label)} **** $ultimos4"
                        binding.cbConfirmarTarjeta.isChecked = true
                        binding.contenedorTajetaGuardada.visibility = View.VISIBLE
                        binding.contenedorTarjetaNueva.visibility = View.GONE
                        usarTarjetaGuardada = true
                    } else {
                        configurarSinTarjetaGuardada()
                    }
                } else {
                    configurarSinTarjetaGuardada()
                }
            } catch (e: Exception) {
                configurarSinTarjetaGuardada()
            }
        }
    }

    private fun configurarSinTarjetaGuardada() {
        binding.cbConfirmarTarjeta.isChecked = false
        binding.cbConfirmarTarjeta.visibility = View.GONE
        binding.contenedorTajetaGuardada.visibility = View.GONE
        binding.contenedorTarjetaNueva.visibility = View.VISIBLE
        usarTarjetaGuardada = false
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
        // Validaciones básicas de campos...
        if (usarTarjetaGuardada) {
            if (binding.etCvv.text.isNullOrEmpty()) {
                // CORREGIDO: String Resource
                binding.contenedorCvv.error = getString(R.string.payment_cvv_req)
                return false
            }
        } else {
            if (binding.etNumeroNuevaTarjeta.text.isNullOrEmpty()) {
                // CORREGIDO: String Resource
                binding.contenedorNumeroNuevaTarjeta.error = getString(R.string.payment_card_num_req)
                return false
            }
            // ... más validaciones si gustas
        }
        return true
    }

    private fun procesarPago() {
        val idString = SessionManager.obtenerUserId(this)
        val inquilinoId = idString.toIntOrNull() ?: 0

        if (inquilinoId == 0 || propiedadId == 0) {
            // CORREGIDO: String Resource
            Toast.makeText(this, getString(R.string.payment_session_error), Toast.LENGTH_SHORT).show()
            return
        }

        val fechaInicio = backendDateFormat.format(Date(fechaInicioSeleccionada!!))
        val fechaFin = backendDateFormat.format(Date(fechaFinSeleccionada!!))

        binding.btnProcesarPago.isEnabled = false
        // CORREGIDO: String Resource
        binding.btnProcesarPago.text = getString(R.string.payment_processing_btn)

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
                    // Usamos el mensaje del backend si existe, sino el default del recurso
                    val msg = resultado?.mensaje ?: getString(R.string.payment_success_msg)
                    Toast.makeText(this@PaymentActivity, msg, Toast.LENGTH_LONG).show()

                    // --- REDIRECCIÓN A MIS RENTAS (MainActivity) ---
                    val intent = Intent(this@PaymentActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra("NAVIGATE_TO", "RENTALS")
                    startActivity(intent)
                    finish()
                    // -----------------------------------------------

                } else {
                    val errorBody = response.errorBody()?.string()
                    // CORREGIDO: String Resource con formato
                    Toast.makeText(this@PaymentActivity, getString(R.string.payment_generic_error_fmt, errorBody), Toast.LENGTH_LONG).show()
                    binding.btnProcesarPago.isEnabled = true
                    // CORREGIDO: String Resource
                    binding.btnProcesarPago.text = getString(R.string.payment_process_btn_default)
                }
            } catch (e: Exception) {
                // CORREGIDO: String Resource con formato
                Toast.makeText(this@PaymentActivity, getString(R.string.payment_connection_error_fmt, e.message), Toast.LENGTH_LONG).show()
                binding.btnProcesarPago.isEnabled = true
                binding.btnProcesarPago.text = getString(R.string.payment_process_btn_default)
            }
        }
    }
}

class DateValidatorBlockRanges(private val blockedRanges: List<Pair<Long, Long>>) : CalendarConstraints.DateValidator {
    constructor(parcel: Parcel) : this(
        mutableListOf<Pair<Long, Long>>().apply {
            val size = parcel.readInt()
            repeat(size) {
                add(Pair(parcel.readLong(), parcel.readLong()))
            }
        }
    )

    override fun isValid(date: Long): Boolean {
        for (range in blockedRanges) {
            if (date >= range.first && date <= range.second) return false
        }
        return true
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(blockedRanges.size)
        for (range in blockedRanges) {
            dest.writeLong(range.first)
            dest.writeLong(range.second)
        }
    }

    companion object CREATOR : Parcelable.Creator<DateValidatorBlockRanges> {
        override fun createFromParcel(parcel: Parcel): DateValidatorBlockRanges = DateValidatorBlockRanges(parcel)
        override fun newArray(size: Int): Array<DateValidatorBlockRanges?> = arrayOfNulls(size)
    }
}