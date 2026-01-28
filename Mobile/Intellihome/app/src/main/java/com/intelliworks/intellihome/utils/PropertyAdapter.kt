package com.intelliworks.intellihome.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.intelliworks.intellihome.R
// Se asume que el archivo de diseño se llama item_property_card.xml
import com.intelliworks.intellihome.databinding.ItemPropertyCardBinding

/**
 * Adaptador para la lista de propiedades.
 * Vincula los datos con el diseño definido en item_property_card.xml.
 */
class PropertyAdapter(
    private var propertyList: List<Property>,
    private val onPropertyClick: ((Property) -> Unit)? = null
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    /**
     * ViewHolder que mantiene las referencias a las vistas usando ViewBinding.
     */
    inner class PropertyViewHolder(val binding: ItemPropertyCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = propertyList[position]

        // Uso de 'with' para simplificar el acceso a las vistas del binding
        with(holder.binding) {
            // Asignación de textos usando los IDs específicos del XML (txtTitle, txtAddress, etc.)
            txtTitle.text = property.titulo ?: "Sin título"
            txtAddress.text = property.direccion ?: "Ubicación desconocida"
            txtPrice.text = "₡${property.precio}"
            txtCapacity.text = formatCapacity(property.capacidad)

            // Carga de imagen principal de la propiedad
            if (property.imagenes.isNotEmpty()) {
                Glide.with(root.context)
                    .load(property.imagenes[0])
                    .centerCrop()
                    .into(imgProperty)
            }
            if (!property.fechaInicio.isNullOrEmpty() && !property.fechaFin.isNullOrEmpty()) {

                val context = root.context

                // Pasamos 'context' para que detecte si es Inglés o Español
                val inicio = PropertyUtils.formatearFechaLocal(context, property.fechaInicio)
                val fin = PropertyUtils.formatearFechaLocal(context, property.fechaFin)

                // El string R.string.fmt_rental_dates también cambia según el idioma
                txtRentDates.text = context.getString(R.string.fmt_rental_dates, inicio, fin)

                txtRentDates.visibility = android.view.View.VISIBLE
            } else {
                txtRentDates.visibility = android.view.View.GONE
            }

            // Configuración del evento de clic para navegación
            root.setOnClickListener {
                onPropertyClick?.invoke(property)
            }
        }
    }

    override fun getItemCount() = propertyList.size

    /**
     * Actualiza la lista de propiedades y notifica al adaptador.
     */
    fun updateList(newList: List<Property>) {
        propertyList = newList
        notifyDataSetChanged()
    }

    /**
     * Formatea la capacidad extrayendo el número de huéspedes del CSV.
     */
    private fun formatCapacity(capacity: String?): String {
        if (capacity.isNullOrEmpty()) return "N/A"
        return try {
            val parts = capacity.split(",")
            "${parts[0]} "
        } catch (e: Exception) {
            capacity
        }
    }
}