package com.intelliworks.intellihome

import android.os.Bundle
import com.intelliworks.intellihome.databinding.ActivityAddPropertyBinding
import com.intelliworks.intellihome.utils.BaseActivity

class AddPropertyActivity : BaseActivity() {

    private lateinit var binding: ActivityAddPropertyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showSettingsButton(false)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyTypeFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppAppearance(binding.root)
    }
}