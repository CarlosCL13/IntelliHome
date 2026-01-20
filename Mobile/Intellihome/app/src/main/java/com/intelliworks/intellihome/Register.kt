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
import android.util.Patterns
import android.view.View

import com.intelliworks.intellihome.databinding.ActivityRegisterBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.repository.CatalogosRepository
import okhttp3.MediaType.Companion.toMediaType
import android.widget.CheckBox
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.intelliworks.intellihome.utils.BaseActivity
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // Variable para almacenar la URI de la imagen de usuario
    private var imagenUsuarioUri: android.net.Uri? = null

    // Constantes
    companion object {
        // Constante para el código de solicitud de selección de imagen
        private const val REQUEST_CODE_PICK_IMAGE = 1001
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

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    // Método onCreate de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Instancias de API y repositorio
        val catalogosApi = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
        val catalogosRepository = CatalogosRepository(catalogosApi)

        // Inicializar CheckBox de términos deshabilitado
        binding.cbTerminos.isEnabled = false
        binding.cbTerminos.isChecked = false

        // Mostrar AlertDialog de términos y condiciones al tocar el texto
        binding.tvVerTerminos.setOnClickListener {
            mostrarDialogoTerminos()
        }


        // Selección de imagen de usuario
        binding.imgUsuario.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg", "image/gif"))
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }


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


        // Registrar usuario se guarda en la base de datos y se redirige al inicio de sesión (POST)
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
            val preguntaRecuperacionId = binding.spPregunta.selectedItemPosition + 1        // ID 1-based
            val respuestaRecuperacion = binding.etRespuesta.text.toString()
            val fingerprintEnabled = binding.cbHuellaDigital.isChecked
            val nombreTitular = binding.etTitularTarjeta.text.toString()
            val numeroEncriptado = binding.etNumeroTarjeta.text.toString()
            val fechaExpiracion = binding.etFechaVencimiento.text.toString()

            val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)                    // Recolectar hobbies seleccionados
            val hobbiesIds = (1 until llHobbies.childCount).mapNotNull { idx ->
                val v = llHobbies.getChildAt(idx)
                if (v is CheckBox && v.isChecked) v.tag?.toString()?.toIntOrNull() else null
            }
            val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)              // Recolectar tipos de casa seleccionados
            val tiposCasaIds = (1 until llCasa.childCount).mapNotNull { idx ->
                val v = llCasa.getChildAt(idx)
                if (v is CheckBox && v.isChecked) v.tag?.toString()?.toIntOrNull() else null
            }

            // Validaciones básicas
            if (!binding.cbTerminos.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || telefono.isEmpty() || fechaNacimiento.isEmpty() || domicilio.isEmpty() || respuestaRecuperacion.isEmpty() || nombreTitular.isEmpty() || numeroEncriptado.isEmpty() || fechaExpiracion.isEmpty() || hobbiesIds.isEmpty() || tiposCasaIds.isEmpty()) {
                Toast.makeText(this, "Favor llenar todos los campos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Imagen de perfil
            val uri = imagenUsuarioUri
            if (uri == null) {
                Toast.makeText(this, "Selecciona una imagen de perfil", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val archivoTemp = crearArchivoTemp(uri)
            if (archivoTemp == null) {
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Preparar partes para multipart
            val usuarioApi = RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.UsuarioApi::class.java)
            val usuarioRepo = com.intelliworks.intellihome.data.repository.UsuarioRepository(usuarioApi)

            // MediaType con charset UTF-8 para manejar caracteres especiales
            val textPlainUtf8 = "text/plain; charset=utf-8".toMediaType()

            val nombreRB = nombre.toRequestBody(textPlainUtf8)
            val apellidosRB = apellidos.toRequestBody(textPlainUtf8)
            val usernameRB = username.toRequestBody(textPlainUtf8)
            val correoRB = correo.toRequestBody(textPlainUtf8)
            val telefonoRB = telefono.toRequestBody(textPlainUtf8)
            val fechaNacimientoRB = fechaNacimiento.toRequestBody(textPlainUtf8)
            val domicilioRB = domicilio.toRequestBody(textPlainUtf8)
            val contrasenaRB = password.toRequestBody(textPlainUtf8)
            val hobbiesIdsRB = hobbiesIds.joinToString(",").toRequestBody(textPlainUtf8)
            val tiposCasaIdsRB = tiposCasaIds.joinToString(",").toRequestBody(textPlainUtf8)
            val preguntaRecuperacionIdRB = preguntaRecuperacionId.toString().toRequestBody(textPlainUtf8)
            val respuestaRecuperacionRB = respuestaRecuperacion.toRequestBody(textPlainUtf8)
            val permitirHuellaRB = (if (fingerprintEnabled) "1" else "0").toRequestBody(textPlainUtf8)
            val nombreTitularRB = nombreTitular.toRequestBody(textPlainUtf8)
            val numeroTarjetaRB = numeroEncriptado.toRequestBody(textPlainUtf8)
            val fechaExpiracionRB = fechaExpiracion.toRequestBody(textPlainUtf8)

            // Sanitizar el nombre del archivo para evitar errores por caracteres especiales
            val nombreArchivoSeguro = archivoTemp.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val imagenPerfilPart = okhttp3.MultipartBody.Part.createFormData(
                "imagen_perfil",
                nombreArchivoSeguro,
                archivoTemp.asRequestBody("image/*".toMediaType())
            )

            fun realizarRegistro() {
                lifecycleScope.launch {
                    try {
                        val response = usuarioRepo.registrarUsuario(
                            nombreRB, apellidosRB, usernameRB, correoRB, telefonoRB, fechaNacimientoRB, domicilioRB, contrasenaRB,
                            imagenPerfilPart, hobbiesIdsRB, tiposCasaIdsRB, preguntaRecuperacionIdRB, respuestaRecuperacionRB, permitirHuellaRB,
                            nombreTitularRB, numeroTarjetaRB, fechaExpiracionRB
                        )
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body?.errores.isNullOrEmpty()) {
                                Toast.makeText(this@Register, body?.mensaje ?: "Registro exitoso", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this@Register, Login::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@Register, body.errores.joinToString("\n"), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@Register, "Error en el registro: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@Register, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (binding.cbHuellaDigital.isChecked) {
                if (canUseBiometric()) {
                    showBiometricPrompt {
                        realizarRegistro()
                    }
                } else {
                    Toast.makeText(this, "Huella digital no disponible en este dispositivo", Toast.LENGTH_LONG).show()
                }
            } else {
                realizarRegistro()
            }
        }


        // Redirigir al inicio de sesión al tocar el texto correspondiente
        binding.btnLoginRedirigir.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }


        // Poblar hobbies dinámicamente (GET)
        lifecycleScope.launch {
            val response = catalogosRepository.getHobbies()
            if (response.isSuccessful) {
                val hobbies = response.body() ?: emptyList()
                val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)
                // Elimina los hijos actuales excepto el TextView título
                while (llHobbies.childCount > 1) llHobbies.removeViewAt(1)
                hobbies.forEach { hobby ->
                    val checkBox = CheckBox(this@Register)
                    checkBox.id = View.generateViewId()
                    checkBox.text = hobby.nombre
                    checkBox.tag = hobby.id
                    checkBox.textSize = 14f
                    llHobbies.addView(checkBox)
                }
            } else {
                Toast.makeText(this@Register, "Error al obtener hobbies", Toast.LENGTH_SHORT).show()
            }
        }

        // Poblar tipos de casa dinámicamente (GET)
        lifecycleScope.launch {
            val response = catalogosRepository.getTiposCasa()
            if (response.isSuccessful) {
                val tiposCasa = response.body() ?: emptyList()
                val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)
                // Elimina los hijos actuales excepto el TextView título
                while (llCasa.childCount > 1) llCasa.removeViewAt(1)
                tiposCasa.forEach { tipo ->
                    val checkBox = CheckBox(this@Register)
                    checkBox.id = View.generateViewId()
                    checkBox.text = tipo.nombre
                    checkBox.tag = tipo.id
                    checkBox.textSize = 14f
                    llCasa.addView(checkBox)
                }
            } else {
                Toast.makeText(this@Register, "Error al obtener tipos de casa", Toast.LENGTH_SHORT).show()
            }
        }

        // Poblar preguntas de recuperación en el Spinner (GET)
        lifecycleScope.launch {
            val response = catalogosRepository.getPreguntasRecuperacion()
            if (response.isSuccessful) {
                val preguntas = response.body() ?: emptyList()
                val textos = preguntas.filterNotNull().map { it.texto }
                if (textos.isNotEmpty()) {
                    val adapter = ArrayAdapter(this@Register, android.R.layout.simple_spinner_dropdown_item, textos)
                    binding.spPregunta.adapter = adapter
                    binding.spPregunta.isEnabled = true
                } else {
                    Toast.makeText(this@Register, "No hay preguntas de recuperación disponibles", Toast.LENGTH_SHORT).show()
                    binding.spPregunta.isEnabled = false
                }
                // Puedes guardar los IDs en una variable si necesitas saber cuál seleccionó el usuario
            } else {
                Toast.makeText(this@Register, "Error al obtener preguntas de recuperación", Toast.LENGTH_SHORT).show()
            }
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

    // Manejar el resultado de la selección de imagen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                imagenUsuarioUri = uri
                binding.imgUsuario.setImageURI(uri)

                // Crea el archivo temporal
                val archivoTemp = crearArchivoTemp(uri)


            }
        }
    }

}
