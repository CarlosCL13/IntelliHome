package com.intelliworks.intellihome.utils

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.R

/**
 * Adaptador versátil para mostrar imágenes en un ViewPager2.
 * @param images Lista de URIs (Strings) de las imágenes.
 * @param layoutId El ID del recurso R.layout que se usará para cada ítem (ej. R.layout.item_image_slider).
 * @param onItemClick Listener opcional para manejar clics en la imagen.
 */
class ImagePagerAdapter(
    private val images: List<String>,
    private val layoutId: Int, // <--- NUEVO PARÁMETRO
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
        // Usamos el layoutId que recibimos en el constructor
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        try {
            holder.imageView.setImageURI(Uri.parse(images[position]))
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    override fun getItemCount(): Int = images.size
}