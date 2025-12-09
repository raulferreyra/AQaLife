package com.urasweb.aqualife.ui.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.urasweb.aqualife.R
import com.urasweb.aqualife.WaterReminderReceiver
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var txtGreeting: TextView
    private lateinit var txtResumenImc: TextView
    private lateinit var txtResumenDatos: TextView
    private lateinit var imcGauge: ImcGaugeView

    private lateinit var txtPerimetro: TextView
    private lateinit var txtRiesgoPerimetro: TextView
    private lateinit var abdGauge: AbdominalRiskGaugeView

    private lateinit var btnVolverAPesar: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtGreeting = view.findViewById(R.id.txtGreeting)
        txtResumenImc = view.findViewById(R.id.txtResumenImc)
        txtResumenDatos = view.findViewById(R.id.txtResumenDatos)
        imcGauge = view.findViewById(R.id.imcGauge)

        txtPerimetro = view.findViewById(R.id.txtPerimetro)
        txtRiesgoPerimetro = view.findViewById(R.id.txtRiesgoPerimetro)
        abdGauge = view.findViewById(R.id.abdGauge)

        btnVolverAPesar = view.findViewById(R.id.btnVolverAPesar)

        // Saludo usando nombre desde Firestore, si existe
        cargarNombreUsuario()

        // Navegar a IMCNewFragment (ajusta el id si tu nav_graph usa otro)
        btnVolverAPesar.setOnClickListener {
            findNavController().navigate(R.id.nav_imc_new)
        }

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val freqMin = prefs.getInt(KEY_FREQ_MINUTOS, 60)

        val perimetro = prefs.getFloat("perimetro_abd", 0f)

        if (perimetro > 0f) {
            val textoPerimetro = "Perímetro abdominal: ${"%.1f".format(perimetro)} cm"
            txtPerimetro.text = textoPerimetro

            val riesgo = when {
                perimetro < 80f -> "Riesgo bajo por grasa abdominal."
                perimetro < 94f -> "Riesgo moderado por grasa abdominal."
                else            -> "Riesgo alto por grasa abdominal."
            }
            txtRiesgoPerimetro.text = riesgo

            txtPerimetro.visibility = View.VISIBLE
            txtRiesgoPerimetro.visibility = View.VISIBLE

            // Actualizar gauge abdominal
            abdGauge.setPerimetro(perimetro)
        } else {
            txtPerimetro.visibility = View.GONE
            txtRiesgoPerimetro.visibility = View.GONE
            abdGauge.setPerimetro(0f)
        }

        val notificacionesPorDia = when (freqMin) {
            30 -> 20
            60 -> 10
            120 -> 5
            else -> 10
        }

        val tieneDatos = prefs.contains(KEY_ALTURA) && prefs.contains(KEY_PESO)

        if (!tieneDatos) {
            findNavController().navigate(R.id.nav_setup)
            return
        }

        val alturaCm = prefs.getFloat(KEY_ALTURA, 0f)
        val pesoKg = prefs.getFloat(KEY_PESO, 0f)
        val fechaNac = prefs.getString(KEY_FECHA, "-")

        val imc = calcularImc(pesoKg, alturaCm)
        val rec = obtenerRecomendacion(imc)

        val litrosObjetivo = (rec.litrosMin + rec.litrosMax) / 2.0
        val mlTotales = litrosObjetivo * 1000.0
        val mlPorNotificacion = mlTotales / notificacionesPorDia

        prefs.edit()
            .putFloat("ml_por_notificacion", mlPorNotificacion.toFloat())
            .apply()

        programarNotificaciones(freqMin)

        val textoImc = """
            IMC: ${"%.2f".format(imc)}
            Categoría: ${rec.categoria}
            Agua recomendada: ${rec.texto}
        """.trimIndent()
        txtResumenImc.text = textoImc

        imcGauge.setImc(imc)

        val textoDatos = """
            Peso: ${"%.1f".format(pesoKg)} kg
            Talla: ${"%.1f".format(alturaCm)} cm
            Fecha nacimiento: $fechaNac
        """.trimIndent()
        txtResumenDatos.text = textoDatos
    }

    private fun cargarNombreUsuario() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            txtGreeting.text = "Hola"
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val nombre = snapshot.getString("nombre")
                txtGreeting.text = if (!nombre.isNullOrBlank()) {
                    "Hola $nombre"
                } else {
                    "Hola"
                }
            }
            .addOnFailureListener {
                txtGreeting.text = "Hola"
            }
    }

    private fun calcularImc(pesoKg: Float, tallaCm: Float): Double {
        val tallaM = tallaCm / 100f
        return (pesoKg / (tallaM * tallaM)).toDouble()
    }

    data class RecomendacionAgua(
        val categoria: String,
        val texto: String,
        val litrosMin: Double,
        val litrosMax: Double
    )

    private fun obtenerRecomendacion(imc: Double): RecomendacionAgua {
        return when {
            imc < 18.5 -> RecomendacionAgua(
                "Bajo peso",
                "1.5 – 2 L (6 – 8 vasos)",
                1.5, 2.0
            )
            imc < 24.9 -> RecomendacionAgua(
                "Normal",
                "2 – 2.5 L (8 – 10 vasos)",
                2.0, 2.5
            )
            imc < 29.9 -> RecomendacionAgua(
                "Sobrepeso",
                "2.5 – 3 L (10 – 12 vasos)",
                2.5, 3.0
            )
            imc < 34.9 -> RecomendacionAgua(
                "Obesidad I",
                "3 – 3.5 L (12 – 14 vasos)",
                3.0, 3.5
            )
            imc < 39.9 -> RecomendacionAgua(
                "Obesidad II",
                "3.5 – 4 L (14 – 16 vasos)",
                3.5, 4.0
            )
            else -> RecomendacionAgua(
                "Obesidad III",
                "4 – 4.5 L (16 – 18 vasos)",
                4.0, 4.5
            )
        }
    }

    private fun programarNotificaciones(freqMin: Int) {
        val context = requireContext()
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)

            if (get(Calendar.HOUR_OF_DAY) >= 19) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intervaloMs = freqMin * 60 * 1000L

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervaloMs,
            pendingIntent
        )
    }

    companion object {
        private const val PREFS_NAME = "imc_prefs"
        private const val KEY_ALTURA = "altura_cm"
        private const val KEY_PESO = "peso_kg"
        private const val KEY_FECHA = "fecha_nac"
        private const val KEY_FREQ_MINUTOS = "60"
    }
}
