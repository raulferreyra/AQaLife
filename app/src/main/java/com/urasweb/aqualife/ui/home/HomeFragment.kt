package com.urasweb.aqualife.ui.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.urasweb.aqualife.R
import com.urasweb.aqualife.WaterReminderReceiver
import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.local.ImcRecordEntity
import com.urasweb.aqualife.data.repository.AquaRepository
import java.util.Calendar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var txtGreeting: TextView
    private lateinit var txtResumenImc: TextView
    private lateinit var txtResumenDatos: TextView
    private lateinit var imcGauge: ImcGaugeView

    private lateinit var txtPerimetro: TextView
    private lateinit var txtRiesgoPerimetro: TextView
    private lateinit var abdGauge: AbdominalRiskGaugeView

    private lateinit var txtHistoricoTitle: TextView
    private lateinit var imcHistoryChart: ImcHistoryChartView

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

        // Views
        txtGreeting = view.findViewById(R.id.txtGreeting)
        txtResumenImc = view.findViewById(R.id.txtResumenImc)
        txtResumenDatos = view.findViewById(R.id.txtResumenDatos)
        imcGauge = view.findViewById(R.id.imcGauge)

        txtPerimetro = view.findViewById(R.id.txtPerimetro)
        txtRiesgoPerimetro = view.findViewById(R.id.txtRiesgoPerimetro)
        abdGauge = view.findViewById(R.id.abdGauge)

        txtHistoricoTitle = view.findViewById(R.id.txtHistoricoTitle)
        imcHistoryChart = view.findViewById(R.id.imcHistoryChart)

        btnVolverAPesar = view.findViewById(R.id.btnVolverAPesar)

        // Saludo
        cargarNombreUsuario()

        // Botón para ir a IMCNewFragment
        btnVolverAPesar.setOnClickListener {
            findNavController().navigate(R.id.nav_imc_new)
        }

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val freqMin = prefs.getInt(KEY_FREQ_MINUTOS, 60)

        // Primero cargamos las unidades desde Firestore a prefs
        cargarUnidadesEnPrefs(prefs) {
            // Luego, ya con unidades disponibles, cargamos historial y renderizamos dashboard
            viewLifecycleOwner.lifecycleScope.launch {
                actualizarDesdeHistorialImc(prefs)
                renderizarDashboard(prefs, freqMin)
            }
        }
    }

    // --------------------------------------------------------------------
    // Unidades: leer de Firestore y guardar en SharedPreferences
    // --------------------------------------------------------------------
    private fun cargarUnidadesEnPrefs(prefs: SharedPreferences, onComplete: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            // Si no hay usuario, dejamos métricas por defecto
            prefs.edit()
                .putString(KEY_LENGTH_UNIT, "cm")
                .putString(KEY_WEIGHT_UNIT, "kg")
                .putString(KEY_VOLUME_UNIT, "ml")
                .apply()
            onComplete()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("units")
            .get()
            .addOnSuccessListener { snapshot ->
                val lengthUnit = snapshot.getString("lengthUnit") ?: "cm"
                val weightUnit = snapshot.getString("weightUnit") ?: "kg"
                val volumeUnit = snapshot.getString("volumeUnit") ?: "ml"

                prefs.edit()
                    .putString(KEY_LENGTH_UNIT, lengthUnit)
                    .putString(KEY_WEIGHT_UNIT, weightUnit)
                    .putString(KEY_VOLUME_UNIT, volumeUnit)
                    .apply()

                onComplete()
            }
            .addOnFailureListener {
                // Si falla, aseguramos defaults si no existían
                if (!prefs.contains(KEY_LENGTH_UNIT)) {
                    prefs.edit()
                        .putString(KEY_LENGTH_UNIT, "cm")
                        .putString(KEY_WEIGHT_UNIT, "kg")
                        .putString(KEY_VOLUME_UNIT, "ml")
                        .apply()
                }
                onComplete()
            }
    }

    // --------------------------------------------------------------------
    // 1) Cargar nombre desde Firestore para "Hola %Nombre%"
    // --------------------------------------------------------------------
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

    // --------------------------------------------------------------------
    // 2) Leer últimos 5 registros de IMC desde Room y actualizar prefs
    //    + gráfico histórico (aplicando máscaras)
    // --------------------------------------------------------------------
    private suspend fun actualizarDesdeHistorialImc(prefs: SharedPreferences) {
        try {
            val appContext = requireContext().applicationContext
            AquaDatabase.init(appContext)
            val db = AquaDatabase.getInstance()
            AquaRepository.init(db)

            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user == null) {
                txtHistoricoTitle.visibility = View.GONE
                imcHistoryChart.visibility = View.GONE
                return
            }

            AquaRepository.setCurrentUser(user.uid)

            val records: List<ImcRecordEntity> =
                AquaRepository.getLastImcRecordsForCurrentUser(limit = 5)

            if (records.isEmpty()) {
                txtHistoricoTitle.visibility = View.GONE
                imcHistoryChart.visibility = View.GONE
                return
            }

            val ordered = records.sortedBy { it.updatedAt }
            val last = ordered.last()

            val pesoActualMetric = last.pesoKg.toFloat()
            val tallaActualCm = (last.tallaM * 100f).toFloat()
            val perimetroActualMetric = last.perimetroAbdominalCm.toFloat()

            // Actualizar prefs con el último registro (siempre en métrico)
            prefs.edit()
                .putFloat(KEY_PESO, pesoActualMetric)
                .putFloat(KEY_ALTURA, tallaActualCm)
                .putFloat(KEY_PERIMETRO_ABD, perimetroActualMetric)
                .apply()

            // Datos en métrico
            val pesosMetric = ordered.map { it.pesoKg.toFloat() }
            val perimetrosMetric = ordered.map { it.perimetroAbdominalCm.toFloat() }

            // Aplicar máscaras según unidades guardadas
            val lengthUnit = prefs.getString(KEY_LENGTH_UNIT, "cm") ?: "cm"
            val weightUnit = prefs.getString(KEY_WEIGHT_UNIT, "kg") ?: "kg"

            val pesosDisplay = pesosMetric.map { peso ->
                if (weightUnit == "lb") {
                    (peso * 2.20462f)
                } else {
                    peso
                }
            }

            val perimetrosDisplay = perimetrosMetric.map { per ->
                if (lengthUnit == "ft") {
                    (per / 30.48f)
                } else {
                    per
                }
            }

            imcHistoryChart.setData(pesosDisplay, perimetrosDisplay)
            txtHistoricoTitle.visibility = View.VISIBLE
            imcHistoryChart.visibility = View.VISIBLE

        } catch (e: Exception) {
            // Si falla el historial, lo ocultamos y seguimos con el Dashboard
            txtHistoricoTitle.visibility = View.GONE
            imcHistoryChart.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "No se pudo cargar el histórico de IMC",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --------------------------------------------------------------------
    // 3) Renderizar Dashboard con datos en SharedPreferences (máscaras)
    // --------------------------------------------------------------------
    private fun renderizarDashboard(prefs: SharedPreferences, freqMin: Int) {
        val lengthUnit = prefs.getString(KEY_LENGTH_UNIT, "cm") ?: "cm"
        val weightUnit = prefs.getString(KEY_WEIGHT_UNIT, "kg") ?: "kg"

        // PERÍMETRO ABDOMINAL + GAUGE
        val perimetroMetric = prefs.getFloat(KEY_PERIMETRO_ABD, 0f)

        if (perimetroMetric > 0f) {
            // Para texto aplicamos máscara
            val perimetroDisplay: Float
            val perimetroUnitLabel: String
            if (lengthUnit == "ft") {
                perimetroDisplay = perimetroMetric / 30.48f
                perimetroUnitLabel = "ft"
            } else {
                perimetroDisplay = perimetroMetric
                perimetroUnitLabel = "cm"
            }

            val textoPerimetro =
                "Perímetro abdominal: ${"%.1f".format(perimetroDisplay)} $perimetroUnitLabel"
            txtPerimetro.text = textoPerimetro

            // El riesgo se calcula SIEMPRE en cm (métrico)
            val riesgo = when {
                perimetroMetric < 80f -> "Riesgo bajo por grasa abdominal."
                perimetroMetric < 94f -> "Riesgo moderado por grasa abdominal."
                else -> "Riesgo alto por grasa abdominal."
            }
            txtRiesgoPerimetro.text = riesgo

            txtPerimetro.visibility = View.VISIBLE
            txtRiesgoPerimetro.visibility = View.VISIBLE

            // Gauge en escala métrica
            abdGauge.setPerimetro(perimetroMetric)
        } else {
            txtPerimetro.visibility = View.GONE
            txtRiesgoPerimetro.visibility = View.GONE
            abdGauge.setPerimetro(0f)
        }

        // Notificaciones por día (config antigua, la mantenemos)
        val notificacionesPorDia = when (freqMin) {
            30 -> 20
            60 -> 10
            120 -> 5
            else -> 10
        }

        // Validar que haya datos básicos
        val tieneDatos = prefs.contains(KEY_ALTURA) && prefs.contains(KEY_PESO)
        if (!tieneDatos) {
            findNavController().navigate(R.id.nav_setup)
            return
        }

        val alturaCm = prefs.getFloat(KEY_ALTURA, 0f)
        val pesoKgMetric = prefs.getFloat(KEY_PESO, 0f)
        val fechaNac = prefs.getString(KEY_FECHA, "-")

        // Cálculo de IMC SIEMPRE en métrico
        val imc = calcularImc(pesoKgMetric, alturaCm)
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

        // Máscaras para peso y talla en el texto
        val pesoDisplay: Float
        val pesoUnitLabel: String
        if (weightUnit == "lb") {
            pesoDisplay = pesoKgMetric * 2.20462f
            pesoUnitLabel = "lb"
        } else {
            pesoDisplay = pesoKgMetric
            pesoUnitLabel = "kg"
        }

        val alturaDisplay: Float
        val alturaUnitLabel: String
        if (lengthUnit == "ft") {
            alturaDisplay = alturaCm / 30.48f
            alturaUnitLabel = "ft"
        } else {
            alturaDisplay = alturaCm
            alturaUnitLabel = "cm"
        }

        val textoDatos = """
            Peso: ${"%.1f".format(pesoDisplay)} $pesoUnitLabel
            Talla: ${"%.1f".format(alturaDisplay)} $alturaUnitLabel
            Fecha nacimiento: $fechaNac
        """.trimIndent()
        txtResumenDatos.text = textoDatos
    }

    // --------------------------------------------------------------------
    // 4) Lógica de IMC / Agua / Notificaciones (igual que antes)
    // --------------------------------------------------------------------
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
        private const val KEY_PERIMETRO_ABD = "perimetro_abd"

        // Unidades guardadas como "máscaras"
        private const val KEY_LENGTH_UNIT = "units_length"
        private const val KEY_WEIGHT_UNIT = "units_weight"
        private const val KEY_VOLUME_UNIT = "units_volume"
    }
}
