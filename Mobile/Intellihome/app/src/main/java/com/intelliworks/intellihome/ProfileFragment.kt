package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.api.UsuarioApi
import com.intelliworks.intellihome.data.model.PropiedadResumenDto
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.SessionManager
import com.google.gson.Gson
import com.intelliworks.intellihome.utils.PropertyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.messaging.FirebaseMessaging
/**
 * Fragmento encargado de la gestión y visualización del perfil del usuario.
 *
 * Funcionalidades principales:
 * 1. Muestra información personal (Nombre, Correo, Domicilio).
 * 2. Traduce IDs de Hobbies y Preferencias a texto legible según el idioma del dispositivo.
 * 3. Lista las propiedades publicadas por el usuario actual.
 * 4. Gestiona la navegación hacia el detalle de propiedades y cierre de sesión.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // --- Dependencias y Adaptadores ---
    private lateinit var adapter: PropertyAdapter
    private lateinit var repoPropiedades: PropiedadRepository
    private lateinit var apiUsuario: UsuarioApi

    // --- Componentes de la Interfaz (UI) ---
    private lateinit var txtNombre: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtCorreo: TextView
    private lateinit var txtDomicilio: TextView
    private lateinit var txtHobbies: TextView
    private lateinit var txtPreferencias: TextView
    private lateinit var imgPerfil: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialización de la capa de red (Retrofit)
        val retrofit = RetrofitInstance.retrofit
        repoPropiedades = PropiedadRepository(retrofit.create(PropiedadApi::class.java))
        apiUsuario = retrofit.create(UsuarioApi::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Vinculación de vistas
        initViews(view)

        // 2. Configuración de eventos (Clicks)
        setupButtons(view)

        // 3. Configuración de lista (RecyclerView)
        setupRecyclerView(view)

        // 4. Carga de datos asíncrona
        loadUserProfile()
        loadUserProperties()
    }

    override fun onResume() {
        super.onResume()
        // Recargar propiedades al regresar a esta pantalla para reflejar cambios recientes
        loadUserProperties()
    }

    /**
     * Vincula los IDs del XML con las variables de la clase.
     * @param view La vista raíz del fragmento.
     */
    private fun initViews(view: View) {
        txtNombre = view.findViewById(R.id.txtNombrePerfil)
        txtUsuario = view.findViewById(R.id.txtUsername)
        txtCorreo = view.findViewById(R.id.txtEmail)
        txtDomicilio = view.findViewById(R.id.txtAddress)
        txtHobbies = view.findViewById(R.id.txtHobbies)
        txtPreferencias = view.findViewById(R.id.txtPreferences)
        imgPerfil = view.findViewById(R.id.imgPerfilUsuario)
    }

    /**
     * Configura los listeners para los botones de acción principal.
     */
    private fun setupButtons(view: View) {
        // Botón: Publicar nueva propiedad
        view.findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            val intent = Intent(requireContext(), AddPropertyActivity::class.java)
            startActivity(intent)
        }

        // Botón: Cerrar Sesión
        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            performLogout()
        }
    }

    /**
     * Cierre de sesión seguro:
     * 1. Obtiene el token actual.
     * 2. Llama a la API para borrarlo del servidor (evita notificaciones fantasma).
     * 3. Borra datos locales y va al Login.
     */
    private fun performLogout() {
        val context = requireContext()
        val userId = SessionManager.obtenerUserId(context)

        // Paso A: Obtener el token FCM actual antes de matar la sesión
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->

            // Si falla obtener el token, igual procedemos a cerrar sesión (null)
            val tokenToDelete = if (task.isSuccessful) task.result else null

            // Paso B: Ejecutar la limpieza en segundo plano
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Solo llamamos a la API si tenemos ID y Token válidos
                    if (!userId.isEmpty() && !tokenToDelete.isNullOrEmpty()) {
                        Log.d("Logout", "Desvinculando dispositivo del servidor...")

                        // Llamada a la API (Endpoint DELETE)
                        val response = apiUsuario.eliminarDispositivo(userId, tokenToDelete)

                        if (response.isSuccessful) {
                            Log.i("Logout", "Dispositivo desvinculado exitosamente.")
                        } else {
                            Log.w("Logout", "El servidor respondió error: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    // Si no hay internet, solo logueamos. NO bloqueamos la salida del usuario.
                    Log.e("Logout", "Error de red al desvincular (Ignorado)", e)
                } finally {
                    // Paso C: Limpieza Local y Navegación (Siempre se ejecuta)
                    withContext(Dispatchers.Main) {
                        // 1. Borrar SharedPreferences
                        SessionManager.cerrarSesion(context)

                        // 2. Ir al Login
                        val intent = Intent(context, Login::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    /**
     * Inicializa el RecyclerView con un adaptador vacío inicialmente.
     */
    private fun setupRecyclerView(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerMyProperties)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PropertyAdapter(emptyList()) { property ->
            navigateToPropertyDetails(property)
        }
        recycler.adapter = adapter
    }

    /**
     * Navega a la actividad de detalle/renta de propiedad.
     */
    private fun navigateToPropertyDetails(property: Property) {
        val intent = Intent(requireContext(), RentPropertyActivity::class.java)
        val gson = Gson()

        // Serializar objeto para pasarlo entre actividades
        intent.putExtra("property_data", gson.toJson(property))

        // Flag para indicar que es modo "visualización de propietario" (sin botón de rentar)
        intent.putExtra("is_rental_active", false)
        startActivity(intent)
    }

    /**
     * Solicita al backend los detalles del perfil del usuario.
     * Maneja la traducción de IDs numéricos a Strings localizados.
     */
    private fun loadUserProfile() {
        val userId = SessionManager.obtenerUserId(requireContext()).toIntOrNull() ?: return

        // Pre-carga visual (UX)
        txtNombre.text = SessionManager.obtenerNombreUsuario(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiUsuario.obtenerPerfilUsuario(userId)

                if (response.isSuccessful && response.body() != null) {
                    val profileDto = response.body()!!

                    // --- Mapeo de Datos Directos ---
                    txtNombre.text = profileDto.nombre_completo
                    txtUsuario.text = "@${profileDto.nombre_usuario}"
                    txtCorreo.text = profileDto.correo
                    txtDomicilio.text = profileDto.domicilio

                    // --- Traducción de IDs (Lógica i18n) ---
                    // Convertimos la lista de enteros [1, 2] -> "Ver TV, Senderismo"
                    val hobbiesText = getTranslatedHobbies(profileDto.hobbies_ids)
                    val prefsText = getTranslatedPreferences(profileDto.preferencias_ids)

                    txtHobbies.text = hobbiesText
                    txtPreferencias.text = prefsText

                    // --- Carga de Imagen ---
                    loadProfileImage(profileDto.imagen)

                } else {
                    Log.e("ProfileFragment", "Error API Perfil: ${response.code()}")
                    txtUsuario.text = getString(R.string.data_not_available)
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Excepción Red Perfil: ${e.message}")
            }
        }
    }

    /**
     * Helper para cargar la imagen de perfil usando Glide.
     * Maneja URLs absolutas y relativas.
     */
    private fun loadProfileImage(imagenPath: String?) {
        if (!imagenPath.isNullOrEmpty()) {
            val fullUrl = if (imagenPath.startsWith("http")) imagenPath
            else "${RetrofitInstance.BASE_URL.trimEnd('/')}/$imagenPath"

            Glide.with(this@ProfileFragment)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher_foreground)
                .error(R.mipmap.ic_launcher_foreground)
                .into(imgPerfil)
        }
    }

    /**
     * Solicita las propiedades pertenecientes al usuario.
     */
    private fun loadUserProperties() {
        val userIdStr = SessionManager.obtenerUserId(requireContext())
        val userId = userIdStr.toIntOrNull() ?: return
        val context = requireContext() // Capturamos el contexto seguro antes de la corrutina

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = repoPropiedades.obtenerPorUsuario(userId)

                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()

                    // Procesamiento pesado (Mapeo + Geocoding) en Hilo IO para no congelar la UI
                    val uiList = withContext(Dispatchers.IO) {
                        dtoList.map { dto ->
                            // ¡UNA SOLA LÍNEA MÁGICA!
                            PropertyUtils.mapDtoToProperty(dto, userIdStr, context)
                        }
                    }

                    adapter.updateList(uiList)
                } else {
                    Log.e("Profile", "Error API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Profile", "Error Red: ${e.message}")
            }
        }
    }

    // ========================================================================
    // MÉTODOS DE TRADUCCIÓN (I18N)
    // ========================================================================

    /**
     * Convierte una lista de IDs de hobbies en una cadena de texto legible y localizada.
     */
    private fun getTranslatedHobbies(ids: List<Int>?): String {
        if (ids.isNullOrEmpty()) return getString(R.string.text_not_defined)

        val names = ids.map { id ->
            when (id) {
                1 -> getString(R.string.hobby_tv)
                2 -> getString(R.string.hobby_hiking)
                3 -> getString(R.string.hobby_boardgames)
                4 -> getString(R.string.hobby_snorkel)
                5 -> getString(R.string.hobby_sports)
                else -> getString(R.string.data_not_available)
            }
        }
        return names.joinToString(", ")
    }

    /**
     * Convierte una lista de IDs de preferencias de casa en texto localizado.
     */
    private fun getTranslatedPreferences(ids: List<Int>?): String {
        if (ids.isNullOrEmpty()) return getString(R.string.text_not_defined)

        val names = ids.map { id ->
            when (id) {
                1 -> getString(R.string.house_minimalist)
                2 -> getString(R.string.house_adventurous)
                3 -> getString(R.string.house_contemporary)
                else -> getString(R.string.data_not_available)
            }
        }
        return names.joinToString(", ")
    }
}

// ========================================================================
// EXTENSIONES
// ========================================================================

/**
 * Extensión para convertir PropiedadResumenDto (Backend) a Property (Modelo UI).
 * Se extrae fuera de la clase para mantener limpieza.
 */
/**
 * Extensión para convertir PropiedadResumenDto (Backend) a Property (Modelo UI).
 */
private fun PropiedadResumenDto.toPropertyUi(userId: String): Property {
    val capacityCsv = "${this.huespedes},${this.habitaciones},${this.camas},${this.banos}"
    val imagesList = if (!this.imagen.isNullOrEmpty()) listOf(this.imagen) else emptyList()

    val locationString = if (this.latitud != null && this.longitud != null)
        "Lat: ${this.latitud}, Lon: ${this.longitud}" else "Ubicación registrada"

    return Property(
        id = this.id.toString(),
        userId = usuarioId.toString(),
        titulo = this.titulo,
        precio = this.precio.toString(),
        direccion = locationString,
        tipo = "Mi Propiedad",
        capacidad = capacityCsv,
        imagenes = imagesList,
        descripcion = "Detalles disponibles al seleccionar",
        actividades = "",
        comodidades = "",
        reglas = "",
        rentedByUserId = null
    )
}