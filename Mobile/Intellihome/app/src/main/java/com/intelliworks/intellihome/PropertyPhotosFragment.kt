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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.PhotosAdapter
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.SessionManager
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
        if (uris.isNotEmpty()) {
            validarYAgregarFotos(uris)
        }
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
            val textoBase = getString(R.string.text_photo_requirements)
            txtContador.text = "$textoBase\n(${lista.size}/$MAX_PHOTOS)"
        }

        btnAddPhotos.setOnClickListener {
            val cantidadActual = viewModel.fotosSeleccionadas.value?.size ?: 0
            if (cantidadActual >= MAX_PHOTOS) {
                val mensaje = "${getString(R.string.error_limit_reached)} (${getString(R.string.error_max_photos, MAX_PHOTOS)})"
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            } else {
                pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        btnTerminar.setOnClickListener { finalizarPublicacion() }
    }

    private fun validarYAgregarFotos(uris: List<Uri>) {
        val fotosActuales = viewModel.fotosSeleccionadas.value ?: emptyList()
        val espacioDisponible = MAX_PHOTOS - fotosActuales.size
        if (espacioDisponible <= 0) return

        val urisAProcesar = uris.take(espacioDisponible)
        val fotosValidas = mutableListOf<Uri>()

        for (uri in urisAProcesar) {
            val validacion = verificarArchivo(uri)
            if (validacion.esValido) {
                fotosValidas.add(uri)
            }
        }
        if (fotosValidas.isNotEmpty()) {
            viewModel.agregarFotos(fotosValidas)
        }
    }

    data class ResultadoValidacion(val esValido: Boolean, val mensajeError: String = "")

    private fun verificarArchivo(uri: Uri): ResultadoValidacion {
        val contentResolver = requireContext().contentResolver
        val type = contentResolver.getType(uri)
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            return ResultadoValidacion(false, getString(R.string.error_format_not_allowed))
        }
        return ResultadoValidacion(true)
    }

    private fun finalizarPublicacion() {
        val fotos = viewModel.fotosSeleccionadas.value
        if (fotos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_no_photos), Toast.LENGTH_SHORT).show()
            return
        }
        mostrarDialogoConfirmacion(getString(R.string.msg_publish_confirmation))
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
        val context = requireContext()
        val titulo = viewModel.titulo.value ?: getString(R.string.default_no_title)
        val precio = viewModel.precio.value ?: "0"
        val direccion = viewModel.direccionTexto.value ?: getString(R.string.default_unknown_location)
        val tipo = viewModel.tipoPropiedad.value ?: getString(R.string.default_property_type)

        val huespedes = viewModel.huespedes.value ?: 0
        val habs = viewModel.habitaciones.value ?: 0
        val camas = viewModel.camas.value ?: 0
        val banos = viewModel.banos.value ?: 0

        val idUsuarioActual = SessionManager.obtenerUserId(context)
        val nombreAnfitrion = SessionManager.obtenerNombreUsuario(context) ?: getString(R.string.default_host)

        /**
         * Almacenamiento en formato CSV (Raw Data) para permitir localización dinámica.
         * Estructura: "huéspedes,habitaciones,camas,baños"
         */
        val capacidadRaw = "$huespedes,$habs,$camas,$banos"

        // Persistencia de imágenes en almacenamiento interno
        val listaUrisOriginales = viewModel.fotosSeleccionadas.value ?: emptyList()
        val listaRutasPersistentes = listaUrisOriginales.mapNotNull { uriOriginal ->
            copiarImagenAInternalStorage(context, uriOriginal)
        }

        val actividadesStr = viewModel.actividadesSeleccionadas.value?.joinToString(", ") ?: getString(R.string.default_no_activities)
        val comodidadesStr = viewModel.comodidadesSeleccionadas.value?.joinToString(", ") ?: getString(R.string.default_no_amenities)
        val descripcionStr = viewModel.descripcion.value ?: getString(R.string.default_no_description)
        val reglasStr = viewModel.reglas.value ?: getString(R.string.default_no_rules)

        val nuevaPropiedad = Property(
            id = UUID.randomUUID().toString(),
            userId = idUsuarioActual,
            nombreUsuario = nombreAnfitrion,
            titulo = titulo,
            precio = precio,
            direccion = direccion,
            tipo = tipo,
            capacidad = capacidadRaw,
            imagenes = listaRutasPersistentes,
            descripcion = descripcionStr,
            actividades = actividadesStr,
            comodidades = comodidadesStr,
            reglas = reglasStr
        )

        PropertyRepository.saveProperty(context, nuevaPropiedad)

        Toast.makeText(context, getString(R.string.msg_publish_success), Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    /**
     * Copia la imagen seleccionada al almacenamiento privado de la aplicación
     * para asegurar su persistencia.
     */
    private fun copiarImagenAInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val imagesDir = File(context.filesDir, "property_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(imagesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}