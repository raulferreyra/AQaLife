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

    private lateinit var txtResumenImc: TextView
    private lateinit var txtResumenDatos: TextView
    private lateinit var imcGauge: ImcGaugeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtResumenImc = view.findViewById(R.id.txtResumenImc)
        txtResumenDatos = view.findViewById(R.id.txtResumenDatos)
        imcGauge = view.findViewById(R.id.imcGauge)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val tieneDatos = prefs.contains(KEY_ALTURA) && prefs.contains(KEY_PESO)

        if (!tieneDatos) {
            // No hay datos: ir a Setup (B)
            findNavController().navigate(R.id.nav_setup)
            return
        }

        val alturaCm = prefs.getFloat(KEY_ALTURA, 0f)
        val pesoKg = prefs.getFloat(KEY_PESO, 0f)
        val fechaNac = prefs.getString(KEY_FECHA, "-")

        val imc = calcularImc(pesoKg, alturaCm)
        val (categoria, aguaLitros, vasos) = obtenerRecomendacion(imc)

        // Bloque de IMC + recomendación (ARRIBA)
        val textoImc = """
            IMC: ${"%.2f".format(imc)}
            Categoría: $categoria
            Agua recomendada: $aguaLitros ($vasos)
        """.trimIndent()
        txtResumenImc.text = textoImc

        // Actualizar KPI
        imcGauge.setImc(imc)

        // Bloque de datos del usuario (ABAJO)
        val textoDatos = """
            Peso: ${"%.1f".format(pesoKg)} kg
            Talla: ${"%.1f".format(alturaCm)} cm
            Fecha nacimiento: $fechaNac
        """.trimIndent()
        txtResumenDatos.text = textoDatos
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
            else       -> Triple("Obesidad III", "4 – 4.5 L", "16 – 18 vasos")
        }
    }

    companion object {
        private const val PREFS_NAME = "imc_prefs"
        private const val KEY_ALTURA = "altura_cm"
        private const val KEY_PESO = "peso_kg"
        private const val KEY_FECHA = "fecha_nac"
    }
}