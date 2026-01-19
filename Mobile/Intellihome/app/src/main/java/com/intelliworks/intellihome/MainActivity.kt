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
            val user: User? = databaseHelper.getUserByUsername(username)

            user?.let {
                mostrarUsuario(it)
            }
        }
    }

    private fun mostrarUsuario(user: User) {
        binding.txtUsername.text = "Usuario: ${user.username}"
        binding.txtUserId.text = "ID: ${user.id}"
        binding.txtFingerprint.text =
            if (user.fingerprintEnabled)
                "Huella: Activada"
            else
                "Huella: Desactivada"
    }
}
