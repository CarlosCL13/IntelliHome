package com.intelliworks.intellihome

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope // Importante para corrutinas
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.utils.PhotosAdapter
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PropertyPhotosFragment : Fragment(R.layout.fragment_property_photos) {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private lateinit var adapter: PhotosAdapter
    private lateinit var txtContador: TextView
    private lateinit var btnTerminar: MaterialButton

    private val MAX_PHOTOS = 10
    private val ALLOWED_TYPES = listOf(
        "image/jpeg", "image/png", "image/gif", "image/svg+xml"
    )

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) validarYAgregarFotos(uris)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerPhotos)
        val btnAddPhotos = view.findViewById<MaterialCardView>(R.id.btnAddPhotos)
        btnTerminar = view.findViewById(R.id.btnTerminar)
        txtContador = view.findViewById(R.id.txtRequisitos)

        adapter = PhotosAdapter { uri -> viewModel.eliminarFoto(uri) }
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        viewModel.fotosSeleccionadas.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
            val hayFotos = lista.isNotEmpty()
            btnTerminar.isEnabled = hayFotos
            btnTerminar.alpha = if (hayFotos) 1.0f else 0.5f
            txtContador.text = "${getString(R.string.text_photo_requirements)}\n(${lista.size}/$MAX_PHOTOS)"
        }

        btnAddPhotos.setOnClickListener {
            if ((viewModel.fotosSeleccionadas.value?.size ?: 0) >= MAX_PHOTOS) {
                Toast.makeText(requireContext(), getString(R.string.error_limit_reached), Toast.LENGTH_SHORT).show()
            } else {
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        btnTerminar.setOnClickListener { finalizarPublicacion() }
    }

    private fun validarYAgregarFotos(uris: List<Uri>) {
        val currentSize = viewModel.fotosSeleccionadas.value?.size ?: 0
        val space = MAX_PHOTOS - currentSize
        if (space > 0) viewModel.agregarFotos(uris.take(space))
    }

    private fun verificarArchivo(uri: Uri): ResultadoValidacion {
        val contentResolver = requireContext().contentResolver
        val type = contentResolver.getType(uri)
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            return ResultadoValidacion(false, getString(R.string.error_format_not_allowed))
        }
        return ResultadoValidacion(true)
    }

    data class ResultadoValidacion(val esValido: Boolean, val mensajeError: String = "")

    private fun finalizarPublicacion() {
        val fotos = viewModel.fotosSeleccionadas.value
        if (fotos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_no_photos), Toast.LENGTH_SHORT).show()
            return
        }
        val resumen = construirResumen()
        mostrarDialogoConfirmacion(resumen)
    }

    /**
     * CORREGIDO: Ahora usa los helpers del ViewModel para obtener los nombres reales
     * en lugar de acceder a variables antiguas que ya no existen.
     */
    private fun construirResumen(): String {
        val sb = StringBuilder()

        // 1. Datos Básicos (Usamos el helper getNombreTipoSeleccionado)
        val tipo = viewModel.getNombreTipoSeleccionado()
        val titulo = viewModel.titulo.value ?: getString(R.string.text_no_title)
        val precio = viewModel.precio.value ?: "0"

        sb.append("${getString(R.string.summary_type)} $tipo\n")
        sb.append("${getString(R.string.summary_title)} $titulo\n")
        sb.append("${getString(R.string.summary_price)} $precio\n\n")

        // 2. Capacidad
        val nHuespedes = viewModel.huespedes.value ?: 0
        sb.append("${getString(R.string.summary_capacity)}: $nHuespedes huéspedes\n\n")

        // 3. Ubicación
        val ubicacion = viewModel.direccionTexto.value ?: getString(R.string.text_no_address)
        sb.append("${getString(R.string.summary_location)} $ubicacion\n\n")

        // 4. Actividades (Usamos helper)
        val actividadesStr = viewModel.getNombresHobbiesSeleccionados()
        val finalAct = if (actividadesStr.isNotEmpty()) actividadesStr else getString(R.string.text_none)
        sb.append("${getString(R.string.summary_activities)} $finalAct\n\n")

        // 5. Comodidades (Usamos helper)
        val comodidadesStr = viewModel.getNombresAmenidadesSeleccionadas()
        val finalCom = if (comodidadesStr.isNotEmpty()) comodidadesStr else getString(R.string.text_none)
        sb.append("${getString(R.string.summary_amenities)} $finalCom\n\n")

        return sb.toString()
    }

    private fun mostrarDialogoConfirmacion(mensaje: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_title_confirm))
            .setMessage(mensaje)
            .setPositiveButton(getString(R.string.btn_publish)) { _, _ -> enviarDatosReales() }
            .setNegativeButton(getString(R.string.btn_edit)) { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun enviarDatosReales() {
        btnTerminar.isEnabled = false // Evitar doble clic
        val context = requireContext()
        val userId = SessionManager.obtenerUserId(context).toIntOrNull() ?: 0

        // --- RECOLECCIÓN DE DATOS (Usando IDs) ---
        // Aquí usamos los IDs que el ViewModel guardó al seleccionar los Chips

        val tipoCasaId = viewModel.tipoSeleccionadoId.value ?: 1 // Default 1 si falla
        val hobbiesIds = viewModel.hobbiesSeleccionadosIds.value?.toList() ?: emptyList()
        val amenidadesIds = viewModel.amenidadesSeleccionadasIds.value?.toList() ?: emptyList()

        // Resto de datos
        val titulo = viewModel.titulo.value ?: "Sin título"
        val descripcion = viewModel.descripcion.value ?: ""
        val precio = (viewModel.precio.value ?: "0").toDoubleOrNull() ?: 0.0
        val huespedes = viewModel.huespedes.value ?: 0
        val habitaciones = viewModel.habitaciones.value ?: 0
        val camas = viewModel.camas.value ?: 0
        val banos = viewModel.banos.value ?: 0
        val reglas = viewModel.reglas.value ?: ""
        val latitud = viewModel.latitud.value ?: 0.0
        val longitud = viewModel.longitud.value ?: 0.0

        val api = RetrofitInstance.retrofit.create(PropiedadApi::class.java)
        val repo = PropiedadRepository(api) // Asegúrate de importar tu repo correcto

        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Publicando...", Toast.LENGTH_SHORT).show()

                val response = repo.registrarPropiedad(
                    context = context,
                    usuarioId = userId,
                    tipoCasaId = tipoCasaId,
                    hobbiesIds = hobbiesIds,
                    amenidadesIds = amenidadesIds,
                    latitud = latitud,
                    longitud = longitud,
                    titulo = titulo,
                    descripcion = descripcion,
                    precio = precio,
                    huespedes = huespedes,
                    habitaciones = habitaciones,
                    camas = camas,
                    banos = banos,
                    cocina = true, // O lógica si cocina está en amenidades
                    reglas = reglas,
                    vehiculos = 1,
                    fotosUris = viewModel.fotosSeleccionadas.value ?: emptyList()
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, getString(R.string.msg_publish_success), Toast.LENGTH_LONG).show()
                    requireActivity().finish()
                } else {
                    val errorStr = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(context, "Error: $errorStr", Toast.LENGTH_LONG).show()
                    btnTerminar.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                btnTerminar.isEnabled = true
            }
        }
    }

    // El método copiarImagenAInternalStorage ya no se llama directamente aquí,
    // lo maneja el repositorio (PropiedadRepository) usando FileUtils.
}