package com.crediweb.aqualife.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.crediweb.aqualife.R
import com.crediweb.aqualife.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var txtResumen: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtResumen = view.findViewById(R.id.txtResumen)

        val prefs = requireContext().getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)

        val tieneDatos = prefs.contains("altura_cm") && prefs.contains("peso_kg")

        if (!tieneDatos) {
            // No hay datos: ir a Pantalla de Inicio
            findNavController().navigate(R.id.nav_setup)
            return
        }

        // Sí hay datos: mostrar Dashboard
        val alturaCm = prefs.getFloat("altura_cm", 0f)
        val pesoKg = prefs.getFloat("peso_kg", 0f)
        val fechaNac = prefs.getString("fecha_nac", "-")

        val imc = calcularImc(pesoKg, alturaCm)
        val (categoria, aguaLitros, vasos) = obtenerRecomendacion(imc)

        val mensaje = """
            Peso: ${"%.1f".format(pesoKg)} kg
            Talla: ${"%.1f".format(alturaCm)} cm
            Fecha nacimiento: $fechaNac

            IMC: ${"%.2f".format(imc)}
            Categoría: $categoria
            Agua recomendada: $aguaLitros ($vasos)
        """.trimIndent()

        txtResumen.text = mensaje
    }

    private fun calcularImc(pesoKg: Float, tallaCm: Float): Double {
        val tallaM = tallaCm / 100f
        return (pesoKg / (tallaM * tallaM)).toDouble()
    }

    private fun obtenerRecomendacion(imc: Double): Triple<String, String, String> {
        return when {
            imc < 18.5 -> Triple("Bajo peso", "1.5 – 2 L", "6 – 8 vasos")
            imc < 24.9 -> Triple("Normal", "2 – 2.5 L", "8 – 10 vasos")
            imc < 29.9 -> Triple("Sobrepeso", "2.5 – 3 L", "10 – 12 vasos")
            imc < 34.9 -> Triple("Obesidad I", "3 – 3.5 L", "12 – 14 vasos")
            imc < 39.9 -> Triple("Obesidad II", "3.5 – 4 L", "14 – 16 vasos")
            else        -> Triple("Obesidad III", "4 – 4.5 L", "16 – 18 vasos")
        }
    }
}
