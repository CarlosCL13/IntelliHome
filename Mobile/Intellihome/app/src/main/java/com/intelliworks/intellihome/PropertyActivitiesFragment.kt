package com.intelliworks.intellihome

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.utils.ActivitiesAdapter

class PropertyActivitiesFragment : Fragment(R.layout.fragment_property_activities) {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private lateinit var adapter: ActivitiesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerActivities)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteActivities)

        // Configurar RecyclerView (Grid de 2 columnas)
        adapter = ActivitiesAdapter(
            onToggle = { id -> viewModel.toggleHobby(id) },
            isSelected = { id ->
                viewModel.hobbiesSeleccionadosIds.value?.contains(id) == true
            }
        )
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        // 1. Cargar lista de Hobbies (Datos del servidor)
        viewModel.listaHobbies.observe(viewLifecycleOwner) { lista ->
            val listaTraducida = lista.map { item ->
                // Usamos la traducción basada en ID
                item.copy(nombre = obtenerNombreHobby(item.id, item.nombre))
            }
            adapter.submitList(listaTraducida)
        }

        // 2. Observar selecciones para actualizar botón y visuales
        viewModel.hobbiesSeleccionadosIds.observe(viewLifecycleOwner) { ids ->
            // Refrescamos la lista para que se pinten los bordes seleccionados
            // (Nota: notifyDataSetChanged es aceptable aquí porque la lista es pequeña)
            adapter.notifyDataSetChanged()

            val haySeleccion = ids.isNotEmpty()
            btnSiguiente.isEnabled = haySeleccion
            btnSiguiente.alpha = if (haySeleccion) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyAddressFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    private fun obtenerNombreHobby(id: Int, original: String): String {
        return when (id) {
            1 -> getString(R.string.hobby_tv)           // Ver TV
            2 -> getString(R.string.hobby_hiking)       // Hiking
            3 -> getString(R.string.hobby_boardgames)   // Juegos de mesa
            4 -> getString(R.string.hobby_snorkel)   // Esnórquel
            5 -> getString(R.string.hobby_sports)       // Deportes
            else -> original
        }
    }
}