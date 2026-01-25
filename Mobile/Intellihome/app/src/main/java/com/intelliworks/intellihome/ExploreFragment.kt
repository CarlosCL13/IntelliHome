package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.PropertyAdapter

/**
 * Fragmento de exploración (Home).
 * Recupera y muestra todas las propiedades persistidas en el repositorio local.
 */
class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var adapter: PropertyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerExplore)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Obtiene la lista completa sin filtros
        val listaGlobal = PropertyRepository.getProperties(requireContext())
        adapter = PropertyAdapter(listaGlobal)
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Refresca la lista al volver a esta pantalla
        val listaActualizada = PropertyRepository.getProperties(requireContext())
        if (::adapter.isInitialized) {
            adapter.updateList(listaActualizada)
        }
    }
}