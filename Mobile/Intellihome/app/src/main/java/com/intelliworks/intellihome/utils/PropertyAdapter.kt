package com.intelliworks.intellihome.utils

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.R

class PropertyAdapter(private var properties: List<Property>) :
    RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgProperty)
        val title: TextView = view.findViewById(R.id.txtTitle)
        val address: TextView = view.findViewById(R.id.txtAddress)
        val capacity: TextView = view.findViewById(R.id.txtCapacity)
        val price: TextView = view.findViewById(R.id.txtPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_card, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val item = properties[position]

        holder.title.text = item.titulo
        holder.address.text = item.direccion
        holder.capacity.text = item.capacidad
        holder.price.text = "$ ${item.precio}"

        if (item.imagenUri.isNotEmpty()) {
            try {
                holder.img.setImageURI(Uri.parse(item.imagenUri))
            } catch (e: Exception) {
                holder.img.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    override fun getItemCount() = properties.size

    fun updateList(newList: List<Property>) {
        properties = newList
        notifyDataSetChanged()
    }
}