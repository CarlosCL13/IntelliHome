package com.intelliworks.intellihome.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.intelliworks.intellihome.R

/**
 * Adaptador para mostrar imágenes en ViewPager2 usando Glide.
 */
class ImagePagerAdapter(
    private val images: List<String>,
    private val layoutId: Int,
    private val onItemClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgSlide)

        init {
            view.setOnClickListener {
                onItemClick?.invoke(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = images[position]

        // --- CORRECCIÓN: Usamos Glide para cargar desde URL ---
        val request = Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)

        // Ajuste de escala según si es pantalla completa o slider
        if (layoutId == R.layout.item_full_screen_image) {
            request.fitCenter()
        } else {
            request.centerCrop()
        }

        request.into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}