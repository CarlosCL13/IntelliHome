package com.intelliworks.intellihome.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE)
        val language = prefs.getString("language", "es") ?: "es"

        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }
}
