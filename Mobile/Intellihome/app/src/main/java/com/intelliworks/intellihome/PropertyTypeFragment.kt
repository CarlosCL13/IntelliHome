package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.utils.PropertyTypeAdapter

class PropertyTypeFragment : Fragment(R.layout.fragment_property_type) {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private lateinit var adapter: PropertyTypeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerTipos)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguiente)

        // Configuración del RecyclerView
        adapter = PropertyTypeAdapter { idSeleccionado ->
            viewModel.setTipo(idSeleccionado)
        }
        // Usamos GridLayoutManager con 2 columnas para imitar tu diseño original
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        // 1. Cargar lista desde la BD
        viewModel.listaTipos.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
        }

        // 2. Restaurar selección previa (si el usuario regresa)
        viewModel.tipoSeleccionadoId.observe(viewLifecycleOwner) { id ->
            adapter.setSelected(id)
        }

        // 3. Habilitar botón "Siguiente"
        viewModel.esTipoValido.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyActivitiesFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}