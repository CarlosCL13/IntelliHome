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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Fragmento encargado de capturar los detalles básicos de la propiedad.
 * Incluye: Título, Precio, Descripción, Reglas, Contadores (habitaciones/huéspedes)
 * y la nueva funcionalidad de Disponibilidad Semanal (Días).
 */
class PropertyDetailsFragment : Fragment(R.layout.fragment_property_details) {

    // ViewModel compartido con la Activity para persistir datos entre fragmentos y cambios de configuración.
    private val viewModel: AddPropertyViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Vinculación de Vistas ---
        val etTitulo = view.findViewById<EditText>(R.id.etTituloPropiedad)
        val etPrecio = view.findViewById<EditText>(R.id.etPrecioPropiedad)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcionPropiedad)
        val etReglas = view.findViewById<EditText>(R.id.etReglasPropiedad)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteDetalles)

        // --- 2. Restauración de Estado ---
        // Al regresar a este fragmento (navegación atrás o rotación),
        // recuperamos lo que el usuario ya había escrito.
        etTitulo.setText(viewModel.titulo.value)
        etPrecio.setText(viewModel.precio.value)
        etDescripcion.setText(viewModel.descripcion.value)
        etReglas.setText(viewModel.reglas.value)

        // --- 3. Listeners de Texto (Data Binding manual) ---
        // Actualizan el ViewModel en tiempo real conforme el usuario escribe.

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

        // --- 4. Configuración de Contadores ---
        // Se configuran los botones +/- para huéspedes, habitaciones, camas y baños.
        setupCounter(view.findViewById(R.id.rowHuespedes), getString(R.string.label_guests), viewModel.huespedes)
        setupCounter(view.findViewById(R.id.rowHabitaciones), getString(R.string.label_bedrooms), viewModel.habitaciones)
        setupCounter(view.findViewById(R.id.rowCamas), getString(R.string.label_beds), viewModel.camas)
        setupCounter(view.findViewById(R.id.rowBanos), getString(R.string.label_bathrooms), viewModel.banos)

        // --- 5. Configuración de Disponibilidad (NUEVO) ---
        // Inicializa el ChipGroup para la selección múltiple de días de la semana.
        setupDiasDisponibilidad(view)

        // --- 6. Validación y Navegación ---
        // Observamos si todos los campos obligatorios están llenos para habilitar el botón.
        viewModel.sonDetallesValidos.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        btnSiguiente.setOnClickListener {
            // Navegación al siguiente paso (Amenidades)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyAmenitiesFragment())
                .addToBackStack(null) // Permite volver atrás con el botón físico
                .commit()
        }
    }

    /**
     * Configura la lógica de los Chips de días (Lunes, Martes...).
     * Maneja tanto la selección del usuario como la restauración visual desde el ViewModel.
     */
    private fun setupDiasDisponibilidad(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupDias)

        // A. Listener de Interacción: Cuando el usuario toca un chip
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                // 'tag' viene del XML (android:tag="Lunes", etc.)
                // Es crucial que el tag no sea null.
                val diaTag = buttonView.tag?.toString() ?: return@setOnCheckedChangeListener

                // Actualizamos el ViewModel con la nueva selección
                viewModel.toggleDia(diaTag, isChecked)
            }
        }

        // B. Observador de Estado: Sincronización UI <- ViewModel
        // Si el usuario vuelve de otra pantalla, esto asegura que los chips correctos estén marcados.
        viewModel.diasSeleccionados.observe(viewLifecycleOwner) { diasList ->
            // Recorremos todos los chips del grupo
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                val diaTag = chip.tag?.toString() ?: continue

                // Verificamos si este día está en la lista guardada
                val debeEstarSeleccionado = diasList.contains(diaTag)

                // Solo cambiamos el estado si es diferente para evitar disparar el listener innecesariamente
                if (chip.isChecked != debeEstarSeleccionado) {
                    chip.isChecked = debeEstarSeleccionado
                }
            }
        }
    }

    /**
     * Helper genérico para configurar las filas de contadores (Huéspedes, Camas, etc.).
     * @param rowView La vista 'include' que contiene los botones y textos.
     * @param label El texto a mostrar (ej: "Huéspedes").
     * @param liveData El MutableLiveData del ViewModel asociado a este contador.
     */
    private fun setupCounter(rowView: View, label: String, liveData: MutableLiveData<Int>) {
        val txtLabel = rowView.findViewById<TextView>(R.id.txtLabel)
        val btnMinus = rowView.findViewById<ImageButton>(R.id.btnMinus)
        val btnPlus = rowView.findViewById<ImageButton>(R.id.btnPlus)
        val txtValue = rowView.findViewById<TextView>(R.id.txtValue)

        txtLabel.text = label

        // Observamos el valor para actualizar el número y habilitar/deshabilitar el botón "-"
        liveData.observe(viewLifecycleOwner) { valor ->
            txtValue.text = valor.toString()

            // Deshabilitar botón de resta si es 0
            val esMayorCero = valor > 0
            btnMinus.isEnabled = esMayorCero
            btnMinus.alpha = if (esMayorCero) 1.0f else 0.3f
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

    /**
     * Clase abstracta para limpiar el código de los TextWatchers.
     * Nos permite implementar solo 'onTextChanged' y ignorar los otros dos métodos obligatorios.
     */
    open class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }
}