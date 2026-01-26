package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.SessionManager

class RentalsFragment : Fragment(R.layout.fragment_rentals) {

    private lateinit var adapter: PropertyAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var recycler: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recyclerRentals)
        emptyState = view.findViewById(R.id.emptyState)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Inicializamos el adapter con lista vacía para evitar errores
        adapter = PropertyAdapter(emptyList())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarMisAlquileres()
    }

    private fun cargarMisAlquileres() {
        // 1. Obtener ID del usuario actual
        val myUserId = SessionManager.obtenerUserId(requireContext())

        // 2. Obtener TODAS las propiedades
        val todasLasPropiedades = PropertyRepository.getProperties(requireContext())

        // 3. FILTRAR: Solo las que tengan 'rentedByUserId' igual a mi ID
        val misAlquileres = todasLasPropiedades.filter {
            it.rentedByUserId == myUserId
        }

        // 4. Actualizar UI
        if (misAlquileres.isEmpty()) {
            recycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            adapter.updateList(misAlquileres)
        }
    }
}