package com.urasweb.aqualife.ui.imc_historic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.urasweb.aqualife.R
import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.repository.AquaRepository
import kotlinx.coroutines.launch

class ImcHistoricFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private val adapter = ImcHistoricAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_imc_historic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvImcHistory)
        emptyView = view.findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        cargarHistorial()
    }

    private fun cargarHistorial() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val appContext = requireContext().applicationContext

                // Aseguramos que la DB y el repo estén inicializados
                AquaDatabase.init(appContext)
                val db = AquaDatabase.getInstance()
                AquaRepository.init(db)

                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser

                if (currentUser == null) {
                    Toast.makeText(
                        requireContext(),
                        "Usuario no autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                    emptyView.isVisible = true
                    recyclerView.isVisible = false
                    return@launch
                }

                AquaRepository.setCurrentUser(currentUser.uid)

                val records = AquaRepository.getImcHistoryForCurrentUser()

                adapter.setItems(records)
                emptyView.isVisible = records.isEmpty()
                recyclerView.isVisible = records.isNotEmpty()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar historial: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                emptyView.isVisible = true
                recyclerView.isVisible = false
            }
        }
    }
}
