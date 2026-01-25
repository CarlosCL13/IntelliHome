package com.intelliworks.intellihome

import android.os.Bundle
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.databinding.ActivityMainBinding


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