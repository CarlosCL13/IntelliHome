package com.intelliworks.intellihome

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PropertyPhotosFragment : Fragment(R.layout.fragment_property_photos) {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private lateinit var adapter: PhotosAdapter

    // Variables para manipular la UI
    private lateinit var txtContador: TextView
    private lateinit var btnTerminar: MaterialButton

    // Constantes
    private val MAX_SIZE_BYTES = 1024 * 1024  // 1MB
    private val MAX_PHOTOS = 10

    // Tipos permitidos
    private val ALLOWED_TYPES = listOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/svg+xml"
    )

    // Lanzador del Selector de Fotos
    // Nota: Configuramos el contrato con el límite máximo de fotos
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) {
            validarYAgregarFotos(uris)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar Vistas
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerPhotos)
        val btnAddPhotos = view.findViewById<MaterialCardView>(R.id.btnAddPhotos)
        btnTerminar = view.findViewById(R.id.btnTerminar)
        txtContador = view.findViewById(R.id.txtRequisitos)

        // 2. Configurar RecyclerView
        adapter = PhotosAdapter { uri -> viewModel.eliminarFoto(uri) }
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        // 3. Observar cambios en la lista de fotos
        viewModel.fotosSeleccionadas.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)

            // A) Activar/Desactivar botón Terminar
            val hayFotos = lista.isNotEmpty()
            btnTerminar.isEnabled = hayFotos
            btnTerminar.alpha = if (hayFotos) 1.0f else 0.5f

            // B) Actualizar texto del contador (Ej: "Requisitos... \n (3/10)")
            val textoBase = getString(R.string.text_photo_requirements)
            txtContador.text = "$textoBase\n(${lista.size}/$MAX_PHOTOS)"
        }

        // 4. Botón Agregar Fotos (Con validación de cupo ANTES de abrir galería)
        btnAddPhotos.setOnClickListener {
            val cantidadActual = viewModel.fotosSeleccionadas.value?.size ?: 0

            if (cantidadActual >= MAX_PHOTOS) {
                Toast.makeText(requireContext(), "Límite alcanzado (Máximo $MAX_PHOTOS fotos)", Toast.LENGTH_SHORT).show()
            } else {
                // Solo abrimos la galería si hay espacio
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        // 5. Botón Terminar
        btnTerminar.setOnClickListener {
            finalizarPublicacion()
        }
    }

    private fun validarYAgregarFotos(uris: List<Uri>) {
        // Cálculo de espacio disponible
        val fotosActuales = viewModel.fotosSeleccionadas.value ?: emptyList()
        val espacioDisponible = MAX_PHOTOS - fotosActuales.size

        if (espacioDisponible <= 0) return

        // Si el usuario seleccionó más fotos de las que caben, cortamos la lista
        val urisAProcesar = if (uris.size > espacioDisponible) {
            Toast.makeText(requireContext(), "Solo se agregaron las primeras $espacioDisponible fotos para no exceder el límite.", Toast.LENGTH_LONG).show()
            uris.take(espacioDisponible)
        } else {
            uris
        }

        val fotosValidas = mutableListOf<Uri>()
        val errores = mutableListOf<String>()

        // Validamos cada foto (Tipo y Peso)
        for (uri in urisAProcesar) {
            val validacion = verificarArchivo(uri)
            if (validacion.esValido) {
                fotosValidas.add(uri)
            } else {
                errores.add(validacion.mensajeError)
            }
        }

        // 1. Agregar las que sí pasaron al ViewModel
        if (fotosValidas.isNotEmpty()) {
            viewModel.agregarFotos(fotosValidas)
        }

        // 2. Mostrar errores si hubo alguno
        if (errores.isNotEmpty()) {
            val mensajeBase = getString(R.string.error_invalid_image)
            // Si es un solo error mostramos detalle, si son muchos mostramos resumen
            val detalle = if (errores.size == 1) errores[0] else "${errores.size} archivos inválidos"
            Toast.makeText(requireContext(), "$mensajeBase: $detalle", Toast.LENGTH_LONG).show()
        }
    }

    // Clase auxiliar simple para el resultado
    data class ResultadoValidacion(val esValido: Boolean, val mensajeError: String = "")

    private fun verificarArchivo(uri: Uri): ResultadoValidacion {
        val contentResolver = requireContext().contentResolver

        // A) Validar TIPO (MIME Type)
        val type = contentResolver.getType(uri)
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            return ResultadoValidacion(false, getString(R.string.error_format_not_allowed))
        }

        // B) Validar TAMAÑO
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val size = it.getLong(sizeIndex)
                    if (size > MAX_SIZE_BYTES) {
                        return ResultadoValidacion(false, getString(R.string.error_size_exceeded))
                    }
                }
            }
        }

        return ResultadoValidacion(true)
    }

    private fun finalizarPublicacion() {
        val fotos = viewModel.fotosSeleccionadas.value
        if (fotos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_no_photos), Toast.LENGTH_SHORT).show()
            return
        }

        val resumen = StringBuilder()

        // Resumen detallado usando Strings resources
        resumen.append("${getString(R.string.summary_type)} ${viewModel.tipoPropiedad.value ?: getString(R.string.text_not_defined)}\n\n")

        resumen.append("${getString(R.string.summary_title)} ${viewModel.titulo.value ?: getString(R.string.text_no_title)}\n")
        resumen.append("${getString(R.string.summary_price)} ${viewModel.precio.value ?: "0"}\n")

        resumen.append("${getString(R.string.summary_capacity)}\n")
        resumen.append("   ${getString(R.string.summary_guests)} ${viewModel.huespedes.value}\n")
        resumen.append("   ${getString(R.string.summary_rooms)} ${viewModel.habitaciones.value}\n")
        resumen.append("   ${getString(R.string.summary_beds)} ${viewModel.camas.value}\n")
        resumen.append("   ${getString(R.string.summary_baths)} ${viewModel.banos.value}\n\n")

        resumen.append("${getString(R.string.summary_location)}\n")
        resumen.append("   ${viewModel.direccionTexto.value ?: getString(R.string.text_no_address)}\n\n")

        val actividadesStr = viewModel.actividadesSeleccionadas.value?.joinToString(", ") ?: getString(R.string.text_none)
        resumen.append("${getString(R.string.summary_activities)} $actividadesStr\n\n")

        val amenidadesStr = viewModel.comodidadesSeleccionadas.value?.joinToString(", ") ?: getString(R.string.text_none)
        resumen.append("${getString(R.string.summary_amenities)} $amenidadesStr\n\n")

        resumen.append(getString(R.string.summary_photos_count, fotos.size))

        mostrarDialogoConfirmacion(resumen.toString())
    }

    private fun mostrarDialogoConfirmacion(mensaje: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_title_confirm))
            .setMessage(mensaje)
            .setPositiveButton(getString(R.string.btn_publish)) { dialog, _ ->
                enviarDatosReales()
            }
            .setNegativeButton(getString(R.string.btn_edit)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun enviarDatosReales() {
        Toast.makeText(requireContext(), getString(R.string.msg_publish_success), Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }
}