package com.intelliworks.intellihome.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.intelliworks.intellihome.R
import com.intelliworks.intellihome.data.model.HobbyDto

class ActivitiesAdapter(
    private val onToggle: (Int) -> Unit, // Callback al hacer click
    private val isSelected: (Int) -> Boolean // Función para saber si está seleccionado
) : ListAdapter<HobbyDto, ActivitiesAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardContainer)
        val img: ImageView = view.findViewById(R.id.imgIcono)
        val txt: TextView = view.findViewById(R.id.txtNombre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.txt.text = item.nombre
        holder.img.setImageResource(obtenerIcono(item.nombre))

        val seleccionado = isSelected(item.id)
        actualizarEstilo(holder.card, seleccionado)

        holder.itemView.setOnClickListener {
            onToggle(item.id)
            // Actualizamos visualmente al instante
            notifyItemChanged(position)
        }
    }

    private fun actualizarEstilo(card: MaterialCardView, seleccionado: Boolean) {
        val context = card.context
        if (seleccionado) {
            // Estilo Seleccionado (Borde azul/color primario, fondo gris claro, más elevación)
            card.setCardBackgroundColor(Color.parseColor("#E3F2FD")) // Azul muy claro
            card.strokeColor = context.getColor(R.color.black) // O tu color primario
            card.strokeWidth = 6
            card.cardElevation = 12f
        } else {
            // Estilo Normal
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeColor = Color.DKGRAY
            card.strokeWidth = 2
            card.cardElevation = 4f
        }
    }

    // --- MAPEO DE ICONOS ---
    private fun obtenerIcono(nombre: String): Int {
        val nombreLimpio = nombre.trim().uppercase()

        // --- DEPURACIÓN ---
        // Si tienes dudas, descomenta esta línea para ver en el Logcat qué nombres llegan
        // android.util.Log.e("ICONOS_DEBUG", "Recibido: '$nombreLimpio'")

        return when {
            // SENDERISMO
            nombreLimpio.contains("SENDER") || nombreLimpio.contains("HIKING") -> R.drawable.hiking

            // TV
            nombreLimpio.contains("TV") || nombreLimpio.contains("TELE") -> R.drawable.tv

            // DEPORTES
            nombreLimpio.contains("DEPORT") || nombreLimpio.contains("SPORT") -> R.drawable.deporte

            // ESNÓRQUEL (Agregamos variaciones comunes)
            nombreLimpio.contains("NORQUEL") ||
                    nombreLimpio.contains("SNORKEL") ||
                    nombreLimpio.contains("ESN") ||       // Por si llega "Esnorquel" o "Esnórquel"
                    nombreLimpio.contains("BUCEO") ||     // A veces se confunde con buceo
                    nombreLimpio.contains("MAR") ||       // Por si es "Actividad de Mar"
                    nombreLimpio.contains("ACUA") -> R.drawable.norquel

            // JUEGOS DE MESA
            nombreLimpio.contains("JUEGO") || nombreLimpio.contains("MESA") || nombreLimpio.contains("BOARD") -> R.drawable.juegosmesa

            else -> {
                // TRUCO: Si no encontramos icono, imprimimos en consola qué nombre falló
                android.util.Log.e("ICONOS_ERROR", "No hay icono para: $nombreLimpio")
                R.drawable.ic_launcher_foreground
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HobbyDto>() {
        override fun areItemsTheSame(oldItem: HobbyDto, newItem: HobbyDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HobbyDto, newItem: HobbyDto) = oldItem == newItem
    }
}