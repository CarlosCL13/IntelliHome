package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView // Importante
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Importante
import com.intelliworks.intellihome.data.api.RetrofitInstance
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.SessionManager

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var adapter: PropertyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Vinculación del Nombre
        val txtNombreUsuario = view.findViewById<TextView>(R.id.txtNombrePerfil)
        txtNombreUsuario.text = SessionManager.obtenerNombreUsuario(requireContext())

        // 2. Vinculación de la Imagen de Perfil (NUEVO)
        val imgPerfil = view.findViewById<ImageView>(R.id.imgPerfilUsuario) // Asegúrate que este ID exista en tu XML
        val rutaRelativa = SessionManager.obtenerFotoPerfil(requireContext())

        if (!rutaRelativa.isNullOrEmpty()) {
            // Construimos la URL completa usando la base de Retrofit
            // Ejemplo: http://192.168.1.50:8000/ + uploads/foto.jpg
            val urlCompleta = RetrofitInstance.BASE_URL + rutaRelativa

            Glide.with(this)
                .load(urlCompleta)
                .circleCrop() // Recorta la imagen en círculo automáticamente
                .placeholder(R.drawable.ic_user_placeholder) // Imagen mientras carga (crea este drawable o usa uno existente)
                .error(R.drawable.ic_user_placeholder) // Imagen si falla
                .into(imgPerfil)
        }

        // Navegación "Agregar Propiedad"
        view.findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            startActivity(Intent(requireContext(), AddPropertyActivity::class.java))
        }

        // Cerrar sesión
        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            SessionManager.cerrarSesion(requireContext())
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        configurarListaPropiedades(view)
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            recargarPropiedadesUsuario()
        }
    }

    private fun configurarListaPropiedades(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerMyProperties)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val propiedadesUsuario = obtenerPropiedadesFiltradas()
        adapter = PropertyAdapter(propiedadesUsuario)
        recycler.adapter = adapter
    }

    private fun recargarPropiedadesUsuario() {
        adapter.updateList(obtenerPropiedadesFiltradas())
    }

    private fun obtenerPropiedadesFiltradas(): List<com.intelliworks.intellihome.utils.Property> {
        val context = requireContext()
        val idUsuarioActual = SessionManager.obtenerUserId(context)
        return PropertyRepository.getProperties(context).filter {
            it.userId == idUsuarioActual
        }
    }
}