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

        val etTitulo = view.findViewById<EditText>(R.id.etTituloPropiedad)
        val etPrecio = view.findViewById<EditText>(R.id.etPrecioPropiedad)
        // NUEVO: Referencias a los campos de texto
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcionPropiedad)
        val etReglas = view.findViewById<EditText>(R.id.etReglasPropiedad)

        // Cargar valores existentes
        etTitulo.setText(viewModel.titulo.value)
        etPrecio.setText(viewModel.precio.value)
        etDescripcion.setText(viewModel.descripcion.value)
        etReglas.setText(viewModel.reglas.value)

        // Listeners para guardar cambios
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
        // NUEVO: Listeners para Descripción y Reglas
        etDescripcion.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.descripcion.value = s.toString()
            }
        })
        etReglas.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.reglas.value = s.toString()
            }
        })

        setupCounter(view.findViewById(R.id.rowHuespedes), getString(R.string.label_guests), viewModel.huespedes)
        setupCounter(view.findViewById(R.id.rowHabitaciones), getString(R.string.label_bedrooms), viewModel.habitaciones)
        setupCounter(view.findViewById(R.id.rowCamas), getString(R.string.label_beds), viewModel.camas)
        setupCounter(view.findViewById(R.id.rowBanos), getString(R.string.label_bathrooms), viewModel.banos)

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

    private fun setupCounter(rowView: View, label: String, liveData: MutableLiveData<Int>) {
        val txtLabel = rowView.findViewById<TextView>(R.id.txtLabel)
        val btnMinus = rowView.findViewById<ImageButton>(R.id.btnMinus)
        val btnPlus = rowView.findViewById<ImageButton>(R.id.btnPlus)
        val txtValue = rowView.findViewById<TextView>(R.id.txtValue)

        txtLabel.text = label

        liveData.observe(viewLifecycleOwner) { valor ->
            txtValue.text = valor.toString()
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

    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }
}