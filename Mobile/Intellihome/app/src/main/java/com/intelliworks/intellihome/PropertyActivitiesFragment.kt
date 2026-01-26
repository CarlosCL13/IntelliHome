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

        val cards = mapOf(
            "HIKING" to view.findViewById<MaterialCardView>(R.id.cardHiking),
            "TV" to view.findViewById<MaterialCardView>(R.id.cardTV),
            "DEPORTES" to view.findViewById<MaterialCardView>(R.id.cardDeportes),
            "JUEGOS_MESA" to view.findViewById<MaterialCardView>(R.id.cardJuegosMesa),
            "ESNORQUEL" to view.findViewById<MaterialCardView>(R.id.cardEsnorquel)
        )

        cards.forEach { (nombre, card) ->
            card.setOnClickListener {
                viewModel.toggleActividad(nombre)
            }
        }

        // Actualiza el estado visual de las tarjetas según la selección en el ViewModel
        viewModel.actividadesSeleccionadas.observe(viewLifecycleOwner) { seleccionadas ->
            cards.forEach { (nombre, card) ->
                if (seleccionadas.contains(nombre)) {
                    marcarSeleccionada(card)
                } else {
                    marcarDesseleccionada(card)
                }
            }
        }

        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteActivities)

        viewModel.hayActividades.observe(viewLifecycleOwner) { haySeleccion ->
            btnSiguiente.isEnabled = haySeleccion
            btnSiguiente.alpha = if (haySeleccion) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyAddressFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun marcarSeleccionada(card: MaterialCardView) {
        card.setCardBackgroundColor(requireContext().getColor(R.color.card_selected_bg))
        card.strokeColor = requireContext().getColor(R.color.card_selected_stroke)
        card.strokeWidth = 6
        card.cardElevation = 8f
    }

    private fun marcarDesseleccionada(card: MaterialCardView) {
        card.setCardBackgroundColor(requireContext().getColor(android.R.color.white))
        card.strokeColor = requireContext().getColor(android.R.color.darker_gray)
        card.strokeWidth = 2
        card.cardElevation = 4f
    }
}