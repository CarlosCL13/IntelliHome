package com.intelliworks.intellihome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class PropertyAmenitiesFragment : Fragment(R.layout.fragment_property_amenities) {

    private val viewModel: AddPropertyViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.containerAmenities)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteAmenities)

        // 1. Observamos la lista REAL que viene de la base de datos (Python)
        viewModel.listaAmenidades.observe(viewLifecycleOwner) { lista ->
            container.removeAllViews() // Limpiamos la lista para evitar duplicados al volver

            lista.forEach { amenidad ->
                // Inflamos tu diseño de checkbox existente
                val checkBox = LayoutInflater.from(context)
                    .inflate(R.layout.item_amenity_checkbox, container, false) as CheckBox

                checkBox.text = amenidad.nombre

                // 2. Verificamos si este ID ya estaba seleccionado en el ViewModel
                val idsSeleccionados = viewModel.amenidadesSeleccionadasIds.value ?: emptySet()
                checkBox.isChecked = idsSeleccionados.contains(amenidad.id)

                // 3. Al hacer clic, guardamos el ID (no el nombre)
                checkBox.setOnClickListener {
                    viewModel.toggleAmenidad(amenidad.id)
                }

                container.addView(checkBox)
            }
        }

        // El botón siguiente siempre está habilitado en amenidades (suelen ser opcionales)
        btnSiguiente.isEnabled = true

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyPhotosFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}