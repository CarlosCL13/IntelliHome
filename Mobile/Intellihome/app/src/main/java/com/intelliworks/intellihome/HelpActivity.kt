package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.databinding.ActivityMainBinding
import com.intelliworks.intellihome.data.model.LoginResponseDto
import com.google.gson.Gson

class HelpActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }
}