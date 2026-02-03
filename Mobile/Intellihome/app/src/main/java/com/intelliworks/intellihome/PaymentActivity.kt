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
import com.intelliworks.intellihome.utils.RegisterHelper
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase auxiliar para parsear el objeto JSON de fechas bloqueadas que viene del backend.
 */
data class ArrendamientoFechaSimple(val fecha_inicio: String, val fecha_fin: String)

/**
 * Activity encargada de procesar el pago y la reserva de una propiedad.
 * Gestiona la selección de fechas (con validaciones de disponibilidad),
 * la visualización de la cotización y la confirmación del arrendamiento.
 */
class PaymentActivity : BaseActivity() {

    // --- Variables de Datos ---
    private var cotizacionActual: com.intelliworks.intellihome.data.model.CotizacionArrendamientoDto? = null
    private var propiedadId: Int = 0
    private var fechaInicioSeleccionada: Long? = null
    private var fechaFinSeleccionada: Long? = null

    // Lista de rangos de fechas (en ms UTC) que ya están ocupados por otros inquilinos
    private val blockedRanges = mutableListOf<Pair<Long, Long>>()

    // Lista de días de la semana permitidos por el dueño (ej: ["Lunes", "Viernes"])
    private var allowedDays: List<String> = emptyList()

    // --- Componentes de UI y Utilidades ---
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var arrendamientoRepository: ArrendamientoRepository
    private lateinit var usuarioRepository: UsuarioRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var usarTarjetaGuardada = true

    // Formateadores de fecha configurados en UTC para evitar desfases de zona horaria
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

        // Inicialización de repositorios y sesión
        arrendamientoRepository = ArrendamientoRepository()
        usuarioRepository = UsuarioRepository(RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.UsuarioApi::class.java))
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        propiedadId = intent.getIntExtra("PROPERTY_ID", 0)

        // 1. Recuperar y procesar datos de bloqueo del Intent
        procesarFechasBloqueadas()
        // Recuperamos la lista de días permitidos enviada desde RentPropertyActivity
        allowedDays = intent.getStringArrayListExtra("ALLOWED_DAYS") ?: emptyList()

        // 2. Configuración de Listeners y UI
        configurarCheckbox()
        configurarBotonPagar()
        configurarSelectorFecha()
        configurarSelectorFechasAlquiler()

        // 3. Cargar información del usuario
        cargarDatosTarjetaGuardada()
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    /**
     * Parsea el JSON de 'BLOCKED_DATES' recibido en el intent y llena la lista [blockedRanges].
     * Convierte las fechas String (yyyy-MM-dd) a milisegundos (Long).
     */
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

    private fun configurarSelectorFechasAlquiler() {
        binding.btnSeleccionarFechas.setOnClickListener {
            mostrarSelectorRangoFechas()
        }
    }

    /**
     * Muestra el MaterialDatePicker con restricciones personalizadas.
     * Combina validadores para:
     * 1. Fechas futuras (DateValidatorPointForward).
     * 2. Disponibilidad específica (ValidadorDisponibilidad: bloquea rangos y días de semana).
     */
    private fun mostrarSelectorRangoFechas() {
        // Validador 1: Solo fechas a partir de hoy
        val validatorForward = DateValidatorPointForward.now()

        // Validador 2: Lógica personalizada (Rangos ocupados + Días de semana permitidos)
        val validatorAvailability = ValidadorDisponibilidad(blockedRanges, allowedDays)

        // Combinar ambos validadores
        val compositeValidator = CompositeDateValidator.allOf(listOf(validatorForward, validatorAvailability))

        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(compositeValidator)

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.selectDates))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            // Validación: Mínimo 1 noche
            if (startDate == endDate) {
                Toast.makeText(this, getString(R.string.payment_min_one_night), Toast.LENGTH_SHORT).show()
                return@addOnPositiveButtonClickListener
            }

            // Validación: Rango libre de reservas previas
            if (!validarRangoLibre(startDate, endDate)) {
                Toast.makeText(this, getString(R.string.payment_dates_overlap), Toast.LENGTH_LONG).show()
                return@addOnPositiveButtonClickListener
            }

            // Validación: Verificar si el rango incluye algún día de la semana no permitido
            if (!validarDiasEnRango(startDate, endDate)) {
                Toast.makeText(this, "El rango incluye días en los que la propiedad no está disponible.", Toast.LENGTH_LONG).show()
                return@addOnPositiveButtonClickListener
            }

            // Selección válida: Guardar y mostrar
            fechaInicioSeleccionada = startDate
            fechaFinSeleccionada = endDate
            mostrarFechasSeleccionadas()

            // Solicitar cotización al backend
            val fechaInicioStr = backendDateFormat.format(Date(startDate))
            val fechaFinStr = backendDateFormat.format(Date(endDate))

            lifecycleScope.launch {
                try {
                    val response = arrendamientoRepository.cotizarArrendamiento(
                        propiedadId = propiedadId,
                        fechaInicio = fechaInicioStr,
                        fechaFin = fechaFinStr
                    )
                    if (response.isSuccessful) {
                        val cotizacion = response.body()
                        if (cotizacion != null) {
                            mostrarCotizacionBackend(cotizacion)
                        }
                    } else {
                        Toast.makeText(this@PaymentActivity, getString(R.string.payment_generic_error_fmt, response.errorBody()?.string()), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PaymentActivity, getString(R.string.payment_connection_error_fmt, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    /**
     * Verifica si el rango seleccionado se solapa con algún rango bloqueado (arrendamientos existentes).
     */
    private fun validarRangoLibre(inicioUsuario: Long, finUsuario: Long): Boolean {
        for (bloqueo in blockedRanges) {
            val inicioOcupado = bloqueo.first
            val finOcupado = bloqueo.second
            // Si hay intersección entre los rangos, no es válido
            if (inicioUsuario <= finOcupado && finUsuario >= inicioOcupado) {
                return false
            }
        }
        return true
    }

    /**
     * Recorre día por día el rango seleccionado para asegurar que TODOS los días
     * sean permitidos por el dueño (según allowedDays).
     */
    private fun validarDiasEnRango(inicio: Long, fin: Long): Boolean {
        if (allowedDays.isEmpty()) return true // Si no hay restricciones, todo es válido

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = inicio

        // Reutilizamos la lógica del validador para verificar día a día
        val validador = ValidadorDisponibilidad(blockedRanges, allowedDays)

        // Iteramos hasta llegar a la fecha fin (exclusive o inclusive según lógica de negocio, aquí inclusive)
        // Nota: En alquileres, normalmente el día de salida (checkout) no cuenta como ocupación nocturna,
        // pero aquí validamos la estadía completa.
        var currentDate = inicio
        while (currentDate < fin) { // < fin porque el último día es checkout
            if (!validador.isValid(currentDate)) {
                return false
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            currentDate = calendar.timeInMillis
        }
        return true
    }

    private fun mostrarFechasSeleccionadas() {
        if (fechaInicioSeleccionada != null && fechaFinSeleccionada != null) {
            val fechaInicio = dateFormat.format(Date(fechaInicioSeleccionada!!))
            val fechaFin = dateFormat.format(Date(fechaFinSeleccionada!!))
            binding.tvFechasSeleccionadas.text = getString(R.string.payment_dates_selected, fechaInicio, fechaFin)
            binding.tvFechasSeleccionadas.visibility = View.VISIBLE

            // Ocultar cotización anterior mientras carga la nueva
            binding.tvBackendSubtotal.visibility = View.GONE
            binding.tvBackendIva.visibility = View.GONE
            binding.tvBackendComision.visibility = View.GONE
            binding.tvBackendPrecioNoche.visibility = View.GONE
            binding.tvBackendNoches.visibility = View.GONE
            binding.tvTotalPagar.visibility = View.GONE
        }
    }

    /**
     * Muestra los datos financieros recibidos del backend en la UI.
     */
    private fun mostrarCotizacionBackend(cotizacion: com.intelliworks.intellihome.data.model.CotizacionArrendamientoDto) {
        cotizacionActual = cotizacion
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###.00", symbols)

        binding.tvBackendSubtotal.text = getString(R.string.payment_subtotal_fmt, formatter.format(cotizacion.subtotal))
        binding.tvBackendIva.text = getString(R.string.payment_iva_fmt, formatter.format(cotizacion.iva))
        binding.tvBackendComision.text = getString(R.string.payment_commission_fmt, formatter.format(cotizacion.comision))
        binding.tvBackendPrecioNoche.text = getString(R.string.payment_price_per_night_fmt, formatter.format(cotizacion.precio_noche))
        binding.tvBackendNoches.text = getString(R.string.payment_nights_fmt, cotizacion.noches)

        // Calcular total final
        val totalCalculado = cotizacion.subtotal + cotizacion.iva + cotizacion.comision
        binding.tvTotalPagar.text = getString(R.string.payment_total_pay_fmt, formatter.format(totalCalculado))

        binding.tvBackendSubtotal.visibility = View.VISIBLE
        binding.tvBackendIva.visibility = View.VISIBLE
        binding.tvBackendComision.visibility = View.VISIBLE
        binding.tvBackendPrecioNoche.visibility = View.VISIBLE
        binding.tvBackendNoches.visibility = View.VISIBLE
        binding.tvTotalPagar.visibility = View.VISIBLE
    }

    // --- Configuración de Formulario ---

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
            configurarSinTarjetaGuardada()
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
        if (usarTarjetaGuardada) {
            if (binding.etCvv.text.isNullOrEmpty()) {
                binding.contenedorCvv.error = getString(R.string.payment_cvv_req)
                return false
            }
        } else {
            if (binding.etNumeroNuevaTarjeta.text.isNullOrEmpty()) {
                binding.contenedorNumeroNuevaTarjeta.error = getString(R.string.payment_card_num_req)
                return false
            }
        }
        return true
    }

    // --- Procesamiento Final ---

    private fun procesarPago() {
        val idString = SessionManager.obtenerUserId(this)
        val inquilinoId = idString.toIntOrNull() ?: 0

        if (inquilinoId == 0 || propiedadId == 0) {
            Toast.makeText(this, getString(R.string.payment_session_error), Toast.LENGTH_SHORT).show()
            return
        }

        val cotizacion = cotizacionActual
        if (cotizacion == null) {
            Toast.makeText(this, getString(R.string.payment_error_no_dates), Toast.LENGTH_SHORT).show()
            return
        }

        val fechaInicio = backendDateFormat.format(Date(fechaInicioSeleccionada!!))
        val fechaFin = backendDateFormat.format(Date(fechaFinSeleccionada!!))

        binding.btnProcesarPago.isEnabled = false
        binding.btnProcesarPago.text = getString(R.string.payment_processing_btn)

        lifecycleScope.launch {
            try {
                val response = arrendamientoRepository.registrarArrendamiento(
                    propiedadId = propiedadId,
                    inquilinoId = inquilinoId,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    subtotal = cotizacion.subtotal,
                    iva = cotizacion.iva,
                    comision = cotizacion.comision
                )

                if (response.isSuccessful) {
                    val resultado = response.body()
                    val msg = resultado?.mensaje ?: getString(R.string.payment_success_msg)
                    Toast.makeText(this@PaymentActivity, msg, Toast.LENGTH_LONG).show()

                    // Redirección exitosa
                    val intent = Intent(this@PaymentActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra("NAVIGATE_TO", "RENTALS")
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@PaymentActivity, getString(R.string.payment_generic_error_fmt, errorBody), Toast.LENGTH_LONG).show()
                    binding.btnProcesarPago.isEnabled = true
                    binding.btnProcesarPago.text = getString(R.string.payment_process_btn_default)
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentActivity, getString(R.string.payment_connection_error_fmt, e.message), Toast.LENGTH_LONG).show()
                binding.btnProcesarPago.isEnabled = true
                binding.btnProcesarPago.text = getString(R.string.payment_process_btn_default)
            }
        }
    }
}

// ================================================================
// CLASE VALIDADORA PERSONALIZADA PARA MATERIAL DATE PICKER
// ================================================================

/**
 * Validador que implementa `CalendarConstraints.DateValidator`.
 * Su función es deshabilitar días en el calendario basándose en dos criterios:
 * 1. Rangos de fechas ya ocupadas (reservas existentes).
 * 2. Días de la semana no permitidos por el dueño (ej. fines de semana).
 */
class ValidadorDisponibilidad(
    private val blockedRanges: List<Pair<Long, Long>>,
    private val allowedDays: List<String>
) : CalendarConstraints.DateValidator {

    // Mapa para normalizar los nombres de días y obtener su entero de Calendar
    private val mapaDias = mapOf(
        "Lunes" to Calendar.MONDAY,
        "Martes" to Calendar.TUESDAY,
        "Miércoles" to Calendar.WEDNESDAY,
        "Miercoles" to Calendar.WEDNESDAY,
        "Jueves" to Calendar.THURSDAY,
        "Viernes" to Calendar.FRIDAY,
        "Sábado" to Calendar.SATURDAY,
        "Sabado" to Calendar.SATURDAY,
        "Domingo" to Calendar.SUNDAY
    )

    constructor(parcel: Parcel) : this(
        mutableListOf<Pair<Long, Long>>().apply {
            val size = parcel.readInt()
            repeat(size) {
                add(Pair(parcel.readLong(), parcel.readLong()))
            }
        },
        parcel.createStringArrayList() ?: emptyList()
    )

    /**
     * Método principal de validación. Retorna true si el día es seleccionable.
     */
    override fun isValid(date: Long): Boolean {
        // 1. VALIDAR RANGOS OCUPADOS
        // Si la fecha cae dentro de un rango bloqueado, es inválida.
        for (range in blockedRanges) {
            if (date >= range.first && date <= range.second) return false
        }

        // 2. VALIDAR DÍA DE LA SEMANA
        // Si hay restricciones de días (allowedDays no está vacío), verificamos.
        if (allowedDays.isNotEmpty()) {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = date
            val diaSemanaActual = calendar.get(Calendar.DAY_OF_WEEK)

            var esDiaPermitido = false
            for (nombreDia in allowedDays) {
                // Limpiamos el nombre para evitar problemas de espacios
                val key = nombreDia.trim()
                val diaPermitidoInt = mapaDias[key]

                if (diaPermitidoInt == diaSemanaActual) {
                    esDiaPermitido = true
                    break
                }
            }
            // Si el día actual no coincidió con ninguno de los permitidos, es inválido.
            if (!esDiaPermitido) return false
        }

        // Si pasó todas las pruebas, el día está disponible.
        return true
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        // Serializar rangos
        dest.writeInt(blockedRanges.size)
        for (range in blockedRanges) {
            dest.writeLong(range.first)
            dest.writeLong(range.second)
        }
        // Serializar lista de días
        dest.writeStringList(allowedDays)
    }

    companion object CREATOR : Parcelable.Creator<ValidadorDisponibilidad> {
        override fun createFromParcel(parcel: Parcel) = ValidadorDisponibilidad(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ValidadorDisponibilidad?>(size)
    }
}