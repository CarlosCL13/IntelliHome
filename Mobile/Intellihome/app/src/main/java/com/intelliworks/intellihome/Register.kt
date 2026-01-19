package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import androidx.biometric.BiometricManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.intelliworks.intellihome.databinding.ActivityRegisterBinding
import android.app.DatePickerDialog
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

import java.text.SimpleDateFormat
import java.util.Calendar
import android.util.Patterns


/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var databaseHelper: DatabaseHelper

    // Variable para almacenar la URI de la imagen de usuario
    private var imagenUsuarioUri: android.net.Uri? = null

    companion object {
        // Constante para el código de solicitud de selección de imagen
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    // Crea un archivo temporal a partir de una URI
    private fun crearArchivoTemp(uri: android.net.Uri): java.io.File? {
        return try {

            // Obtiene el nombre del archivo original (si existe)
            var nombreArchivo = "imagen_temp.jpg"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    nombreArchivo = it.getString(nameIndex)
                }
            }

            // Crea el archivo temporal en el directorio de la caché
            val archivoTemp = java.io.File.createTempFile(
                nombreArchivo.substringBeforeLast('.'),
                "." + nombreArchivo.substringAfterLast('.', "jpg"),
                cacheDir
            )
            archivoTemp.deleteOnExit()

            // Copia el contenido de la URI al archivo temporal
            contentResolver.openInputStream(uri)?.use { inputStream ->
                archivoTemp.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            archivoTemp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAppAppearance(binding.root)

        // Inicializar CheckBox de términos deshabilitado
        binding.cbTerminos.isEnabled = false
        binding.cbTerminos.isChecked = false

        // Mostrar AlertDialog de términos y condiciones al tocar el texto
        binding.tvVerTerminos.setOnClickListener {
            mostrarDialogoTerminos()
        }

        // Base de datos de prueba
        databaseHelper = DatabaseHelper(this)

        // Selección de imagen de usuario
        binding.imgUsuario.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg", "image/gif"))
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        // Mostrar/ocultar contraseñas
        var passwordVisible = false
        var confirmarPasswordVisible = false
        binding.btnMostrarPassword.setOnClickListener {
            passwordVisible = !passwordVisible

            // Guardamos el typeface actual antes del cambio (opcional pero seguro)
            val tf = binding.etContrasena.typeface

            if (passwordVisible) {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_close_eye)
            }

            // RESTAURAR FUENTE: Esto evita que el texto cambie de tamaño o estilo
            binding.etContrasena.typeface = tf

            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }

        binding.btnMostrarPassword2.setOnClickListener {
            confirmarPasswordVisible = !confirmarPasswordVisible

            val tf = binding.etConfirmarContrasena.typeface

            if (confirmarPasswordVisible) {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_close_eye)
            }

            // RESTAURAR FUENTE
            binding.etConfirmarContrasena.typeface = tf

            binding.etConfirmarContrasena.setSelection(binding.etConfirmarContrasena.text?.length ?: 0)
        }
        // CONFIGURACIÓN DEL SPINNER
        val adapter = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.preguntas_recuperacion, // El nombre que pusiste en strings.xml
            android.R.layout.simple_spinner_item
        )
        // Cómo se ve cuando se despliega
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Aplicarlo al Spinner
        binding.spPregunta.adapter = adapter

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
            val imagenPerfil = imagenUsuarioUri?.toString() ?: ""
            val rolId = 2


            // Se valida si el usuario aceptó los términos y condiciones
            if (!binding.cbTerminos.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Se valida el formato de correo electrónico
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Se valida que los campos del formulario no esten vacios
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || telefono.isEmpty() || fechaNacimiento.isEmpty() || domicilio.isEmpty() || respuestaRecuperacion.isEmpty() || nombreTitular.isEmpty() || numeroEncriptado.isEmpty() || fechaExpiracion.isEmpty()) {
                Toast.makeText(this, "Favor llenar todos los campos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Se valida que la contraseña y confirmar contraseña sean iguales
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
    // Función para mostrar el diálogo de términos y condiciones
    private fun mostrarDialogoTerminos() {
        val builder = AlertDialog.Builder(this)

        // Cargar el texto de términos desde res/raw/terms.txt
        val inputStream = resources.openRawResource(R.raw.terms)
        val terminos = inputStream.bufferedReader().use { it.readText() }

        // Crear ScrollView y TextView para los términos
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this)
        textView.text = terminos
        textView.setPadding(32, 32, 32, 32)
        textView.textSize = 16f
        scrollView.addView(textView)

        // Botón aceptar (se habilita al llegar al final)
        builder.setView(scrollView)
        builder.setNegativeButton("Cancelar", null)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            binding.cbTerminos.isEnabled = true
            binding.cbTerminos.isChecked = true
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        // Deshabilita el botón aceptar hasta que se llegue al final del ScrollView
        val btnAceptar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnAceptar.isEnabled = false
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(0)
            if (view != null) {
                val diff = view.bottom - (scrollView.height + scrollView.scrollY)
                btnAceptar.isEnabled = diff <= 0
            }
        }
    }
    // Función para registrar el usuario en la base de datos
    private fun registerUser(rolId: Int, imagenPerfil: String, nombre: String, apellidos: String, correo: String, username: String, password: String, telefono: String, fechaNacimiento: String, domicilio: String, preguntaRecuperacionId: Int, respuestaRecuperacion: String, fingerprintEnabled: Boolean, intentosFallidos: Int, estadoCuenta: String, nombreTitular: String, numeroEncriptado: String, fechaExpiracion: String, marca: String, ultimos4: String) {
        // Se llama a la función insertUser del DatabaseHelper para insertar el usuario en la base de datos
        val result = databaseHelper.insertUser(rolId, imagenPerfil, nombre, apellidos, correo, username, password, telefono, fechaNacimiento, domicilio, preguntaRecuperacionId, respuestaRecuperacion, fingerprintEnabled, intentosFallidos, estadoCuenta, nombreTitular, numeroEncriptado, fechaExpiracion, marca, ultimos4)

        // Si el registro fue exitoso, se redirige al inicio de sesión, sino se muestra un mensaje de error
        if (result != -1L) {
            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, Login::class.java))
            finish()
        } else {
            Toast.makeText(this, "El registro falló", Toast.LENGTH_LONG).show()
        }
    }
    // Manejar el resultado de la selección de imagen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Si el usuario seleccionó una imagen, se guarda su URI en la variable
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {

            // Obtiene la URI de la imagen seleccionada
            val uri = data?.data

            // Si la URI no es nula, se guarda en la variable y se muestra en el ImageView
            if (uri != null) {

                // Valida tamaño máximo de 1MB
                val inputStream = contentResolver.openInputStream(uri)
                val size = inputStream?.available() ?: 0
                inputStream?.close()
                if (size > 1048576) { // 1MB en bytes
                    Toast.makeText(this, "La imagen debe ser menor a 1MB", Toast.LENGTH_LONG).show()
                    return
                }

                // Persiste permisos de lectura
                contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                imagenUsuarioUri = uri
                binding.imgUsuario.setImageURI(uri)

                // Crea el archivo temporal
                val archivoTemp = crearArchivoTemp(uri)


            }
        }
    }
}
