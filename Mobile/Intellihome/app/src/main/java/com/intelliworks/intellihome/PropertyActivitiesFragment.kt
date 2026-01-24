package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView

class PropertyActivitiesFragment : Fragment(R.layout.fragment_property_activities) {

    private val viewModel: AddPropertyViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mapa de tus tarjetas y su nombre clave
        val cards = mapOf(
            "HIKING" to view.findViewById<MaterialCardView>(R.id.cardHiking),
            "TV" to view.findViewById<MaterialCardView>(R.id.cardTV),
            "DEPORTES" to view.findViewById<MaterialCardView>(R.id.cardDeportes),
            "JUEGOS_MESA" to view.findViewById<MaterialCardView>(R.id.cardJuegosMesa),
            "ESNORQUEL" to view.findViewById<MaterialCardView>(R.id.cardEsnorquel)
        )

        // 1. Configurar Listeners
        cards.forEach { (nombre, card) ->
            card.setOnClickListener {
                viewModel.toggleActividad(nombre)
            }
        }

        // 2. Observar cambios para actualizar UI
        viewModel.actividadesSeleccionadas.observe(viewLifecycleOwner) { seleccionadas ->
            cards.forEach { (nombre, card) ->
                if (seleccionadas.contains(nombre)) {
                    marcarSeleccionada(card)
                } else {
                    marcarDesseleccionada(card)
                }
            }
        }

        // 3. Botón Siguiente
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteActivities)

        // IMPORTANTE: Asegúrate de que en tu ViewModel la variable se llame 'hayActividades'
        // Si pusiste 'hayActividadesValidas', cambia el nombre aquí abajo.
        viewModel.hayActividades.observe(viewLifecycleOwner) { haySeleccion ->
            btnSiguiente.isEnabled = haySeleccion
            btnSiguiente.alpha = if (haySeleccion) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            // Lógica para ir al siguiente paso (Dirección)
            // Asegúrate de crear PropertyAddressFragment primero para que no de error
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyAddressFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun marcarSeleccionada(card: MaterialCardView) {
        // Asegúrate de tener definidos estos colores en colors.xml o usa colores de sistema
        // R.color.card_selected_bg (ej: #E3F2FD - azul muy claro)
        // R.color.card_selected_stroke (ej: #2196F3 - azul normal)
        card.setCardBackgroundColor(requireContext().getColor(R.color.card_selected_bg))
        card.strokeColor = requireContext().getColor(R.color.card_selected_stroke)
        card.strokeWidth = 6 // Aumenté el grosor (en px) para que se note más
        card.cardElevation = 8f
    }

    private fun marcarDesseleccionada(card: MaterialCardView) {
        // CORRECCIÓN CRÍTICA: Usar WHITE para que coincida con el XML y no se vea feo el padding
        card.setCardBackgroundColor(requireContext().getColor(android.R.color.white))
        card.strokeColor = requireContext().getColor(android.R.color.darker_gray)
        card.strokeWidth = 2
        card.cardElevation = 4f
    }
}