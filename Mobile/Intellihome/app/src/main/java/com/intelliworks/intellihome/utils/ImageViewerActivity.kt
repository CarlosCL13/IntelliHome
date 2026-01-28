package com.intelliworks.intellihome

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.intelliworks.intellihome.utils.BaseActivity
import com.intelliworks.intellihome.utils.ImagePagerAdapter

class ImageViewerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerFull)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val tvCounter = findViewById<TextView>(R.id.tvCounter)

        val imageList = intent.getStringArrayListExtra("images") ?: arrayListOf()
        val startPosition = intent.getIntExtra("position", 0)

        // Pasamos el layout de pantalla completa (R.layout.item_full_screen_image)
        val adapter = ImagePagerAdapter(
            images = imageList,
            layoutId = R.layout.item_full_screen_image, // <--- Layout para full screen (fitCenter)
            onItemClick = null // No necesitamos clic aquí
        )
        viewPager.adapter = adapter

        viewPager.setCurrentItem(startPosition, false)

        if (imageList.isNotEmpty()) {
            tvCounter.text = "${startPosition + 1}/${imageList.size}"
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvCounter.text = "${position + 1}/${imageList.size}"
            }
        })

        btnClose.setOnClickListener {
            finish()
        }
    }
}