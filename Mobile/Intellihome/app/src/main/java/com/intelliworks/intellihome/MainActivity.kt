package com.intelliworks.intellihome

import com.intelliworks.intellihome.utils.BaseActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.model.User

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        val username = intent.getStringExtra("username")

        if (username != null) {
            val user = databaseHelper.getUserByUsername(username)
            user?.let { mostrarUsuario(it) }
        }
    }

    private fun mostrarUsuario(user: User) {
        binding.txtUserId.text = "ID: ${user.id}"
        binding.txtUsername.text = "Usuario: ${user.username}"
        binding.txtNombre.text = "Nombre: ${user.nombre} ${user.apellidos}"
        binding.txtCorreo.text = "Correo: ${user.correo}"
        binding.txtTelefono.text = "Teléfono: ${user.telefono}"
        binding.txtFechaNacimiento.text = "Fecha de nacimiento: ${user.fechaNacimiento}"
        binding.txtDomicilio.text = "Domicilio: ${user.domicilio}"

        // Aquí podrías usar tu arreglo de preguntas internacionalizado
        val preguntas = resources.getStringArray(R.array.preguntas_recuperacion)
        val pregunta = if (user.preguntaRecuperacionId in 1..preguntas.size)
            preguntas[user.preguntaRecuperacionId - 1]
        else
            "Pregunta no definida"
        binding.txtPreguntaRecuperacion.text = "Pregunta: $pregunta"

        binding.txtRespuestaRecuperacion.text = "Respuesta: ${user.respuestaRecuperacion}"
        binding.txtFingerprint.text =
            "Huella: ${if (user.fingerprintEnabled) "Activada" else "Desactivada"}"
        binding.txtEstadoCuenta.text = "Estado: ${user.estadoCuenta}"

        binding.txtDatosTarjeta.text =
            "Tarjeta: **** **** **** ${user.ultimos4} (${user.marca}) - Exp: ${user.fechaExpiracion}"
    }
    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }
}
