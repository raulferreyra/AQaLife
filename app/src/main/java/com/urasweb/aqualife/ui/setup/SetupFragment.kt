package com.urasweb.aqualife.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.urasweb.aqualife.R
import com.google.android.material.textfield.TextInputEditText

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [SetupFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SetupFragment : Fragment() {

    private lateinit var inputAltura: TextInputEditText
    private lateinit var inputPeso: TextInputEditText
    private lateinit var inputFechaNac: TextInputEditText
    private lateinit var btnGuardar: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputAltura = view.findViewById(R.id.inputAltura)
        inputPeso = view.findViewById(R.id.inputPeso)
        inputFechaNac = view.findViewById(R.id.inputFechaNac)
        btnGuardar = view.findViewById(R.id.btnGuardar)

        btnGuardar.setOnClickListener {
            guardarDatosYVolverAlDashboard()
        }
    }

    private fun guardarDatosYVolverAlDashboard() {
        val alturaStr = inputAltura.text?.toString()?.trim()
        val pesoStr = inputPeso.text?.toString()?.trim()
        val fechaStr = inputFechaNac.text?.toString()?.trim()

        if (alturaStr.isNullOrEmpty() || pesoStr.isNullOrEmpty() || fechaStr.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val alturaCm = alturaStr.toFloatOrNull()
        val pesoKg = pesoStr.toFloatOrNull()

        if (alturaCm == null || pesoKg == null || alturaCm <= 0f || pesoKg <= 0f) {
            Toast.makeText(requireContext(), "Revisa peso y talla", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = requireContext().getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("altura_cm", alturaCm)
            .putFloat("peso_kg", pesoKg)
            .putString("fecha_nac", fechaStr)
            .apply()

        // Volver al Dashboard (HomeFragment)
        findNavController().navigateUp()
    }
}