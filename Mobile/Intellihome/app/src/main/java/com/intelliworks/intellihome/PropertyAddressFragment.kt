package com.intelliworks.intellihome

import android.app.Activity
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.Locale

class PropertyAddressFragment : Fragment(R.layout.fragment_property_address), OnMapReadyCallback {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private var mMap: GoogleMap? = null
    private lateinit var etSearchAddress: EditText

    // Manejador del resultado de Google Places
    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val location = place.latLng

            if (location != null) {
                // Actualizamos mapa y ViewModel
                actualizarUbicacion(location, place.address ?: "Dirección desconocida")
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Log.e("PlacesError", status.statusMessage ?: "Error desconocido")
            Toast.makeText(requireContext(), "Error en Places: ${status.statusMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearchAddress = view.findViewById(R.id.etSearchAddress)

        // 1. Inicializar Places (¡Reemplaza con tu Key Real!)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBt8cN-5O3kQtcGIRD61VragAFNo58RqTc")
        }

        // 2. Cargar el Mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 3. Configurar clic en la barra de búsqueda
        etSearchAddress.setOnClickListener {
            // Especificamos los campos que queremos que nos devuelva Google
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("CR") // Opcional: Filtrar busquedas solo en Costa Rica
                .build(requireContext())
            startAutocomplete.launch(intent)
        }

        // 4. Botón Siguiente
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteAddress)

        // Observamos el ViewModel para activar el botón
        viewModel.esDireccionValida.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        // Si ya había datos guardados (al volver atrás), llenar el campo de texto
        viewModel.direccionTexto.value?.let {
            etSearchAddress.setText(it)
        }

        // En PropertyAddressFragment.kt
        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyDetailsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Coordenadas iniciales (Costa Rica aprox)
        // Si el ViewModel ya tiene una ubicación guardada, úsala, si no, usa default
        val latGuardada = viewModel.latitud.value
        val lonGuardada = viewModel.longitud.value

        val posicionInicial = if (latGuardada != null && lonGuardada != null) {
            LatLng(latGuardada, lonGuardada)
        } else {
            LatLng(9.934739, -84.087502) // San José, Costa Rica
        }

        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 15f))

        // LISTENER: Cuando el usuario deja de mover el mapa
        mMap?.setOnCameraIdleListener {
            val centro = mMap?.cameraPosition?.target
            if (centro != null) {
                // 1. Guardar coordenadas
                viewModel.latitud.value = centro.latitude
                viewModel.longitud.value = centro.longitude

                // 2. Obtener dirección a partir de las coordenadas (Geocoding Inverso)
                obtenerDireccionDeCoordenadas(centro)
            }
        }
    }

    private fun actualizarUbicacion(location: LatLng, address: String) {
        // Mover cámara
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

        // Actualizar UI y ViewModel
        etSearchAddress.setText(address)
        viewModel.direccionTexto.value = address
        viewModel.latitud.value = location.latitude
        viewModel.longitud.value = location.longitude
    }

    private fun obtenerDireccionDeCoordenadas(latLng: LatLng) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            // Nota: getFromLocation puede bloquear el hilo principal brevemente,
            // idealmente usar corrutinas, pero para este prototipo está bien así.
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Construimos una cadena de dirección legible
                val direccionCompleta = address.getAddressLine(0) // Ej: "Calle 5, San José..."

                etSearchAddress.setText(direccionCompleta)
                viewModel.direccionTexto.value = direccionCompleta
            }
        } catch (e: Exception) {
            Log.e("Geocoding", "Error obteniendo dirección", e)
        }
    }
}