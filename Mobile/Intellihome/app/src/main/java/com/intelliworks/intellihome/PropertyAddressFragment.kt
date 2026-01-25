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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.Locale

class PropertyAddressFragment : Fragment(R.layout.fragment_property_address), OnMapReadyCallback {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private var mMap: GoogleMap? = null
    private lateinit var etSearchAddress: EditText

    // Procesa el resultado de la actividad de autocompletado de Google Places
    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val location = place.latLng

            if (location != null) {
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

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        etSearchAddress.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("CR")
                .build(requireContext())
            startAutocomplete.launch(intent)
        }

        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteAddress)

        viewModel.esDireccionValida.observe(viewLifecycleOwner) { esValido ->
            btnSiguiente.isEnabled = esValido
            btnSiguiente.alpha = if (esValido) 1.0f else 0.5f
        }

        viewModel.direccionTexto.value?.let {
            etSearchAddress.setText(it)
        }

        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PropertyDetailsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val latGuardada = viewModel.latitud.value
        val lonGuardada = viewModel.longitud.value

        val posicionInicial = if (latGuardada != null && lonGuardada != null) {
            LatLng(latGuardada, lonGuardada)
        } else {
            LatLng(9.934739, -84.087502) // San José, Costa Rica (Default)
        }

        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 15f))

        // Al finalizar el movimiento del mapa, obtenemos las coordenadas centrales y realizamos Geocoding inverso
        mMap?.setOnCameraIdleListener {
            val centro = mMap?.cameraPosition?.target
            if (centro != null) {
                viewModel.latitud.value = centro.latitude
                viewModel.longitud.value = centro.longitude
                obtenerDireccionDeCoordenadas(centro)
            }
        }
    }

    private fun actualizarUbicacion(location: LatLng, address: String) {
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))

        etSearchAddress.setText(address)
        viewModel.direccionTexto.value = address
        viewModel.latitud.value = location.latitude
        viewModel.longitud.value = location.longitude
    }

    /**
     * Realiza Geocoding inverso para obtener una dirección legible a partir de coordenadas LatLng.
     * Utiliza el Geocoder nativo de Android.
     */
    private fun obtenerDireccionDeCoordenadas(latLng: LatLng) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            // Nota: getFromLocation es síncrono. Para implementaciones robustas usar corrutinas.
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val direccionCompleta = address.getAddressLine(0)

                etSearchAddress.setText(direccionCompleta)
                viewModel.direccionTexto.value = direccionCompleta
            }
        } catch (e: Exception) {
            Log.e("Geocoding", "Error obteniendo dirección", e)
        }
    }
}