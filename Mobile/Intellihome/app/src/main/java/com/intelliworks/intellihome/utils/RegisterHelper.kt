package com.intelliworks.intellihome.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.intelliworks.intellihome.R
import java.io.File
import java.util.Calendar

/**
 * Helper con utilidades para el proceso de registro
 */
object RegisterHelper {
    
    private const val MAX_IMAGE_SIZE_BYTES = 1048576 // 1MB
    private val ALLOWED_IMAGE_TYPES = listOf("image/jpeg", "image/jpg", "image/png", "image/gif")
    private val ALLOWED_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif")

    // ========== FUNCIONES DE IMÁGENES ==========
    
    /**
     * Valida el tamaño de una imagen
     * @return true si es válida, false si excede el tamaño permitido
     */
    fun validarTamanoImagen(context: Context, uri: Uri): Boolean {
        val inputStream = context.contentResolver.openInputStream(uri)
        val size = inputStream?.available() ?: 0
        inputStream?.close()
        return size <= MAX_IMAGE_SIZE_BYTES
    }

    /**
     * Valida el tipo/extensión de una imagen
     * @return true si el tipo es permitido (jpg, jpeg, png, gif), false en caso contrario
     */
    fun validarTipoImagen(context: Context, uri: Uri): Boolean {
        // Validar por MIME type
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && mimeType in ALLOWED_IMAGE_TYPES) {
            return true
        }

        // Validar por extensión del nombre del archivo
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                val fileName = it.getString(nameIndex).lowercase()
                val extension = fileName.substringAfterLast('.', "")
                return extension in ALLOWED_EXTENSIONS
            }
        }
        
        return false
    }

    /**
     * Valida que una imagen cumpla con el tamaño y tipo permitidos
     * @return Par con (esValida, mensajeError). Si esValida es true, mensajeError es null
     */
    fun validarImagen(context: Context, uri: Uri): Pair<Boolean, String?> {
        // Validar tipo primero
        if (!validarTipoImagen(context, uri)) {
            return Pair(false, "Solo se permiten imágenes JPG, PNG o GIF")
        }

        // Validar tamaño
        if (!validarTamanoImagen(context, uri)) {
            return Pair(false, "La imagen debe ser menor a 1MB")
        }

        return Pair(true, null)
    }

    /**
     * Crea un archivo temporal a partir de una URI
     * @return File temporal o null si hay error
     */
    fun crearArchivoTemporal(context: Context, uri: Uri): File? {
        return try {
            var nombreArchivo = "imagen_temp.jpg"

            // Intenta obtener el nombre original del archivo
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    nombreArchivo = it.getString(nameIndex)
                }
            }

            // Crea el archivo temporal en el directorio de caché
            val archivoTemp = File.createTempFile(
                nombreArchivo.substringBeforeLast('.'),
                "." + nombreArchivo.substringAfterLast('.', "jpg"),
                context.cacheDir
            )
            archivoTemp.deleteOnExit()

            // Copia el contenido al archivo temporal
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                archivoTemp.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            archivoTemp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ========== FUNCIONES DE DIÁLOGOS ==========

    /**
     * Muestra un diálogo para seleccionar mes y año
     */
    fun mostrarSelectorMesAnio(context: Context, onDateSelected: (month: Int, year: Int) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_month_year_picker, null)

        val selectorMes = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val selectorAnio = dialogView.findViewById<NumberPicker>(R.id.yearPicker)

        // Configura el rango para el mes (1 a 12)
        selectorMes.minValue = 1
        selectorMes.maxValue = 12
        selectorMes.value = Calendar.getInstance().get(Calendar.MONTH) + 1

        // Configura el rango para el año
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        selectorAnio.minValue = anioActual
        selectorAnio.maxValue = anioActual + 20
        selectorAnio.value = anioActual

        AlertDialog.Builder(context)
            .setTitle("Seleccione mes y año")
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                onDateSelected(selectorMes.value, selectorAnio.value)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo de términos y condiciones con scroll obligatorio
     */
    fun mostrarDialogoTerminos(context: Context, terminosRawId: Int, onAceptar: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        
        // Lee el archivo de términos
        val inputStream = context.resources.openRawResource(terminosRawId)
        val terminos = inputStream.bufferedReader().use { it.readText() }
        
        // Crea el ScrollView con el TextView
        val scrollView = ScrollView(context)
        val textView = TextView(context)
        textView.text = terminos
        textView.setPadding(32, 32, 32, 32)
        textView.textSize = 16f
        scrollView.addView(textView)

        builder.setView(scrollView)
        builder.setNegativeButton("Cancelar", null)
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            onAceptar()
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        // Controla el botón aceptar - solo se habilita al llegar al final
        val btnAceptar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnAceptar.isEnabled = false

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollView.getChildAt(0)
            if (view != null) {
                val diff = view.bottom - (scrollView.height + scrollView.scrollY)
                btnAceptar.isEnabled = diff <= 0
            }
        }
    }
}
