package com.intelliworks.intellihome

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.intelliworks.intellihome.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Aquí puedes mostrar datos del usuario si lo deseas
    }
}