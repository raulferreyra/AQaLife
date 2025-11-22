package com.urasweb.aqualife.ui.personalinfo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.urasweb.aqualife.R

class PersonalInfoFragment : Fragment() {

    private lateinit var inputPeso: EditText
    private lateinit var inputAltura: EditText
    private lateinit var inputFecha: EditText
    private lateinit var inputPerimetro: EditText
    private lateinit var spinnerFrecuencia: Spinner
    private lateinit var btnGuardar: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personal_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputPeso = view.findViewById(R.id.inputPeso)
        inputAltura = view.findViewById(R.id.inputAltura)
        inputFecha = view.findViewById(R.id.inputFechaNac)
        inputPerimetro = view.findViewById(R.id.inputPerimetro)
        spinnerFrecuencia = view.findViewById(R.id.spinnerFrecuencia)
        btnGuardar = view.findViewById(R.id.btnGuardar)

        // Opciones del spinner
        val opciones = listOf(
            "Cada 30 minutos",
            "Cada 1 hora",
            "Cada 2 horas"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            opciones
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrecuencia.adapter = adapter

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Cargar valores
        val peso = prefs.getFloat(KEY_PESO, 0f)
        val altura = prefs.getFloat(KEY_ALTURA, 0f)
        val fecha = prefs.getString(KEY_FECHA, "")
        val perimetro = prefs.getFloat(KEY_PERIMETRO, 0f)
        val freq = prefs.getInt(KEY_FREQ_MINUTOS, 60) // por defecto 60 min

        if (peso > 0f) inputPeso.setText(peso.toString())
        if (altura > 0f) inputAltura.setText(altura.toString())
        inputFecha.setText(fecha ?: "")
        if (perimetro > 0f) inputPerimetro.setText(perimetro.toString())

        spinnerFrecuencia.setSelection(
            when (freq) {
                30 -> 0
                120 -> 2
                else -> 1 // 60 min
            }
        )

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun guardarCambios() {
        val pesoStr = inputPeso.text.toString().trim()
        val alturaStr = inputAltura.text.toString().trim()
        val fechaStr = inputFecha.text.toString().trim()
        val perimetroStr = inputPerimetro.text.toString().trim()

        val peso = pesoStr.toFloatOrNull()
        val altura = alturaStr.toFloatOrNull()
        val perimetro = perimetroStr.toFloatOrNull()

        if (peso == null || altura == null || fechaStr.isEmpty()) {
            Toast.makeText(requireContext(), "Completa peso, talla y fecha", Toast.LENGTH_SHORT).show()
            return
        }

        val freqMin = when (spinnerFrecuencia.selectedItemPosition) {
            0 -> 30
            1 -> 60
            else -> 120
        }

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_PESO, peso)
            .putFloat(KEY_ALTURA, altura)
            .putString(KEY_FECHA, fechaStr)
            .putFloat(KEY_PERIMETRO, perimetro ?: 0f)
            .putInt(KEY_FREQ_MINUTOS, freqMin)
            .apply()

        Toast.makeText(requireContext(), "Datos guardados", Toast.LENGTH_SHORT).show()

        // Volver al Dashboard (A)
        findNavController().navigateUp()
    }

    companion object {
        private const val PREFS_NAME = "imc_prefs"
        private const val KEY_ALTURA = "altura_cm"
        private const val KEY_PESO = "peso_kg"
        private const val KEY_FECHA = "fecha_nac"
        private const val KEY_PERIMETRO = "perimetro_abd"
        private const val KEY_FREQ_MINUTOS = "freq_minutos"
    }
}
