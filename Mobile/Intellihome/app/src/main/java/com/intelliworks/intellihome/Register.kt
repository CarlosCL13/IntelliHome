package com.intelliworks.intellihome

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.CheckBox
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.launch
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.CatalogosApi
import com.intelliworks.intellihome.data.model.DatosFormularioRegistro
import com.intelliworks.intellihome.data.repository.CatalogosRepository
import com.intelliworks.intellihome.databinding.ActivityRegisterBinding
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.encriptarClave
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.encriptarToken
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.generarClaveBiometrica
import com.intelliworks.intellihome.utils.BiometricCryptoHelper.generarTokenPublico
import com.intelliworks.intellihome.utils.RegisterHelper
import com.intelliworks.intellihome.utils.RegistroValidator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Cipher

/**
 * Clase para la pantalla de registro de usuario.
 */
class Register : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val validator = RegistroValidator()

    // Variable para almacenar la URI de la imagen de usuario
    private var imagenUsuarioUri: android.net.Uri? = null

    // Constante para el código de solicitud de selección de imagen
    companion object { private const val REQUEST_CODE_PICK_IMAGE = 1001 }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }

    // Método onCreate de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar listeners de UI
        configurarListenersUI()

        // Cargar catálogos desde el servidor
        cargarCatalogos()
    }

    // Configura todos los listeners de la interfaz de usuario
    private fun configurarListenersUI() {
        // Deshabilita el checkbox de términos y condiciones
        binding.cbTerminos.isEnabled = false
        binding.cbTerminos.isChecked = false

        // Muestra AlertDialog de términos y condiciones al tocar el texto
        binding.tvVerTerminos.setOnClickListener {
            RegisterHelper.mostrarDialogoTerminos(this, R.raw.terms) {
                binding.cbTerminos.isEnabled = true
                binding.cbTerminos.isChecked = true
            }
        }

        // Selección de imagen de usuario
        binding.imgUsuario.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg", "image/gif"))
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        // Configura los botones de mostrar/ocultar contraseña
        configurarTogglePassword()

        // Configura los selectores de fecha
        configurarSelectoresFecha()

        // Configura el botón de registro
        configurarBotonRegistrar()

        // Configura el botón para ir a login
        configurarBotonLogin()
    }

    // Configura los botones de mostrar/ocultar contraseña
    private fun configurarTogglePassword() {
        var passwordVisible = false
        var confirmarPasswordVisible = false

        // Alterna la visibilidad de la contraseña al tocar el botón
        binding.btnMostrarPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword.setImageResource(R.drawable.ic_close_eye)
            }
            binding.etContrasena.setSelection(binding.etContrasena.text?.length ?: 0)
        }
        // Alterna la visibilidad de la confirmación de contraseña al tocar el botón
        binding.btnMostrarPassword2.setOnClickListener {
            confirmarPasswordVisible = !confirmarPasswordVisible
            if (confirmarPasswordVisible) {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_open_eye)
            } else {
                binding.etConfirmarContrasena.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
                binding.btnMostrarPassword2.setImageResource(R.drawable.ic_close_eye)
            }
            binding.etConfirmarContrasena.setSelection(binding.etConfirmarContrasena.text?.length ?: 0)
        }
    }

    // Configura los selectores de fecha (nacimiento y tarjeta)
    private fun configurarSelectoresFecha() {
        val calendario = Calendar.getInstance()
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd")
        val asignarFecha = DatePickerDialog.OnDateSetListener { _, anio, mes, dia ->
            calendario.set(Calendar.YEAR, anio)
            calendario.set(Calendar.MONTH, mes)
            calendario.set(Calendar.DAY_OF_MONTH, dia)
            binding.etFechaNacimiento.setText(formatoFecha.format(calendario.time))
        }

        // Selector de fecha de nacimiento
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
        // Botón para abrir el calendario
        binding.btnCalendario.setOnClickListener {
            DatePickerDialog(
                this@Register,
                asignarFecha,
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Selector de fecha de vencimiento de la tarjeta
        binding.etFechaVencimiento.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                RegisterHelper.mostrarSelectorMesAnio(this@Register) { mes, anio ->
                    binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
                }
            }
        }
        // Botón para abrir el selector mes/año
        binding.btnCalendario2.setOnClickListener {
            RegisterHelper.mostrarSelectorMesAnio(this@Register) { mes, anio ->
                binding.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
            }
        }
    }

    // Configura el botón de registro
    private fun configurarBotonRegistrar() {
        binding.btnRegistrar.setOnClickListener {
            val mensajeError = validarDatosRegistro()

            // Si hay algún error en la validación, muestra un Toast y detiene el proceso
            if (mensajeError != null) {
                Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Crea un archivo temporal a partir de la URI de la imagen seleccionada
            val archivoTemp = RegisterHelper.crearArchivoTemporal(this, imagenUsuarioUri!!)
            if (archivoTemp == null) {
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Procesa el registro, posiblemente con autenticación biométrica
            procesarRegistroConBiometria(archivoTemp)
        }
    }

    // Configura el botón para ir a login
    private fun configurarBotonLogin() {
        binding.btnLoginRedirigir.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Recolecta todos los datos del formulario
    private fun recolectarDatosFormulario(): DatosFormularioRegistro {
        return DatosFormularioRegistro(
            username = binding.etUsername.text.toString(),
            password = binding.etContrasena.text.toString(),
            confirmPassword = binding.etConfirmarContrasena.text.toString(),
            nombre = binding.etNombre.text.toString(),
            apellidos = binding.etApellidos.text.toString(),
            correo = binding.etCorreo.text.toString(),
            telefono = binding.etTelefono.text.toString(),
            fechaNacimiento = binding.etFechaNacimiento.text.toString(),
            domicilio = binding.etDomicilio.text.toString(),
            preguntaRecuperacionId = binding.spPregunta.selectedItemPosition + 1,
            respuestaRecuperacion = binding.etRespuesta.text.toString(),
            fingerprintEnabled = binding.cbHuellaDigital.isChecked,
            nombreTitular = binding.etTitularTarjeta.text.toString(),
            numeroEncriptado = binding.etNumeroTarjeta.text.toString(),
            fechaExpiracion = binding.etFechaVencimiento.text.toString(),
            hobbiesIds = recolectarHobbiesSeleccionados(),
            tiposCasaIds = recolectarTiposCasaSeleccionados()
        )
    }

    // Valida todos los datos del formulario y retorna mensaje de error si hay alguno
    private fun validarDatosRegistro(): String? {
        val datos = recolectarDatosFormulario()
        return validator.validarFormularioCompleto(
            datos = datos,
            terminosAceptados = binding.cbTerminos.isChecked,
            imagenSeleccionada = imagenUsuarioUri != null
        )
    }

    // Recolecta los IDs de los hobbies seleccionados
    private fun recolectarHobbiesSeleccionados(): List<Int> {
        val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)
        return (1 until llHobbies.childCount).mapNotNull { idx ->
            val v = llHobbies.getChildAt(idx)
            if (v is CheckBox && v.isChecked) v.tag?.toString()?.toIntOrNull() else null
        }
    }

    // Recolecta los IDs de los tipos de casa seleccionados
    private fun recolectarTiposCasaSeleccionados(): List<Int> {
        val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)
        return (1 until llCasa.childCount).mapNotNull { idx ->
            val v = llCasa.getChildAt(idx)
            if (v is CheckBox && v.isChecked) v.tag?.toString()?.toIntOrNull() else null
        }
    }

    // Procesa el registro con autenticación biométrica si está habilitada
    private fun procesarRegistroConBiometria(archivoTemp: java.io.File) {
        if (binding.cbHuellaDigital.isChecked) {
            if (canUseBiometric()) {
                generarClaveBiometrica()
                showBiometricPrompt { autenticado ->
                    if (autenticado) {
                        try {
                            val cifrado = encriptarClave()
                            ejecutarRegistroUsuario(archivoTemp, cifrado)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error al encriptar token: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Huella digital no disponible en este dispositivo", Toast.LENGTH_LONG).show()
            }
        } else {
            ejecutarRegistroUsuario(archivoTemp, null)
        }
    }

    // Ejecuta el registro del usuario en el servidor
    private fun ejecutarRegistroUsuario(archivoTemp: java.io.File, cifrado: Cipher?) {
        lifecycleScope.launch {
            // Obtener datos del formulario
            val datos = recolectarDatosFormulario()
            val tokenPublico = if (datos.fingerprintEnabled) generarTokenPublico() else null

            // Si se selecciona huella digital, encripta y guarda el token
            if (datos.fingerprintEnabled && cifrado != null && tokenPublico != null) {
                val (tokenEncriptado, iv) = encriptarToken(tokenPublico, cifrado)
                val prefs = getSharedPreferences("biometric_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("token_encriptado", tokenEncriptado)
                    .putString("token_iv", iv)
                    .apply()
            }

            // Preparar request multipart
            val textPlainUtf8 = "text/plain; charset=utf-8".toMediaType()
            val nombreRB = datos.nombre.toRequestBody(textPlainUtf8)
            val apellidosRB = datos.apellidos.toRequestBody(textPlainUtf8)
            val usernameRB = datos.username.toRequestBody(textPlainUtf8)
            val correoRB = datos.correo.toRequestBody(textPlainUtf8)
            val telefonoRB = datos.telefono.toRequestBody(textPlainUtf8)
            val fechaNacimientoRB = datos.fechaNacimiento.toRequestBody(textPlainUtf8)
            val domicilioRB = datos.domicilio.toRequestBody(textPlainUtf8)
            val contrasenaRB = datos.password.toRequestBody(textPlainUtf8)
            val hobbiesIdsRB = datos.hobbiesIds.joinToString(",").toRequestBody(textPlainUtf8)
            val tiposCasaIdsRB = datos.tiposCasaIds.joinToString(",").toRequestBody(textPlainUtf8)
            val preguntaRecuperacionIdRB = datos.preguntaRecuperacionId.toString().toRequestBody(textPlainUtf8)
            val respuestaRecuperacionRB = datos.respuestaRecuperacion.toRequestBody(textPlainUtf8)
            val permitirHuellaRB = (if (datos.fingerprintEnabled) "1" else "0").toRequestBody(textPlainUtf8)
            val nombreTitularRB = datos.nombreTitular.toRequestBody(textPlainUtf8)
            val numeroTarjetaRB = datos.numeroEncriptado.toRequestBody(textPlainUtf8)
            val fechaExpiracionRB = datos.fechaExpiracion.toRequestBody(textPlainUtf8)
            val tokenPublicoRB = (tokenPublico ?: "").toRequestBody(textPlainUtf8)
            val nombreArchivoSeguro = archivoTemp.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val imagenPerfilPart = okhttp3.MultipartBody.Part.createFormData(
                "imagen_perfil",
                nombreArchivoSeguro,
                archivoTemp.asRequestBody("image/*".toMediaType())
            )

            // Crear repositorio y realizar registro
            val usuarioApi = RetrofitInstance.retrofit.create(com.intelliworks.intellihome.data.api.UsuarioApi::class.java)
            val usuarioRepo = com.intelliworks.intellihome.data.repository.UsuarioRepository(usuarioApi)

            try {
                val response = usuarioRepo.registrarUsuario(
                    nombreRB, apellidosRB, usernameRB, correoRB, telefonoRB, fechaNacimientoRB,
                    domicilioRB, contrasenaRB, imagenPerfilPart, hobbiesIdsRB, tiposCasaIdsRB,
                    preguntaRecuperacionIdRB, respuestaRecuperacionRB, permitirHuellaRB,
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

    // Carga los catálogos desde el servidor
    private fun cargarCatalogos() {
        val catalogosApi = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
        val catalogosRepository = CatalogosRepository(catalogosApi)

        lifecycleScope.launch {
            cargarHobbies(catalogosRepository)
        }

        lifecycleScope.launch {
            cargarTiposCasa(catalogosRepository)
        }

        lifecycleScope.launch {
            cargarPreguntasRecuperacion(catalogosRepository)
        }
    }

    // Carga la lista de hobbies
    private suspend fun cargarHobbies(catalogosRepository: CatalogosRepository) {
        val hobbies = catalogosRepository.obtenerHobbies()

        if (hobbies.isNotEmpty()) {
            val llHobbies = findViewById<ViewGroup>(R.id.ll_hobbies)
            while (llHobbies.childCount > 1) llHobbies.removeViewAt(1)

            hobbies.forEach { hobby ->
                val checkBox = CheckBox(this@Register)
                checkBox.id = View.generateViewId()

                // --- TRADUCCIÓN POR ID ---
                checkBox.text = obtenerNombreHobby(hobby.id, hobby.nombre)
                // -------------------------

                checkBox.tag = hobby.id
                checkBox.textSize = 14f
                llHobbies.addView(checkBox)
            }
        } else {
            Toast.makeText(this@Register, "No se pudieron cargar los hobbies", Toast.LENGTH_SHORT).show()
        }
    }

    // Carga la lista de tipos de casa
    private suspend fun cargarTiposCasa(catalogosRepository: CatalogosRepository) {
        val tiposCasa = catalogosRepository.obtenerTipos()

        if (tiposCasa.isNotEmpty()) {
            val llCasa = findViewById<ViewGroup>(R.id.ll_casa_preferencia)
            while (llCasa.childCount > 1) llCasa.removeViewAt(1)

            tiposCasa.forEach { tipo ->
                val checkBox = CheckBox(this@Register)
                checkBox.id = View.generateViewId()

                // --- TRADUCCIÓN POR ID ---
                checkBox.text = obtenerNombreTipoCasa(tipo.id, tipo.nombre)
                // -------------------------

                checkBox.tag = tipo.id
                checkBox.textSize = 14f
                llCasa.addView(checkBox)
            }
        } else {
            Toast.makeText(this@Register, "No se pudieron cargar los tipos de casa", Toast.LENGTH_SHORT).show()
        }
    }

    // Carga la lista de preguntas de recuperación
    private suspend fun cargarPreguntasRecuperacion(catalogosRepository: CatalogosRepository) {
        val api = RetrofitInstance.retrofit.create(CatalogosApi::class.java)
        val response = api.getPreguntasRecuperacion()

        if (response.isSuccessful) {
            val preguntas = response.body() ?: emptyList()

            // --- TRADUCCIÓN POR ID ---
            val textosTraducidos = preguntas.map {
                obtenerPreguntaTraducida(it.id, it.texto)
            }
            // -------------------------

            if (textosTraducidos.isNotEmpty()) {
                val adapter = ArrayAdapter(this@Register, android.R.layout.simple_spinner_dropdown_item, textosTraducidos)
                binding.spPregunta.adapter = adapter
                binding.spPregunta.isEnabled = true
            }
        } else {
            Toast.makeText(this@Register, "Error al cargar preguntas", Toast.LENGTH_SHORT).show()
        }
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

    // Controla la selección de imagen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Verifica si la acción corresponde a la selección de imagen y si fue exitosa
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            // Obtiene la ruta de la imagen seleccionada
            val uri = data?.data

            // Si la URI es válida, valida el tamaño de la imagen
            if (uri != null) {
                // Valida el tamaño de la imagen
                if (!RegisterHelper.validarTamanoImagen(this, uri)) {
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

    // ------------------------------------------------------------------------
    // FUNCIONES AUXILIARES DE TRADUCCIÓN
    // ------------------------------------------------------------------------

    private fun obtenerNombreHobby(id: Int, original: String): String {
        return when (id) {
            1 -> getString(R.string.hobby_tv)           // Ver TV
            2 -> getString(R.string.hobby_hiking)       // Senderismo
            3 -> getString(R.string.hobby_boardgames)   // Juegos de mesa
            4 -> getString(R.string.hobby_snorkel)   // Esnórquel
            5 -> getString(R.string.hobby_sports)       // Deportes
            else -> original
        }
    }

    private fun obtenerNombreTipoCasa(id: Int, original: String): String {
        return when (id) {
            1 -> getString(R.string.house_minimalist)   // Aventurera
            2 -> getString(R.string.house_contemporary)  // Contemporánea
            3 -> getString(R.string.house_adventurous)    // Minimalista
            else -> original
        }
    }

    private fun obtenerPreguntaTraducida(id: Int, original: String): String {
        // IDs basados en el orden de tu base de datos sqlite
        return when (id) {
            1 -> getString(R.string.rec_soccer)     // Primer país
            2 -> getString(R.string.rec_teacher)      // Fruta favorita
            3 -> getString(R.string.rec_birthplace) // Lugar de nacimiento
            4 -> getString(R.string.rec_fruit)    // Profesor preferido
            5 -> getString(R.string.rec_travel)     // Mejor equipo CR
            else -> original
        }
    }

}