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
            container.removeAllViews()

            lista.forEach { amenidad ->
                val checkBox = LayoutInflater.from(context)
                    .inflate(R.layout.item_amenity_checkbox, container, false) as CheckBox

                // CAMBIO AQUÍ: Usamos la función traductora en lugar de amenidad.nombre directo
                checkBox.text = obtenerNombreAmenidad(amenidad.id, amenidad.nombre)

                val idsSeleccionados = viewModel.amenidadesSeleccionadasIds.value ?: emptySet()
                checkBox.isChecked = idsSeleccionados.contains(amenidad.id)

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
    /**
     * Mapea IDs de Amenidades a Strings.xml
     */
    private fun obtenerNombreAmenidad(id: Int, original: String): String {
        return when (id) {
            1 -> getString(R.string.am_kitchen)
            2 -> getString(R.string.am_ac)
            3 -> getString(R.string.am_heating)
            4 -> getString(R.string.am_wifi)
            5 -> getString(R.string.am_cable_tv)
            6 -> getString(R.string.am_washer_dryer)
            7 -> getString(R.string.am_pool)
            8 -> getString(R.string.am_garden)
            9 -> getString(R.string.am_bbq)
            10 -> getString(R.string.am_balcony)
            11 -> getString(R.string.am_gym)
            12 -> getString(R.string.am_parking)
            13 -> getString(R.string.am_security)
            14 -> getString(R.string.am_ensuite)
            15 -> getString(R.string.am_outdoor_furniture)
            16 -> getString(R.string.am_microwave)
            17 -> getString(R.string.am_dishwasher)
            18 -> getString(R.string.am_coffee_maker)
            19 -> getString(R.string.am_linens)
            20 -> getString(R.string.am_common_areas)
            21 -> getString(R.string.am_extra_beds)
            22 -> getString(R.string.am_cleaning)
            23 -> getString(R.string.am_public_transport)
            24 -> getString(R.string.am_pets)
            25 -> getString(R.string.am_shops)
            26 -> getString(R.string.am_floor_heating)
            27 -> getString(R.string.am_workspace)
            28 -> getString(R.string.am_entertainment)
            29 -> getString(R.string.am_fireplace)
            30 -> getString(R.string.am_internet_high_speed)
            else -> original
        }
    }
}