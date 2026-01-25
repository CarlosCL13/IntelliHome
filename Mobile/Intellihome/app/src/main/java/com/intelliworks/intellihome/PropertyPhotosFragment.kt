package com.intelliworks.intellihome

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
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

    // Constante: 1MB en bytes
    private val MAX_SIZE_BYTES = 1024 * 1024

    // Tipos permitidos (MIME types)
    private val ALLOWED_TYPES = listOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/svg+xml"
    )

    // Lanzador del Selector de Fotos
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            validarYAgregarFotos(uris)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerPhotos)
        adapter = PhotosAdapter { uri -> viewModel.eliminarFoto(uri) }
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        viewModel.fotosSeleccionadas.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
        }

        view.findViewById<MaterialCardView>(R.id.btnAddPhotos).setOnClickListener {
            // Solicitamos imágenes (el filtro de tipo se hace manual después para ser estricto con SVG/GIF)
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        view.findViewById<MaterialButton>(R.id.btnTerminar).setOnClickListener {
            finalizarPublicacion()
        }
    }
    private fun validarYAgregarFotos(uris: List<Uri>) {
        val fotosValidas = mutableListOf<Uri>()
        val errores = mutableListOf<String>()

        for (uri in uris) {
            val validacion = verificarArchivo(uri)
            if (validacion.esValido) {
                fotosValidas.add(uri)
            } else {
                errores.add(validacion.mensajeError)
            }
        }

        // 1. Agregar las que sí pasaron
        if (fotosValidas.isNotEmpty()) {
            viewModel.agregarFotos(fotosValidas)
        }

        // 2. Mostrar errores si hubo alguno
        if (errores.isNotEmpty()) {
            // Mostramos el primer error o un resumen
            val mensaje = if (errores.size == 1) {
                "Imagen inválida: ${errores[0]}"
            } else {
                "${errores.size} imágenes inválidas. Verifique tamaño (1MB) y formato."
            }
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
        }
    }

    // Clase auxiliar simple para el resultado
    data class ResultadoValidacion(val esValido: Boolean, val mensajeError: String = "")

    private fun verificarArchivo(uri: Uri): ResultadoValidacion {
        val contentResolver = requireContext().contentResolver

        // A) Validar TIPO (MIME Type)
        val type = contentResolver.getType(uri)
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            return ResultadoValidacion(false, "Formato no permitido ($type). Use JPG, PNG, GIF o SVG.")
        }

        // B) Validar TAMAÑO
        // Usamos un cursor para leer los metadatos sin abrir el archivo entero
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                // Si no puede leer el tamaño, asumimos que es inválido por seguridad o lo dejamos pasar (depende de tu regla)
                if (sizeIndex != -1) {
                    val size = it.getLong(sizeIndex)
                    if (size > MAX_SIZE_BYTES) {
                        return ResultadoValidacion(false, "El archivo excede 1MB (${size / 1024} KB)")
                    }
                }
            }
        }

        return ResultadoValidacion(true)
    }

    private fun finalizarPublicacion() {
        // 1. Verificar si hay fotos (Validación básica)
        val fotos = viewModel.fotosSeleccionadas.value
        if (fotos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Debes agregar al menos una foto.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. RECOPILAR TODOS LOS DATOS EN UNA CADENA DE TEXTO (String)
        val resumen = StringBuilder()

        // -- Datos Básicos --
        resumen.append("🏠 TIPO: ${viewModel.tipoPropiedad.value ?: "No definido"}\n\n")

        // -- Detalles --
        resumen.append("📝 TÍTULO: ${viewModel.titulo.value ?: "Sin título"}\n")
        resumen.append("💲 PRECIO: $${viewModel.precio.value ?: "0"}\n")

        // -- Capacidad (Contadores) --
        resumen.append("👥 CAPACIDAD:\n")
        resumen.append("   - Huéspedes: ${viewModel.huespedes.value}\n")
        resumen.append("   - Habitaciones: ${viewModel.habitaciones.value}\n")
        resumen.append("   - Camas: ${viewModel.camas.value}\n")
        resumen.append("   - Baños: ${viewModel.banos.value}\n\n")

        // -- Ubicación --
        resumen.append("📍 UBICACIÓN:\n")
        resumen.append("   ${viewModel.direccionTexto.value ?: "Sin dirección"}\n")
        resumen.append("   (Lat: ${viewModel.latitud.value}, Lon: ${viewModel.longitud.value})\n\n")

        // -- Listas (Actividades y Amenidades) --
        // Usamos joinToString para que se vean bonitas separadas por comas
        val actividadesStr = viewModel.actividadesSeleccionadas.value?.joinToString(", ") ?: "Ninguna"
        resumen.append("🏃 ACTIVIDADES: $actividadesStr\n\n")

        val amenidadesStr = viewModel.comodidadesSeleccionadas.value?.joinToString(", ") ?: "Ninguna"
        resumen.append("✨ COMODIDADES: $amenidadesStr\n\n")

        // -- Fotos --
        resumen.append("📸 FOTOS: Se subirán ${fotos.size} imágenes validada(s).")

        // 3. MOSTRAR EL DIÁLOGO DE CONFIRMACIÓN
        mostrarDialogoConfirmacion(resumen.toString())
    }

    private fun mostrarDialogoConfirmacion(mensaje: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Publicación")
            .setMessage(mensaje) // Aquí va todo el texto que construimos arriba
            .setPositiveButton("PUBLICAR") { dialog, _ ->
                // Aquí iría la llamada REAL a tu Backend / Firebase
                // Por ahora, cerramos y simulamos éxito
                enviarDatosReales()
            }
            .setNegativeButton("Editar") { dialog, _ ->
                dialog.dismiss() // Cierra el diálogo para que el usuario pueda corregir algo
            }
            .setCancelable(false) // Evita que se cierre tocando afuera
            .show()
    }

    private fun enviarDatosReales() {
        // Lógica final de guardado
        Toast.makeText(requireContext(), "¡Propiedad guardada con éxito!", Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }
}