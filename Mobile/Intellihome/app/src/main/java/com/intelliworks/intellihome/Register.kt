package com.intelliworks.intellihome

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import android.widget.CheckBox
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.util.Patterns
import android.view.View
import androidx.biometric.BiometricManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.launch
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.repository.CatalogosRepository
import com.intelliworks.intellihome.databinding.ActivityRegisterBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.encriptarClave
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.encriptarToken
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.generarClaveBiometrica
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.generarTokenPublico
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Cipher

/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding

    // Variable para almacenar la URI de la imagen de usuario
    private var imagenUsuarioUri: android.net.Uri? = null

    // Constante para el código de solicitud de selección de imagen
    companion object { private const val REQUEST_CODE_PICK_IMAGE = 1001 }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    // Crea un archivo temporal a partir de una URI
    private fun crearArchivoTemp(uri: android.net.Uri): java.io.File? {
        return try {
            var nombreArchivo = "imagen_temp.jpg"

            // Intenta obtener el nombre original del archivo desde la ruta
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    nombreArchivo = it.getString(nameIndex) // Si existe, lo asigna
                }
            }

            // Crea el archivo temporal en el directorio de caché con el nombre y extensión correctos
            val archivoTemp = java.io.File.createTempFile(
                nombreArchivo.substringBeforeLast('.'),
                "." + nombreArchivo.substringAfterLast('.', "jpg"),
                cacheDir
            )
            archivoTemp.deleteOnExit()

            // Copia el contenido de la ruta al archivo temporal
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

    // Método onCreate de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Instancias de API y repositorio
        val catalogosApi = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
        val catalogosRepository = CatalogosRepository(catalogosApi)

        // Deshabilita el checkbox de términos y condiciones
        binding.cbTerminos.isEnabled = false
        binding.cbTerminos.isChecked = false

        // Muestra AlertDialog de términos y condiciones al tocar el texto
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

        // Muestra/oculta contraseña
        binding.btnMostrarPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            // Si la contraseña es visible, la muestra, sino la oculta
            if (passwordVisible) {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_close_eye)
            }
            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }

        // Muestra/oculta confirmar contraseña
        binding.btnMostrarPassword2.setOnClickListener {
            confirmarPasswordVisible = !confirmarPasswordVisible
            // Si la contraseña es visible, la muestra, sino la oculta
            if (confirmarPasswordVisible) {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
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

        // Muestra el calendario al tocar el campo de fecha de nacimiento
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
        binding.btnCalendario.setOnClickListener {
            DatePickerDialog(
                this@Register,
                asignarFecha,
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Muestra el selector al tocar el campo de fecha de vencimiento de la tarjeta
        binding.etFechaVencimiento.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                selectorMesAnio { mes, anio ->
                    binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
                }
            }
        }
        binding.btnCalendario2.setOnClickListener {
            selectorMesAnio { mes, anio ->
                binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
            }
        }

        // Registrar usuario se guarda en la base de datos y se redirige al inicio de sesión (POST)
        binding.btnRegistrar.setOnClickListener {

            // Obtiene los datos del formulario
            val username = binding.etUsername.text.toString()
            val password = binding.etContrasena.text.toString()
            val confirmPassword = binding.etConfirmarContrasena.text.toString()
            val nombre = binding.etNombre.text.toString()
            val apellidos = binding.etApellidos.text.toString()
            val correo = binding.etCorreo.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val fechaNacimiento = binding.etFechaNacimiento.text.toString()
            val domicilio = binding.etDomicilio.text.toString()
            val preguntaRecuperacionId = binding.spPregunta.selectedItemPosition + 1                 // ID 1-based
            val respuestaRecuperacion = binding.etRespuesta.text.toString()
            val fingerprintEnabled = binding.cbHuellaDigital.isChecked
            val nombreTitular = binding.etTitularTarjeta.text.toString()
            val numeroEncriptado = binding.etNumeroTarjeta.text.toString()
            val fechaExpiracion = binding.etFechaVencimiento.text.toString()
            val tokenPublico = if (fingerprintEnabled) generarTokenPublico() else null               // Genera token solo si huella está habilitada

            val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)                            // Recolectar hobbies seleccionados
            val hobbiesIds = (1 until llHobbies.childCount).mapNotNull { idx ->
                val v = llHobbies.getChildAt(idx)
                if (v is CheckBox && v.isChecked) v.tag?.toString()?.toIntOrNull() else null
            }

            val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)                      // Recolectar tipos de casa seleccionados
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

            // Define la imagen de perfil y genera el archivo temporal para enviar
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

            // Configuración de partes para multipart
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
            val tokenPublicoRB = (tokenPublico ?: "").toRequestBody(textPlainUtf8)
            val nombreArchivoSeguro = archivoTemp.name.replace(Regex("[^A-Za-z0-9._-]"), "_") // Asegurar nombre de archivo seguro
            val imagenPerfilPart = okhttp3.MultipartBody.Part.createFormData("imagen_perfil", nombreArchivoSeguro, archivoTemp.asRequestBody("image/*".toMediaType()))

            // Función para el registro de usuario
            fun realizarRegistro(cifrado: Cipher? = null) {
                lifecycleScope.launch {

                    // Si se selecciona huella digital, se encripta el token usando el Cipher autenticado y se guarda
                    if (fingerprintEnabled && cifrado != null) {
                        val (tokenEncriptado, iv) = encriptarToken(tokenPublico ?: "", cifrado)
                        val prefs = getSharedPreferences("biometric_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("token_encriptado", tokenEncriptado)
                            .putString("token_iv", iv)
                            .apply()
                    }

                    // Intenta de registrar el usuario
                    try {
                        val response = usuarioRepo.registrarUsuario(
                            nombreRB, apellidosRB, usernameRB, correoRB, telefonoRB, fechaNacimientoRB, domicilioRB, contrasenaRB,
                            imagenPerfilPart, hobbiesIdsRB, tiposCasaIdsRB, preguntaRecuperacionIdRB, respuestaRecuperacionRB, permitirHuellaRB,
                            nombreTitularRB, numeroTarjetaRB, fechaExpiracionRB, tokenPublicoRB
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

            // Valida la selección de huella digital y si se puede usar antes de realizar el registro
            if (binding.cbHuellaDigital.isChecked) {
                if (canUseBiometric()) {
                    // Genera la clave biométrica antes del prompt
                    generarClaveBiometrica()
                    // Muestra el prompt biométrico para confirmar
                    showBiometricPrompt { autenticado ->
                        if (autenticado) {
                            // Encripta el token después de la autenticación exitosa
                            try {
                                val cifrado = encriptarClave()
                                realizarRegistro(cifrado)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Error al encriptar token: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
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

        // Obtiene la lista de hobbies (GET)
        lifecycleScope.launch {
            val response = catalogosRepository.getHobbies()
            if (response.isSuccessful) {
                val hobbies = response.body() ?: emptyList()
                val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)
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

        // Obtiene la lista de tipos de casa (GET)
        lifecycleScope.launch {
            val response = catalogosRepository.getTiposCasa()
            if (response.isSuccessful) {
                val tiposCasa = response.body() ?: emptyList()
                val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)
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

        // Obtiene la lista de preguntas de recuperación (GET)
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

        // Obtiene el NumberPicker para el mes y año
        val selectorMes = diagoloEmergente.findViewById<NumberPicker>(R.id.monthPicker)
        val selectorAnio = diagoloEmergente.findViewById<NumberPicker>(R.id.yearPicker)

        // Configura el rango permitido para el mes (1 a 12)
        selectorMes.minValue = 1
        selectorMes.maxValue = 12
        selectorMes.value = Calendar.getInstance().get(Calendar.MONTH) + 1

        // Obtiene el año actual y configura el rango permitido para el año
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        selectorAnio.minValue = anioActual
        selectorAnio.maxValue = anioActual + 20
        selectorAnio.value = anioActual

        // Construye y muestra el diálogo de selección
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
        // Verifica si el dispositivo puede usar huella digital
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Función para mostrar el prompt de huella digital
    private fun showBiometricPrompt(onSuccess: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        // Crea el prompt de autenticación biométrica
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // Se llama cuando la autenticación es exitosa
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(true)
                }
                // Se llama si ocurre un error durante la autenticación
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@Register, errString, Toast.LENGTH_LONG).show()
                    onSuccess(false)
                }
                // Se llama si la autenticación falla
                override fun onAuthenticationFailed() {
                    Toast.makeText(this@Register, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Configura el contenido del prompt biométrico y lo muestra
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar huella digital") // Título del diálogo
            .setSubtitle("Habilitar el inicio de sesión con huella digital") // Subtítulo
            .setNegativeButtonText("Cancelar") // Texto del botón de cancelar
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    // Función para mostrar el diálogo de términos y condiciones
    private fun mostrarDialogoTerminos() {
        val builder = AlertDialog.Builder(this)
        // Abre el archivo desde recursos, lee el texto y lo muestra
        val inputStream = resources.openRawResource(R.raw.terms)
        val terminos = inputStream.bufferedReader().use { it.readText() }
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this)
        textView.text = terminos                                        // Asigna el texto leído
        textView.setPadding(32, 32, 32, 32) // Añade padding para mejor legibilidad
        textView.textSize = 16f                                         // Ajusta el tamaño de letra
        scrollView.addView(textView)                             // Agrega el TextView al ScrollView

        // Configura el AlertDialog
        builder.setView(scrollView)
        builder.setNegativeButton("Cancelar", null)
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            binding.cbTerminos.isEnabled = true
            binding.cbTerminos.isChecked = true
            dialog.dismiss()
        }

        // Crea y muestra el diálogo
        val dialog = builder.create()
        dialog.show()

        // Obtiene el botón aceptar para controlarlo manualmente
        val btnAceptar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnAceptar.isEnabled = false

        // Habilita el botón aceptar solo si el usuario llega al final del texto
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(0)
            if (view != null) {
                val diff = view.bottom - (scrollView.height + scrollView.scrollY)
                btnAceptar.isEnabled = diff <= 0
            }
        }
    }

    // Controla la selección de imagen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Verifica si la acción corresponde a la selección de imagen y si fue exitosa
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            // Obtiene la ruta de la imagen seleccionada
            val uri = data?.data

            // Si la URI es válida, abre y lee la imagen, obtiene el tamaño y lo cierra
            if (uri != null) {
                val inputStream = contentResolver.openInputStream(uri)
                val size = inputStream?.available() ?: 0
                inputStream?.close()

                // Si la imagen supera 1MB, muestra un mensaje y cancela
                if (size > 1048576) { // 1MB en bytes
                    Toast.makeText(this, "La imagen debe ser menor a 1MB", Toast.LENGTH_LONG).show()
                    return
                }
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                imagenUsuarioUri = uri

                // Muestra la imagen seleccionada en el ImageView
                binding.imgUsuario.setImageURI(uri)
            }
        }
    }

}