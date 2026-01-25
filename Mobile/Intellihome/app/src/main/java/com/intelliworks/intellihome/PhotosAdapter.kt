package com.intelliworks.intellihome

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PhotosAdapter(
    private val onDeleteClick: (Uri) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    private var photos: List<Uri> = emptyList()

    fun submitList(newList: List<Uri>) {
        photos = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_selected, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPhoto: ImageView = itemView.findViewById(R.id.imgPhoto)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(uri: Uri) {
            imgPhoto.setImageURI(uri) // Carga simple. Para producción usar Glide o Coil

            btnRemove.setOnClickListener {
                onDeleteClick(uri)
            }
        }
    }
}