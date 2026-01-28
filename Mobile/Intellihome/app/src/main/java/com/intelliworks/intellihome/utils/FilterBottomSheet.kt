package com.intelliworks.intellihome.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.intelliworks.intellihome.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class FilterBottomSheet(
    // Nuevos parámetros para los límites dinámicos
    private val absoluteMaxPrice: Float,
    private val absoluteMaxGuests: Float,
    // Valores seleccionados actualmente
    private val currentMaxPrice: Float,
    private val currentMinGuests: Float,
    // Callbacks
    private val onApply: (maxPrice: Float, minGuests: Float) -> Unit,
    private val onReset: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sliderPrice = view.findViewById<Slider>(R.id.sliderPrice)
        val sliderGuests = view.findViewById<Slider>(R.id.sliderGuests)
        val lblPrice = view.findViewById<TextView>(R.id.lblPrice)
        val lblGuests = view.findViewById<TextView>(R.id.lblGuests)
        val btnApply = view.findViewById<MaterialButton>(R.id.btnApply)
        val btnReset = view.findViewById<MaterialButton>(R.id.btnReset)

        // 1. CONFIGURAR LÍMITES BASADOS EN DATOS REALES
        sliderPrice.valueFrom = 0f
        sliderPrice.valueTo = if (absoluteMaxPrice > 0) absoluteMaxPrice else 100f

        sliderGuests.valueFrom = 1f
        sliderGuests.valueTo = if (absoluteMaxGuests > 1) absoluteMaxGuests else 5f

        // 2. ESTABLECER VALORES ACTUALES
        sliderPrice.value = currentMaxPrice.coerceIn(sliderPrice.valueFrom, sliderPrice.valueTo)
        sliderGuests.value = currentMinGuests.coerceIn(sliderGuests.valueFrom, sliderGuests.valueTo)

        // 3. TEXTOS INICIALES (Con traducción y formato del strings.xml)
        // Usamos getString con parámetros para reemplazar los %1$s y %1$d
        lblPrice.text = getString(R.string.fmt_max_price, "₡${sliderPrice.value.toInt()}")
        lblGuests.text = getString(R.string.fmt_min_guests, sliderGuests.value.toInt())

        // 4. LISTENERS DE CAMBIO (Actualizan el texto al mover el slider)
        sliderPrice.addOnChangeListener { _, value, _ ->
            lblPrice.text = getString(R.string.fmt_max_price, "₡${value.toInt()}")
        }

        sliderGuests.addOnChangeListener { _, value, _ ->
            lblGuests.text = getString(R.string.fmt_min_guests, value.toInt())
        }

        // 5. ACCIONES
        btnApply.setOnClickListener {
            onApply(sliderPrice.value, sliderGuests.value)
            dismiss()
        }

        btnReset.setOnClickListener {
            onReset()
            dismiss()
        }
    }
}