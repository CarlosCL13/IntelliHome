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
import androidx.lifecycle.lifecycleScope
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
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class PropertyPhotosFragment : Fragment(R.layout.fragment_property_photos) {

    private val viewModel: AddPropertyViewModel by activityViewModels()
    private lateinit var adapter: PhotosAdapter
    private lateinit var txtContador: TextView
    private lateinit var btnTerminar: MaterialButton

    private val MAX_PHOTOS = 10
    private val MAX_FILE_SIZE_MB = 1
    private val ALLOWED_TYPES = listOf(
        "image/jpeg", "image/png", "image/gif"
    )

    // Variable para guardar la URI de la foto que se está tomando
    private var latestPhotoUri: Uri? = null

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) validarYAgregarFotos(uris)
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && latestPhotoUri != null) {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {

                // Llamamos a la nueva función
                val uriComprimida = comprimirYGuardarNuevoArchivo(latestPhotoUri!!)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (uriComprimida != null) {
                        // IMPORTANTE: Pasamos la uriComprimida, NO la latestPhotoUri
                        validarYAgregarFotos(listOf(uriComprimida))
                    } else {
                        Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Lee la imagen original, la comprime y la guarda en un NUEVO archivo.
     * Retorna la URI del nuevo archivo comprimido.
     */
    private fun comprimirYGuardarNuevoArchivo(uriOriginal: Uri): Uri? {
        return try {
            val contentResolver = requireContext().contentResolver

            // 1. Decodificar
            val inputStream = contentResolver.openInputStream(uriOriginal)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 2. Redimensionar si es gigante (prevención de OOM)
            var workingBitmap = originalBitmap
            val maxDimension = 2048
            if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                workingBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            }

            // 3. Comprimir en memoria
            val stream = java.io.ByteArrayOutputStream()
            var quality = 100
            workingBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)

            // Bajar calidad hasta que pese menos de 1MB (1000 KB aprox de margen)
            while (stream.toByteArray().size > 1000 * 1024 && quality > 10) {
                stream.reset()
                quality -= 10
                workingBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)
            }

            // 4. GUARDAR EN UN ARCHIVO NUEVO (Clave del arreglo)
            val nuevoArchivo = java.io.File(requireContext().cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = java.io.FileOutputStream(nuevoArchivo)
            fileOutputStream.write(stream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()

            // Limpieza
            if (workingBitmap != originalBitmap) workingBitmap.recycle()
            originalBitmap.recycle()

            // Retornamos la URI del archivo nuevo (file://...)
            android.net.Uri.fromFile(nuevoArchivo)

        } catch (e: Exception) {
            e.printStackTrace()
            null
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

        if (savedInstanceState != null) {
            val uriString = savedInstanceState.getString("pending_photo_uri")
            if (uriString != null) {
                latestPhotoUri = Uri.parse(uriString)
            }
        }

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
                mostrarOpcionesDeImagen()
            }
        }

        btnTerminar.setOnClickListener { finalizarPublicacion() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (latestPhotoUri != null) {
            outState.putString("pending_photo_uri", latestPhotoUri.toString())
        }
    }

    private fun mostrarOpcionesDeImagen() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar fotos")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamara()
                    1 -> abrirGaleria()
                }
            }
            .show()
    }
    private fun abrirGaleria() {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun abrirCamara() {
        // Crear un archivo temporal para guardar la foto
        try {
            latestPhotoUri = getTmpFileUri()
            takePhotoLauncher.launch(latestPhotoUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al iniciar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            tmpFile
        )
    }

    private fun validarYAgregarFotos(uris: List<Uri>) {
        val currentSize = viewModel.fotosSeleccionadas.value?.size ?: 0
        var spaceAvailable = MAX_PHOTOS - currentSize

        if (spaceAvailable <= 0) return

        val fotosValidas = mutableListOf<Uri>()
        var errores = 0
        var mensajeError = ""

        for (uri in uris) {
            if (spaceAvailable <= 0) break

            val resultado = verificarArchivo(uri)
            if (resultado.esValido) {
                fotosValidas.add(uri)
                spaceAvailable--
            } else {
                errores++
                mensajeError = resultado.mensajeError
            }
        }

        if (errores > 0) {
            val msg = if (errores == 1) "Una foto fue rechazada: $mensajeError"
            else "$errores fotos rechazadas (Formato o peso)"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }

        if (fotosValidas.isNotEmpty()) {
            viewModel.agregarFotos(fotosValidas)
        }
    }

    private fun verificarArchivo(uri: Uri): ResultadoValidacion {
        val contentResolver = requireContext().contentResolver

        // 1. Validar Tipo MIME
        val type = contentResolver.getType(uri)
        // Nota: A veces las fotos de cámara recién creadas pueden devolver null en type al principio,
        // pero FileProvider suele manejarlo bien. Si da problemas, puedes inferir por extensión.
        if (type != null && !ALLOWED_TYPES.contains(type)) {
            return ResultadoValidacion(false, "Formato no permitido")
        }

        // 2. Validar Tamaño
        var sizeInBytes: Long = 0
        val cursor = contentResolver.query(uri, null, null, null, null)

        // Si el cursor es nulo (pasa a veces con FileProvider interno), intentamos leer el archivo directo
        if (cursor != null) {
            cursor.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeInBytes = it.getLong(sizeIndex)
                    }
                }
            }
        } else {
            // Fallback para calcular tamaño si el cursor falla (común en URIs de FileProvider)
            try {
                val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                sizeInBytes = fileDescriptor?.statSize ?: 0
                fileDescriptor?.close()
            } catch (e: Exception) {
                // Ignorar error de lectura
            }
        }

        val sizeInMb = sizeInBytes / (1024.0 * 1024.0)

        // IMPORTANTE: Ajusta MAX_FILE_SIZE_MB si las fotos de tu cámara son grandes (ej. 5MB)
        // Las cámaras modernas sacan fotos de 3 a 10MB fácilmente.
        if (sizeInMb > MAX_FILE_SIZE_MB) {
            return ResultadoValidacion(false, "La imagen es muy pesada (> ${MAX_FILE_SIZE_MB}MB)")
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

    private fun construirResumen(): String {
        val sb = StringBuilder()
        val tipo = viewModel.getNombreTipoSeleccionado()
        val titulo = viewModel.titulo.value ?: getString(R.string.text_no_title)
        val precio = viewModel.precio.value ?: "0"

        sb.append("${getString(R.string.summary_type)} $tipo\n")
        sb.append("${getString(R.string.summary_title)} $titulo\n")
        sb.append("${getString(R.string.summary_price)} $precio\n\n")

        val nHuespedes = viewModel.huespedes.value ?: 0
        sb.append("${getString(R.string.summary_capacity)}: $nHuespedes huéspedes\n\n")

        val ubicacion = viewModel.direccionTexto.value ?: getString(R.string.text_no_address)
        sb.append("${getString(R.string.summary_location)} $ubicacion\n\n")

        val actividadesStr = viewModel.getNombresHobbiesSeleccionados()
        val finalAct = if (actividadesStr.isNotEmpty()) actividadesStr else getString(R.string.text_none)
        sb.append("${getString(R.string.summary_activities)} $finalAct\n\n")

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
        btnTerminar.isEnabled = false
        val context = requireContext()
        val userId = SessionManager.obtenerUserId(context).toIntOrNull() ?: 0

        val tipoCasaId = viewModel.tipoSeleccionadoId.value ?: 1
        val hobbiesIds = viewModel.hobbiesSeleccionadosIds.value?.toList() ?: emptyList()
        val amenidadesIds = viewModel.amenidadesSeleccionadasIds.value?.toList() ?: emptyList()
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
        val repo = PropiedadRepository(api)

        lifecycleScope.launch {
            try {
                Toast.makeText(context, "Publicando...", Toast.LENGTH_SHORT).show()

                val response = repo.registrarPropiedad(
                    context = context,
                    usuarioId = userId,
                    tipoCasaId = tipoCasaId,
                    hobbiesIds = hobbiesIds,
                    amenidadesIds = amenidadesIds,
                    diasDisponibles = viewModel.diasSeleccionados.value?.toList() ?: emptyList(),
                    latitud = latitud,
                    longitud = longitud,
                    titulo = titulo,
                    descripcion = descripcion,
                    precio = precio,
                    huespedes = huespedes,
                    habitaciones = habitaciones,
                    camas = camas,
                    banos = banos,
                    cocina = true,
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
}