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

        // CARGAMOS LA LISTA AUTOMÁTICAMENTE (Español o Inglés)
        val listaAmenidades = resources.getStringArray(R.array.amenities_list).toList()

        val container = view.findViewById<LinearLayout>(R.id.containerAmenities)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteAmenities)

        // --- 1. Generación Dinámica de CheckBoxes ---
        // Recorremos la lista y creamos una vista para cada texto
        listaAmenidades.forEach { nombreAmenidad ->

            // Inflamos el diseño individual que creamos en el paso 1
            val checkBox = LayoutInflater.from(context)
                .inflate(R.layout.item_amenity_checkbox, container, false) as CheckBox

            checkBox.text = nombreAmenidad
            checkBox.tag = nombreAmenidad // Usamos el nombre como ID interno

            // Configurar Listener
            checkBox.setOnClickListener {
                viewModel.toggleComodidad(nombreAmenidad)
            }

            // Restaurar estado (si el usuario ya lo había marcado y volvió atrás)
            val seleccionadas = viewModel.comodidadesSeleccionadas.value ?: emptySet()
            checkBox.isChecked = seleccionadas.contains(nombreAmenidad)

            // Añadir al layout visual
            container.addView(checkBox)
        }

        // --- 2. Habilitar botón "Siguiente" ---
        // Opción A: Siempre habilitado (el usuario puede no seleccionar nada)
        btnSiguiente.isEnabled = true

        // Opción B: Si quieres obligar a elegir al menos 1, descomenta esto:
        /*
        viewModel.comodidadesSeleccionadas.observe(viewLifecycleOwner) { set ->
             btnSiguiente.isEnabled = set.isNotEmpty()
             btnSiguiente.alpha = if (set.isNotEmpty()) 1.0f else 0.5f
        }
        */

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyPhotosFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}