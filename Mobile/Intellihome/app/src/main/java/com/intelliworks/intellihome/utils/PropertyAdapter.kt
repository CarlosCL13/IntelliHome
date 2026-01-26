package com.intelliworks.intellihome.utils

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // <--- IMPORTANTE
import com.google.gson.Gson
import com.intelliworks.intellihome.R
import com.intelliworks.intellihome.RentPropertyActivity
import java.text.NumberFormat
import java.util.Locale

class PropertyAdapter(private var propertyList: List<Property>) :
    RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProperty: ImageView = view.findViewById(R.id.imgProperty)
        val txtTitle: TextView = view.findViewById(R.id.txtTitle)
        val txtAddress: TextView = view.findViewById(R.id.txtAddress)
        val txtCapacity: TextView = view.findViewById(R.id.txtCapacity)
        val txtPrice: TextView = view.findViewById(R.id.txtPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_card, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val currentProperty = propertyList[position]
        val context = holder.itemView.context

        holder.txtTitle.text = currentProperty.titulo
        holder.txtAddress.text = currentProperty.direccion
        holder.txtCapacity.text = PropertyUtils.getFormattedCapacity(context, currentProperty.capacidad)

        val precioDouble = currentProperty.precio.replace(",", "").toDoubleOrNull() ?: 0.0
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        holder.txtPrice.text = currencyFormat.format(precioDouble)

        // --- CORRECCIÓN: CARGA DE IMÁGENES WEB CON GLIDE ---
        if (currentProperty.imagenes.isNotEmpty()) {
            val urlImagen = currentProperty.imagenes[0]
            // Glide se encarga de descargarla y ponerla, o poner un error si falla
            Glide.with(context)
                .load(urlImagen)
                .placeholder(android.R.drawable.ic_menu_gallery) // Mientras carga
                .error(android.R.drawable.ic_delete) // Si falla
                .centerCrop()
                .into(holder.imgProperty)
        } else {
            holder.imgProperty.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RentPropertyActivity::class.java)
            // Pasamos la propiedad básica (ID, titulo, precio)
            // La pantalla de detalles se encargará de descargar el resto de la info (descripción, host, etc.)
            val gson = Gson()
            val propertyJson = gson.toJson(currentProperty)
            intent.putExtra("property_data", propertyJson)

            val currentUserId = SessionManager.obtenerUserId(context)
            val isRentalOrOwnerActive = (currentProperty.rentedByUserId == currentUserId) || (currentProperty.userId == currentUserId)
            intent.putExtra("is_rental_active", isRentalOrOwnerActive)

            context.startActivity(intent)
        }
    }

    override fun getItemCount() = propertyList.size

    fun updateList(newList: List<Property>) {
        propertyList = newList
        notifyDataSetChanged()
    }
}