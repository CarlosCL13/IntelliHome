package com.intelliworks.intellihome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.data.repository.PropertyRepository
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.SessionManager

/**
 * Fragmento de perfil de usuario.
 * Muestra información de la sesión actual y lista las propiedades creadas por este usuario.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var adapter: PropertyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vinculación de datos del usuario desde SessionManager
        val txtNombreUsuario = view.findViewById<TextView>(R.id.txtNombrePerfil) // Asegurar ID en XML
        txtNombreUsuario.text = SessionManager.obtenerNombreUsuario(requireContext())

        // Navegación al flujo de creación de propiedad
        view.findViewById<Button>(R.id.btnAddProperty).setOnClickListener {
            startActivity(Intent(requireContext(), AddPropertyActivity::class.java))
        }
        // Cerrar sesión
        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            // Limpia los datos de sesión
            SessionManager.cerrarSesion(requireContext())

            // Redirige al Login y limpia la pila de actividades para que no pueda volver atrás
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
        val propiedadesUsuario = obtenerPropiedadesFiltradas()
        adapter.updateList(propiedadesUsuario)
    }

    /**
     * Filtra las propiedades locales comparando el userId de la propiedad
     * con el userId de la sesión activa.
     */
    private fun obtenerPropiedadesFiltradas(): List<com.intelliworks.intellihome.utils.Property> {
        val context = requireContext()
        val idUsuarioActual = SessionManager.obtenerUserId(context)

        return PropertyRepository.getProperties(context).filter {
            it.userId == idUsuarioActual
        }
    }
}