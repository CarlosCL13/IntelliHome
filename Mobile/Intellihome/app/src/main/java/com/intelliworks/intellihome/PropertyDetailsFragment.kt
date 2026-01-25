package com.intelliworks.intellihome

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData

class PropertyDetailsFragment : Fragment(R.layout.fragment_property_details) {

    private val viewModel: AddPropertyViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Vincular Inputs de Texto (Título y Precio)
        val etTitulo = view.findViewById<EditText>(R.id.etTituloPropiedad)
        val etPrecio = view.findViewById<EditText>(R.id.etPrecioPropiedad)

        // Cargar datos previos si existen
        etTitulo.setText(viewModel.titulo.value)
        etPrecio.setText(viewModel.precio.value)

        // Listeners para guardar en ViewModel al escribir
        etTitulo.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.titulo.value = s.toString()
            }
        })
        etPrecio.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.precio.value = s.toString()
            }
        })

        // 2. Configurar Contadores (Usando el ID del include)
        setupCounter(view.findViewById(R.id.rowHuespedes), "Huéspedes", viewModel.huespedes)
        setupCounter(view.findViewById(R.id.rowHabitaciones), "Habitaciones", viewModel.habitaciones)
        setupCounter(view.findViewById(R.id.rowCamas), "Camas", viewModel.camas)
        setupCounter(view.findViewById(R.id.rowBanos), "Baños", viewModel.banos)

        // 3. Botón Siguiente
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteDetalles)
        viewModel.sonDetallesValidos.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyAmenitiesFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // Función auxiliar para configurar cada fila de contador sin repetir código
    private fun setupCounter(rowView: View, label: String, liveData: MutableLiveData<Int>) {
        val txtLabel = rowView.findViewById<TextView>(R.id.txtLabel)
        val btnMinus = rowView.findViewById<ImageButton>(R.id.btnMinus)
        val btnPlus = rowView.findViewById<ImageButton>(R.id.btnPlus)
        val txtValue = rowView.findViewById<TextView>(R.id.txtValue)

        txtLabel.text = label

        // Observar cambios para actualizar el número
        liveData.observe(viewLifecycleOwner) { valor ->
            txtValue.text = valor.toString()
            // Deshabilitar botón menos si es 0 (o 1 según lógica)
            btnMinus.isEnabled = valor > 0
            btnMinus.alpha = if (valor > 0) 1.0f else 0.3f
        }

        btnPlus.setOnClickListener {
            val actual = liveData.value ?: 0
            liveData.value = actual + 1
        }

        btnMinus.setOnClickListener {
            val actual = liveData.value ?: 0
            if (actual > 0) {
                liveData.value = actual - 1
            }
        }
    }

    // Clase auxiliar pequeña para no ensuciar el código con TextWatchers vacíos
    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }


}