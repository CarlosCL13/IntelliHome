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

        // Carga la lista desde recursos para soportar internacionalización (strings.xml)
        val listaAmenidades = resources.getStringArray(R.array.amenities_list).toList()

        val container = view.findViewById<LinearLayout>(R.id.containerAmenities)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteAmenities)

        // Generación programática de CheckBoxes para evitar redundancia en XML
        listaAmenidades.forEach { nombreAmenidad ->
            val checkBox = LayoutInflater.from(context)
                .inflate(R.layout.item_amenity_checkbox, container, false) as CheckBox

            checkBox.text = nombreAmenidad
            checkBox.tag = nombreAmenidad

            checkBox.setOnClickListener {
                viewModel.toggleComodidad(nombreAmenidad)
            }

            val seleccionadas = viewModel.comodidadesSeleccionadas.value ?: emptySet()
            checkBox.isChecked = seleccionadas.contains(nombreAmenidad)

            container.addView(checkBox)
        }

        btnSiguiente.isEnabled = true

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyPhotosFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}