package com.intelliworks.intellihome

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.intelliworks.intellihome.data.api.PropiedadApi
import com.intelliworks.intellihome.data.api.RetrofitInstance.BASE_URL
import com.intelliworks.intellihome.data.model.PropiedadAlquiladaDto
import com.intelliworks.intellihome.data.repository.PropiedadRepository
import com.intelliworks.intellihome.utils.Property
import com.intelliworks.intellihome.utils.PropertyAdapter
import com.intelliworks.intellihome.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RentalsFragment : Fragment(R.layout.fragment_rentals) {

    private lateinit var adapter: PropertyAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var recycler: RecyclerView

    private lateinit var repo: PropiedadRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(PropiedadApi::class.java)
        repo = PropiedadRepository(api)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recyclerRentals)
        emptyState = view.findViewById(R.id.emptyState)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = PropertyAdapter(emptyList())
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarMisAlquileres()
    }

    private fun cargarMisAlquileres() {
        val myUserIdStr = SessionManager.obtenerUserId(requireContext())
        val myUserId = myUserIdStr.toIntOrNull()

        if (myUserId == null) {
            Log.e("RentalsFragment", "UserId inválido en sesión: $myUserIdStr")
            mostrarEmpty()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = repo.obtenerAlquiladasPorUsuario(myUserId)

                if (!response.isSuccessful) {
                    Log.e("RentalsFragment", "GET alquiladas falló: HTTP ${response.code()}")
                    mostrarEmpty()
                    return@launch
                }

                val alquiladasDto = response.body().orEmpty()
                val alquiladasUi = alquiladasDto.map { it.toPropertyUi(myUserIdStr) }

                if (alquiladasUi.isEmpty()) {
                    mostrarEmpty()
                } else {
                    recycler.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                    adapter.updateList(alquiladasUi)
                }
            } catch (e: Exception) {
                Log.e("RentalsFragment", "Excepción cargando alquiladas: ${e.message}", e)
                mostrarEmpty()
            }
        }
    }

    private fun mostrarEmpty() {
        recycler.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }
}

private fun PropiedadAlquiladaDto.toPropertyUi(myUserIdStr: String): Property {
    val capacidadCsv = "${this.huespedes},${this.habitaciones},${this.camas},${this.banos}"
    val imagenes = if (this.imagen != null) listOf(this.imagen) else emptyList()

    return Property(
        id = this.id.toString(),
        userId = "0",
        nombreUsuario = "Anfitrión",
        titulo = this.titulo,
        precio = this.precio.toString(),
        direccion = "Ubicación en mapa",
        tipo = "Casa",
        capacidad = capacidadCsv,
        imagenes = imagenes,
        descripcion = "Ver detalles para más información.",
        actividades = "",
        comodidades = "",
        reglas = "",
        rentedByUserId = myUserIdStr
    )
}

