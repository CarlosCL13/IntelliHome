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

    // Tu lista exacta copiada del documento PDF
    private val listaAmenidades = listOf(
        "Cocina equipada (con electrodomésticos modernos)", // [cite: 2]
        "Aire acondicionado", // [cite: 3]
        "Calefacción", // [cite: 4]
        "Wi-Fi gratuito", // [cite: 5]
        "Televisión por cable o satélite", // [cite: 6]
        "Lavadora y secadora", // [cite: 7]
        "Piscina", // [cite: 8]
        "Jardín o patio", // [cite: 9]
        "Barbacoa o parrilla", // [cite: 10]
        "Terraza o balcón", // [cite: 11]
        "Gimnasio en casa", // [cite: 12]
        "Garaje o espacio de estacionamiento", // [cite: 13]
        "Sistema de seguridad", // [cite: 14]
        "Habitaciones con baño en suite", // [cite: 15]
        "Muebles de exterior", // [cite: 16]
        "Microondas", // [cite: 17]
        "Lavavajillas", // [cite: 18]
        "Cafetera", // [cite: 19]
        "Ropa de cama y toallas incluidas", // [cite: 20]
        "Acceso a áreas comunes (piscina, gimnasio)", // [cite: 21]
        "Camas adicionales o sofá cama", // [cite: 22]
        "Servicios de limpieza opcionales", // [cite: 23]
        "Acceso a transporte público cercano", // [cite: 24]
        "Mascotas permitidas", // [cite: 25]
        "Cercanía a tiendas y restaurantes", // [cite: 26]
        "Sistema de calefacción por suelo radiante", // [cite: 27]
        "Escritorio o área de trabajo", // [cite: 28]
        "Sistemas de entretenimiento (videojuegos, etc.)", // [cite: 29]
        "Chimenea", //
        "Acceso a internet de alta velocidad" // [cite: 31]
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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