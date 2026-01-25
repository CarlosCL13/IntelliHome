package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Fragmento de historial de alquileres.
 * Actualmente funciona como placeholder para la lógica de reservas del cliente.
 */
class RentalsFragment : Fragment(R.layout.fragment_rentals) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuración inicial del RecyclerView (oculto por defecto en el XML)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerRentals)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // TODO: Implementar lógica de carga de reservas activas
    }
}