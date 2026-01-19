package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import android.os.Bundle
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.model.User

/**
 * Controlador principal de la aplicación que gestiona el panel de perfil de usuario.
 * Se encarga de la recuperación de la entidad completa desde persistencia y su
 * representación en la interfaz de usuario.
 */
class MainActivity : BaseActivity() {

    private lateinit var enlace: ActivityMainBinding
    private lateinit var baseDatos: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enlace = ActivityMainBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        baseDatos = DatabaseHelper(this)

        // Recuperación del identificador único transferido desde el flujo de autenticación
        val nombreUsuario = intent.getStringExtra("username")

        if (nombreUsuario != null) {
            val usuario = baseDatos.getUserByUsername(nombreUsuario)
            usuario?.let { desplegarInformacionUsuario(it) }
        }
    }

    /**
     * Vincula los atributos del modelo de usuario con los componentes de la vista.
     * Implementa lógica de mapeo para recursos internacionalizados y datos encriptados.
     */
    private fun desplegarInformacionUsuario(usuario: User) {
        enlace.txtUserId.text = "ID: ${usuario.id}"
        enlace.txtUsername.text = "Usuario: ${usuario.username}"
        enlace.txtNombre.text = "Nombre: ${usuario.nombre} ${usuario.apellidos}"
        enlace.txtCorreo.text = "Correo: ${usuario.correo}"
        enlace.txtTelefono.text = "Teléfono: ${usuario.telefono}"
        enlace.txtFechaNacimiento.text = "Fecha de nacimiento: ${usuario.fechaNacimiento}"
        enlace.txtDomicilio.text = "Domicilio: ${usuario.domicilio}"

        // Resolución dinámica de la pregunta de seguridad basada en el índice de recursos
        val catalogoPreguntas = resources.getStringArray(R.array.preguntas_recuperacion)
        val preguntaDesplegada = if (usuario.preguntaRecuperacionId in 1..catalogoPreguntas.size)
            catalogoPreguntas[usuario.preguntaRecuperacionId - 1]
        else
            "Pregunta no definida"

        enlace.txtPreguntaRecuperacion.text = "Pregunta: $preguntaDesplegada"

        enlace.txtRespuestaRecuperacion.text = "Respuesta: ${usuario.respuestaRecuperacion}"
        enlace.txtFingerprint.text =
            "Huella: ${if (usuario.fingerprintEnabled) "Activada" else "Desactivada"}"
        enlace.txtEstadoCuenta.text = "Estado: ${usuario.estadoCuenta}"

        // Representación de información financiera enmascarada para protección de datos
        enlace.txtDatosTarjeta.text =
            "Tarjeta: **** **** **** ${usuario.ultimos4} (${usuario.marca}) - Exp: ${usuario.fechaExpiracion}"
    }

    override fun onResume() {
        super.onResume()
        // Sincronización de la apariencia visual con las preferencias globales
        applyAppAppearance(enlace.root)
    }
}