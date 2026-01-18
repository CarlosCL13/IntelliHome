package com.intelliworks.intellihome

import android.app.DatePickerDialog
import androidx.biometric.BiometricManager
import android.content.Intent
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

import java.text.SimpleDateFormat
import java.util.Calendar

import com.intelliworks.intellihome.databinding.ActivityRegisterBinding

/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        var passwordVisible = false
        var confirmarPasswordVisible = false
        // Mostrar/ocultar contraseña
        binding.btnMostrarPassword.setOnClickListener {
            passwordVisible = !passwordVisible

            // Si la contraseña es visible, mostrar el texto, sino ocultarlo
            if (passwordVisible) {
                // Cambiar input type para mostrar contraseña
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                // Cambiar icono del botón
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_open_eye)
            } else {
                // Cambiar input type para ocultar contraseña
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                // Cambiar icono del botón
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_close_eye)
            }
            // Mantener el cursor al final
            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }
        // Mostrar/ocultar confirmar contraseña
        binding.btnMostrarPassword2.setOnClickListener {
            confirmarPasswordVisible = !confirmarPasswordVisible

            // Si la contraseña es visible, mostrar el texto, sino ocultarlo
            if (confirmarPasswordVisible) {
                // Cambiar input type para mostrar contraseña
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                // Cambiar icono del botón
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_open_eye)
            } else {
                // Cambiar input type para ocultar contraseña
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                // Cambiar icono del botón
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_close_eye)
            }
            binding.etConfirmarContrasena.setSelection(binding.etConfirmarContrasena.text?.length ?: 0)
        }


        // Base de datos de prueba
        databaseHelper = DatabaseHelper(this)


        // Asignar fecha de nacimiento
        val calendario = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd")
        val asignarFecha = DatePickerDialog.OnDateSetListener {_, anio, mes, dia ->
            calendario.set(Calendar.YEAR, anio)
            calendario.set(Calendar.MONTH, mes)
            calendario.set(Calendar.DAY_OF_MONTH, dia)
            binding.etFechaNacimiento.setText(formatoFecha.format(calendario.time))
        }
        // Mostrar calendario al tocar el campo de fecha de nacimiento
        binding.etFechaNacimiento.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                DatePickerDialog(
                    this@Register,
                    asignarFecha,
                    calendario.get(Calendar.YEAR),
                    calendario.get(Calendar.MONTH),
                    calendario.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        // Mostrar calendario con el boton en fecha de nacimiento
        binding.btnCalendario.setOnClickListener {
            DatePickerDialog(
                this@Register,
                asignarFecha,
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        // Mostrar selector al tocar el campo de fecha de vencimiento de la tarjeta
        binding.etFechaVencimiento.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                selectorMesAnio { mes, anio ->
                    binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
                }
            }
        }
        // Mostrar selector con el botón en fecha de vencimiento de la tarjeta
        binding.btnCalendario2.setOnClickListener {
            selectorMesAnio { mes, anio ->
                binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
            }
        }


        // Obtener preguntas de recuperación
        val preguntas = resources.getStringArray(R.array.preguntas_recuperacion)
        val adapterPreguntas = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, preguntas)
        // Configurar Spinner de pregunta de recuperación
        binding.spPregunta.adapter = adapterPreguntas


        // Registrar usuario se guarda en la base de datos y se redirige al inicio de sesión
        binding.btnRegistrar.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etContrasena.text.toString()
            val confirmPassword = binding.etConfirmarContrasena.text.toString()
            val nombre = binding.etNombre.text.toString()
            val apellidos = binding.etApellidos.text.toString()
            val correo = binding.etCorreo.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val fechaNacimiento = binding.etFechaNacimiento.text.toString()
            val domicilio = binding.etDomicilio.text.toString()
            val preguntaRecuperacionId = binding.spPregunta.selectedItemPosition + 1 // ID 1-based
            val respuestaRecuperacion = binding.etRespuesta.text.toString()
            val fingerprintEnabled = binding.cbHuellaDigital.isChecked
            val intentosFallidos = 0
            val estadoCuenta = "activo"
            val nombreTitular = binding.etTitularTarjeta.text.toString()
            val numeroEncriptado = binding.etNumeroTarjeta.text.toString()
            val fechaExpiracion = binding.etFechaVencimiento.text.toString()
            val marca = "Mastercard"
            val ultimos4 = if (numeroEncriptado.length >= 4) numeroEncriptado.takeLast(4) else "0000"
            val imagenPerfil = ""
            val rolId = 2

            // Se valida que los campos del formulario no esten vacios
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || telefono.isEmpty() || fechaNacimiento.isEmpty() || domicilio.isEmpty() || respuestaRecuperacion.isEmpty() || nombreTitular.isEmpty() || numeroEncriptado.isEmpty() || fechaExpiracion.isEmpty()) {
                Toast.makeText(this, "Favor llenar todos los campos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Compara contraseña y confirmar contraseña
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Se valida si se marcó la casilla de huella digital, sino se registra el usuario sin huella
            if (binding.cbHuellaDigital.isChecked) {
                if (canUseBiometric()) {
                    showBiometricPrompt {
                        registerUser(rolId, imagenPerfil, nombre, apellidos, correo, username, password, telefono, fechaNacimiento, domicilio, preguntaRecuperacionId, respuestaRecuperacion, fingerprintEnabled, intentosFallidos, estadoCuenta, nombreTitular, numeroEncriptado, fechaExpiracion, marca, ultimos4)
                    }
                } else {
                    Toast.makeText(this, "Huella digital no disponible en este dispositivo", Toast.LENGTH_LONG).show()
                    registerUser(rolId, imagenPerfil, nombre, apellidos, correo, username, password, telefono, fechaNacimiento, domicilio, preguntaRecuperacionId, respuestaRecuperacion, false, intentosFallidos, estadoCuenta, nombreTitular, numeroEncriptado, fechaExpiracion, marca, ultimos4)
                }
            } else {
                registerUser(rolId, imagenPerfil, nombre, apellidos, correo, username, password, telefono, fechaNacimiento, domicilio, preguntaRecuperacionId, respuestaRecuperacion, false, intentosFallidos, estadoCuenta, nombreTitular, numeroEncriptado, fechaExpiracion, marca, ultimos4)
            }
        }


        // Redirigir al inicio de sesión al tocar el texto correspondiente
        binding.btnLoginRedirigir.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

    }


    // Selector de mes/año para fecha de vencimiento de la tarjeta
    public
    fun selectorMesAnio(onDateSelected: (month: Int, year: Int) -> Unit) {
        val diagoloEmergente = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val selectorMes = diagoloEmergente.findViewById<NumberPicker>(R.id.monthPicker)
        val selectorAnio = diagoloEmergente.findViewById<NumberPicker>(R.id.yearPicker)

        // Mínimo y máximo para el selector de mes
        selectorMes.minValue = 1
        selectorMes.maxValue = 12
        selectorMes.value = Calendar.getInstance().get(Calendar.MONTH) + 1

        // Mínimo y máximo para el selector de año
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        selectorAnio.minValue = anioActual
        selectorAnio.maxValue = anioActual + 20
        selectorAnio.value = anioActual

        // Configuración del diálogo
        AlertDialog.Builder(this)
            .setTitle("Seleccione mes y año")
            .setView(diagoloEmergente)
            .setPositiveButton("Aceptar") { _, _ ->
                val mes = selectorMes.value
                val anio = selectorAnio.value
                onDateSelected(mes, anio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Función para validar si el dispositivo puede usar huella digital
    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Función para mostrar el prompt de huella digital
    private fun showBiometricPrompt(onSuccess: () -> Unit) {

        // Ejecutor, esta variable nos permite ejecutar el prompt en el hilo principal de la aplicación
        val executor = ContextCompat.getMainExecutor(this)

        // Configuración del prompt
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@Register, errString, Toast.LENGTH_LONG).show()
                }
            })

        // Texto a mostrar en el prompt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar huella digital")
            .setSubtitle("Habilitar el inicio de sesión con huella digital")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Función para registrar el usuario en la base de datos
    private fun registerUser(rolId: Int, imagenPerfil: String, nombre: String, apellidos: String, correo: String, username: String, password: String, telefono: String, fechaNacimiento: String, domicilio: String, preguntaRecuperacionId: Int, respuestaRecuperacion: String, fingerprintEnabled: Boolean, intentosFallidos: Int, estadoCuenta: String, nombreTitular: String, numeroEncriptado: String, fechaExpiracion: String, marca: String, ultimos4: String) {

        // Se llama a la función insertUser del DatabaseHelper para insertar el usuario en la base de datos
        val result = databaseHelper.insertUser(rolId, imagenPerfil, nombre, apellidos, correo, username, password, telefono, fechaNacimiento, domicilio, preguntaRecuperacionId, respuestaRecuperacion, fingerprintEnabled, intentosFallidos, estadoCuenta, nombreTitular, numeroEncriptado, fechaExpiracion, marca, ultimos4)

        // Si el registro fue exitoso, se redirige al inicio de sesión, sino se muestra un mensaje de error
        if (result != -1L) {
            // Muestra mensaje de éxito
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()
            // Redirige al inicio de sesión
            startActivity(Intent(this, Login::class.java))
            finish()
        } else {
            // Muestra mensaje de error
            Toast.makeText(this, "El registro falló", Toast.LENGTH_LONG).show()
        }
    }

}