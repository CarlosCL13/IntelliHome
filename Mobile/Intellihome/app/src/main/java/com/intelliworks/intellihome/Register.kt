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
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
import java.text.SimpleDateFormat
import java.util.Calendar
import android.util.Patterns

/**
 * Controlador para la gestión de registro de nuevos usuarios.
 * Implementa validaciones de integridad, selección de recursos multimedia,
 * aceptación de políticas legales y aprovisionamiento de seguridad biométrica.
 */
class Register : BaseActivity() {

    private lateinit var enlace: ActivityRegisterBinding
    private lateinit var baseDatos: DatabaseHelper

    // Referencia local para la persistencia de la imagen de perfil seleccionada
    private var uriImagenUsuario: android.net.Uri? = null

    companion object {
        private const val CODIGO_SOLICITUD_IMAGEN = 1001
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(enlace.root)
    }

    /**
     * Genera un archivo de respaldo temporal para la gestión de la imagen seleccionada.
     */
    private fun crearArchivoTemporal(uri: android.net.Uri): java.io.File? {
        return try {
            var nombreArchivo = "imagen_temp.jpg"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val indiceNombre = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && indiceNombre != -1) {
                    nombreArchivo = it.getString(indiceNombre)
                }
            }

            val archivoTemp = java.io.File.createTempFile(
                nombreArchivo.substringBeforeLast('.'),
                "." + nombreArchivo.substringAfterLast('.', "jpg"),
                cacheDir
            )
            archivoTemp.deleteOnExit()

            contentResolver.openInputStream(uri)?.use { entrada ->
                archivoTemp.outputStream().use { salida ->
                    entrada.copyTo(salida)
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
        enlace = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(enlace.root)
        applyAppAppearance(enlace.root)

        baseDatos = DatabaseHelper(this)

        // Estado inicial de cumplimiento legal
        enlace.cbTerminos.isEnabled = false
        enlace.cbTerminos.isChecked = false

        enlace.tvVerTerminos.setOnClickListener {
            mostrarDialogoTerminos()
        }

        // Gestión de selección de avatar de usuario
        enlace.imgUsuario.setOnClickListener {
            val intentGaleria = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpg", "image/jpeg"))
            }
            startActivityForResult(intentGaleria, CODIGO_SOLICITUD_IMAGEN)
        }

        // Lógica de alternancia de visibilidad de credenciales
        configurarControlVisibilidadClave()

        // Configuración de adaptadores para la recuperación de cuenta
        val adaptadorPreguntas = android.widget.ArrayAdapter.createFromResource(
            this,
            R.array.preguntas_recuperacion,
            android.R.layout.simple_spinner_item
        )
        adaptadorPreguntas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        enlace.spPregunta.adapter = adaptadorPreguntas

        // Inicialización de componentes de selección de fecha
        configurarSelectoresFecha()

        enlace.btnRegistrar.setOnClickListener {
            ejecutarFlujoRegistro()
        }

        enlace.btnLoginRedirigir.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    /**
     * Valida y procesa la inserción del nuevo usuario en el sistema.
     */
    private fun ejecutarFlujoRegistro() {
        val nombreUsuario = enlace.etUsername.text.toString()
        val clave = enlace.etContrasena.text.toString()
        val confirmacionClave = enlace.etConfirmarContrasena.text.toString()
        val correo = enlace.etCorreo.text.toString()
        val numeroTarjeta = enlace.etNumeroTarjeta.text.toString()

        // Validaciones de integridad y cumplimiento
        if (!enlace.cbTerminos.isChecked) {
            Toast.makeText(this, "Debe aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Formato de correo inválido", Toast.LENGTH_LONG).show()
            return
        }

        if (clave != confirmacionClave) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
            return
        }

        // Proceso de aprovisionamiento biométrico opcional
        if (enlace.cbHuellaDigital.isChecked) {
            if (dispositivoSoportaBiometria()) {
                solicitarRegistroBiometrico {
                    persistirUsuario(true)
                }
            } else {
                Toast.makeText(this, "Biometría no disponible", Toast.LENGTH_LONG).show()
                persistirUsuario(false)
            }
        } else {
            persistirUsuario(false)
        }
    }

    /**
     * Realiza la llamada transaccional a la base de datos para crear el registro.
     */
    private fun persistirUsuario(huellaHabilitada: Boolean) {
        val resultado = baseDatos.insertUser(
            rolId = 2,
            imagenPerfil = uriImagenUsuario?.toString() ?: "",
            nombre = enlace.etNombre.text.toString(),
            apellidos = enlace.etApellidos.text.toString(),
            correo = enlace.etCorreo.text.toString(),
            username = enlace.etUsername.text.toString(),
            contrasena = enlace.etContrasena.text.toString(),
            telefono = enlace.etTelefono.text.toString(),
            fechaNacimiento = enlace.etFechaNacimiento.text.toString(),
            domicilio = enlace.etDomicilio.text.toString(),
            preguntaId = enlace.spPregunta.selectedItemPosition + 1,
            respuesta = enlace.etRespuesta.text.toString(),
            huellaActiva = huellaHabilitada,
            intentos = 0,
            estado = "activo",
            titular = enlace.etTitularTarjeta.text.toString(),
            numeroEnc = enlace.etNumeroTarjeta.text.toString(),
            expiracion = enlace.etFechaVencimiento.text.toString(),
            marca = "Mastercard",
            ultimos4 = enlace.etNumeroTarjeta.text.toString().takeLast(4).ifEmpty { "0000" }
        )

        if (resultado != -1L) {
            Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, Login::class.java))
            finish()
        } else {
            Toast.makeText(this, "Error en el registro de datos", Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarControlVisibilidadClave() {
        var esVisible = false
        enlace.btnMostrarPassword.setOnClickListener {
            esVisible = !esVisible
            val fuenteActual = enlace.etContrasena.typeface
            enlace.etContrasena.inputType = if (esVisible)
                TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
            enlace.etContrasena.typeface = fuenteActual
            enlace.etContrasena.setSelection(enlace.etContrasena.text?.length ?: 0)
        }
    }

    private fun configurarSelectoresFecha() {
        val calendario = Calendar.getInstance()
        val listenerFecha = DatePickerDialog.OnDateSetListener { _, anio, mes, dia ->
            calendario.set(anio, mes, dia)
            enlace.etFechaNacimiento.setText(SimpleDateFormat("yyyy-MM-dd").format(calendario.time))
        }

        enlace.etFechaNacimiento.setOnClickListener {
            DatePickerDialog(this, listenerFecha,
                calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show()
        }

        enlace.etFechaVencimiento.setOnClickListener {
            mostrarSelectorMesAnio { mes, anio ->
                enlace.etFechaVencimiento.setText(String.format("%02d/%d", mes, anio))
            }
        }
    }

    private fun mostrarSelectorMesAnio(alSeleccionar: (Int, Int) -> Unit) {
        val vistaDialogo = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val pickerMes = vistaDialogo.findViewById<NumberPicker>(R.id.monthPicker).apply { minValue = 1; maxValue = 12 }
        val pickerAnio = vistaDialogo.findViewById<NumberPicker>(R.id.yearPicker).apply {
            val anioBase = Calendar.getInstance().get(Calendar.YEAR)
            minValue = anioBase; maxValue = anioBase + 20
        }

        AlertDialog.Builder(this)
            .setTitle("Vencimiento de tarjeta")
            .setView(vistaDialogo)
            .setPositiveButton("Aceptar") { _, _ -> alSeleccionar(pickerMes.value, pickerAnio.value) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dispositivoSoportaBiometria(): Boolean {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun solicitarRegistroBiometrico(alExito: () -> Unit) {
        val ejecutor = ContextCompat.getMainExecutor(this)
        val promptBiometrico = BiometricPrompt(this, ejecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(resultado)
                alExito()
            }
        })

        val infoPrompt = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Seguridad Biométrica")
            .setSubtitle("Confirme su identidad para habilitar acceso rápido")
            .setNegativeButtonText("Omitir")
            .build()

        promptBiometrico.authenticate(infoPrompt)
    }

    private fun mostrarDialogoTerminos() {
        val terminosTexto = resources.openRawResource(R.raw.terms).bufferedReader().use { it.readText() }
        val scrollContenedor = android.widget.ScrollView(this)
        val vistaTexto = android.widget.TextView(this).apply {
            text = terminosTexto; setPadding(48, 48, 48, 48); textSize = 15f
        }
        scrollContenedor.addView(vistaTexto)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Términos y Condiciones")
            .setView(scrollContenedor)
            .setPositiveButton("Aceptar") { _, _ ->
                enlace.cbTerminos.isEnabled = true
                enlace.cbTerminos.isChecked = true
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.show()
        val botonAceptar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE).apply { isEnabled = false }

        scrollContenedor.viewTreeObserver.addOnScrollChangedListener {
            val vistaHija = scrollContenedor.getChildAt(0)
            if (vistaHija != null) {
                val diferencia = vistaHija.bottom - (scrollContenedor.height + scrollContenedor.scrollY)
                botonAceptar.isEnabled = diferencia <= 0 // Solo habilita si el usuario leyó hasta el final
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_SOLICITUD_IMAGEN && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uriImagenUsuario = uri
                enlace.imgUsuario.setImageURI(uri)
                crearArchivoTemporal(uri)
            }
        }
    }
}