package com.intelliworks.intellihome

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView

class PropertyTypeFragment : Fragment(R.layout.fragment_property_type) {

    private val viewModel: AddPropertyViewModel by activityViewModels()

    private lateinit var cardContemporaneo: MaterialCardView
    private lateinit var cardMinimalista: MaterialCardView
    private lateinit var cardAventurero: MaterialCardView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        cardContemporaneo = view.findViewById(R.id.cardContemporaneo)
        cardMinimalista = view.findViewById(R.id.cardMinimalista)
        cardAventurero = view.findViewById(R.id.cardAventurero)

        cardContemporaneo.setOnClickListener {
            seleccionar(cardContemporaneo, "CONTEMPORANEO")
        }

        cardMinimalista.setOnClickListener {
            seleccionar(cardMinimalista, "MINIMALISTA")
        }

        cardAventurero.setOnClickListener {
            seleccionar(cardAventurero, "AVENTURERO")
        }

        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguiente)

        // Observar si se puede avanzar
        viewModel.esTipoValido.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            // Opcional: Cambiar alfa para que se vea "apagado"
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyActivitiesFragment())
                .addToBackStack(null) // Para poder volver atrás con el botón del cel
                .commit()
        }
    }

    private fun seleccionar(cardSeleccionada: MaterialCardView, tipo: String) {
        // 1️⃣ Resetear todas
        resetearCards()

        // 2️⃣ Marcar seleccionada
        cardSeleccionada.setCardBackgroundColor(
            requireContext().getColor(R.color.card_selected_bg)
        )
        cardSeleccionada.strokeColor =
            requireContext().getColor(R.color.card_selected_stroke)
        cardSeleccionada.strokeWidth = 3
        cardSeleccionada.cardElevation = 8f

        // 3️⃣ Guardar selección
        viewModel.tipoPropiedad.value = tipo
    }

    private fun resetearCards() {
        val cards = listOf(cardContemporaneo, cardMinimalista, cardAventurero)

        cards.forEach { card ->
            card.setCardBackgroundColor(
                requireContext().getColor(R.color.card_default_bg)
            )
            card.strokeColor =
                requireContext().getColor(R.color.card_default_stroke)
            card.strokeWidth = 1
            card.cardElevation = 4f
        }
    }
}
