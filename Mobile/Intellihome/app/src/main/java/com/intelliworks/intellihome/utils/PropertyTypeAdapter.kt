package com.intelliworks.intellihome.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.intelliworks.intellihome.R
import com.intelliworks.intellihome.data.model.TipoCasaDto

/**
 * Adaptador para manejar la selección de "Tipo de Propiedad" en un RecyclerView.
 *
 * Implementa una lógica de **Selección Única** (Single Selection):
 * Al seleccionar un elemento, automáticamente se deselecciona el anterior.
 *
 * @property items Lista inicial de tipos de casa.
 * @property onSelect Callback que notifica al fragmento/ViewModel qué ID fue seleccionado.
 */
class PropertyTypeAdapter(
    private var items: List<TipoCasaDto> = emptyList(),
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<PropertyTypeAdapter.ViewHolder>() {

    // Almacena el ID del elemento actualmente seleccionado. Null si no hay selección.
    private var selectedId: Int? = null

    /**
     * Actualiza la lista de datos mostrada en el adaptador.
     * Se usa cuando llegan los datos desde la base de datos.
     */
    fun submitList(newItems: List<TipoCasaDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Restaura el estado de selección visual.
     * Útil si el usuario navega atrás desde otra pantalla y queremos mostrar
     * lo que había elegido previamente.
     */
    fun setSelected(id: Int?) {
        selectedId = id
        notifyDataSetChanged()
    }

    /**
     * Mantiene referencias a las vistas para optimizar el rendimiento.
     *
     * CORRECCIÓN IMPORTANTE:
     * Como el elemento raíz de 'item_type_card.xml' es el propio MaterialCardView,
     * no usamos 'findViewById' para buscarlo, sino que hacemos un cast directo de 'view'.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView // Cast directo para evitar NullPointerException
        val img: ImageView = view.findViewById(R.id.imgIcono)
        val txt: TextView = view.findViewById(R.id.txtNombre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_type_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 1. Asignar textos e imágenes dinámicas
        holder.txt.text = item.nombre
        holder.img.setImageResource(obtenerIcono(item.nombre))

        // 2. Determinar si este ítem específico es el seleccionado
        val isSelected = (item.id == selectedId)

        // 3. Actualizar la apariencia visual (bordes, colores) según el estado
        actualizarEstilo(holder.card, isSelected)

        // 4. Manejar el clic del usuario
        holder.itemView.setOnClickListener {
            selectedId = item.id // Actualizamos la selección interna
            onSelect(item.id)    // Notificamos al exterior

            // Refrescamos toda la lista para eliminar el borde del ítem anterior
            // y poner el borde al nuevo.
            notifyDataSetChanged()
        }
    }

    /**
     * Aplica los cambios visuales de estado (Seleccionado vs Normal).
     */
    private fun actualizarEstilo(card: MaterialCardView, isSelected: Boolean) {
        val context = card.context
        if (isSelected) {
            // Estilo Seleccionado: Fondo azul claro, borde negro, mayor elevación
            card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            card.strokeColor = context.getColor(R.color.black)
            card.strokeWidth = 6
            card.cardElevation = 12f
        } else {
            // Estilo Normal: Fondo blanco, borde gris, elevación estándar
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeColor = Color.DKGRAY
            card.strokeWidth = 2
            card.cardElevation = 4f
        }
    }

    /**
     * Mapea el nombre que viene de la base de datos a un recurso Drawable local.
     * Utiliza normalización de texto (trim/uppercase) y búsqueda parcial (contains)
     * para asegurar que coincida aunque haya pequeñas diferencias (mayúsculas, acentos).
     */
    private fun obtenerIcono(nombre: String): Int {
        val nombreLimpio = nombre.trim().uppercase()

        // Log para depuración: Descomentar si las imágenes no cargan
        android.util.Log.d("ICONOS", "Tipo recibido: '$nombreLimpio'")

        return when {
            // Grupo: Contemporáneo
            nombreLimpio.contains("CONTEMPOR") -> R.drawable.contemporaneo

            // Grupo: Minimalista
            nombreLimpio.contains("MINIMAL") -> R.drawable.minimalista

            // Grupo: Aventurero
            nombreLimpio.contains("AVENTUR") || nombreLimpio.contains("ADVENTUR") -> R.drawable.aventurero

            // Fallback: Icono por defecto
            else -> R.drawable.ic_launcher_foreground
        }
    }

    override fun getItemCount() = items.size
}