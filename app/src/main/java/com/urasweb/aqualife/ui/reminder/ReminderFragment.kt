package com.urasweb.aqualife.ui.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.urasweb.aqualife.R
import com.urasweb.aqualife.WaterReminderReceiver
import com.urasweb.aqualife.WeightReminderReceiver
import java.util.Calendar

class ReminderFragment : Fragment() {

    // UI Agua
    private lateinit var rgAgua: RadioGroup
    private lateinit var rbAgua30: RadioButton
    private lateinit var rbAgua60: RadioButton
    private lateinit var rbAgua120: RadioButton
    private lateinit var checkAguaEnabled: CheckBox

    // UI Peso
    private lateinit var rgPeso: RadioGroup
    private lateinit var rbPeso7: RadioButton
    private lateinit var rbPeso15: RadioButton
    private lateinit var rbPeso30: RadioButton
    private lateinit var checkPesoEnabled: CheckBox

    // UI Horario
    private lateinit var txtHoraInicio: TextView
    private lateinit var txtHoraFin: TextView
    private lateinit var btnCambiarInicio: Button
    private lateinit var btnCambiarFin: Button
    private lateinit var btnGuardar: Button

    // Valores en memoria (por defecto)
    private var horaInicio: Int = 9
    private var minutoInicio: Int = 0
    private var horaFin: Int = 22
    private var minutoFin: Int = 0

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias UI
        rgAgua = view.findViewById(R.id.rgReminderAgua)
        rbAgua30 = view.findViewById(R.id.rbAgua30)
        rbAgua60 = view.findViewById(R.id.rbAgua60)
        rbAgua120 = view.findViewById(R.id.rbAgua120)
        checkAguaEnabled = view.findViewById(R.id.checkAguaEnabled)

        rgPeso = view.findViewById(R.id.rgReminderPeso)
        rbPeso7 = view.findViewById(R.id.rbPeso7)
        rbPeso15 = view.findViewById(R.id.rbPeso15)
        rbPeso30 = view.findViewById(R.id.rbPeso30)
        checkPesoEnabled = view.findViewById(R.id.checkPesoEnabled)

        txtHoraInicio = view.findViewById(R.id.txtHoraInicio)
        txtHoraFin = view.findViewById(R.id.txtHoraFin)
        btnCambiarInicio = view.findViewById(R.id.btnCambiarHoraInicio)
        btnCambiarFin = view.findViewById(R.id.btnCambiarHoraFin)
        btnGuardar = view.findViewById(R.id.btnGuardarReminders)

        // Valores por defecto en UI
        aplicarValoresPorDefectoUI()

        // Listeners
        btnCambiarInicio.setOnClickListener { mostrarTimePicker(true) }
        btnCambiarFin.setOnClickListener { mostrarTimePicker(false) }
        btnGuardar.setOnClickListener { guardarEnFirestoreYProgramarAlarmas() }

        // Cargar lo que haya en Firestore (si no hay, se quedan los defaults de UI)
        cargarDesdeFirestore()
    }

    private fun aplicarValoresPorDefectoUI() {
        // Agua: 60 minutos, activado
        rbAgua60.isChecked = true
        checkAguaEnabled.isChecked = true

        // Peso: 7 días, activado
        rbPeso7.isChecked = true
        checkPesoEnabled.isChecked = true

        // Horario 09:00 - 22:00
        horaInicio = 9
        minutoInicio = 0
        horaFin = 22
        minutoFin = 0
        actualizarTextosHora()
    }

    private fun mostrarTimePicker(esInicio: Boolean) {
        val horaActual = if (esInicio) horaInicio else horaFin
        val minutoActual = if (esInicio) minutoInicio else minutoFin

        val dialog = TimePickerDialog(
            requireContext(),
            { _, h, m ->
                if (esInicio) {
                    horaInicio = h
                    minutoInicio = m
                } else {
                    horaFin = h
                    minutoFin = m
                }
                actualizarTextosHora()
            },
            horaActual,
            minutoActual,
            true
        )
        dialog.show()
    }

    private fun actualizarTextosHora() {
        txtHoraInicio.text = String.format("%02d:%02d", horaInicio, minutoInicio)
        txtHoraFin.text = String.format("%02d:%02d", horaFin, minutoFin)
    }

    private fun cargarDesdeFirestore() {
        val user = auth.currentUser ?: return

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("reminders")

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // No hay doc: mantenemos los defaults en UI
                    return@addOnSuccessListener
                }

                // Agua
                val aguaEnabled = snapshot.getBoolean("aguaEnabled") ?: true
                val aguaIntervalo = snapshot.getLong("aguaIntervaloMinutos")?.toInt() ?: 60

                checkAguaEnabled.isChecked = aguaEnabled
                when (aguaIntervalo) {
                    30 -> rbAgua30.isChecked = true
                    120 -> rbAgua120.isChecked = true
                    else -> rbAgua60.isChecked = true
                }

                // Peso
                val pesoEnabled = snapshot.getBoolean("pesoEnabled") ?: true
                val pesoFrecuenciaDias = snapshot.getLong("pesoFrecuenciaDias")?.toInt() ?: 7

                checkPesoEnabled.isChecked = pesoEnabled
                when (pesoFrecuenciaDias) {
                    15 -> rbPeso15.isChecked = true
                    30 -> rbPeso30.isChecked = true
                    else -> rbPeso7.isChecked = true
                }

                // Horario
                horaInicio = snapshot.getLong("horaInicio")?.toInt() ?: 9
                minutoInicio = snapshot.getLong("minutoInicio")?.toInt() ?: 0
                horaFin = snapshot.getLong("horaFin")?.toInt() ?: 22
                minutoFin = snapshot.getLong("minutoFin")?.toInt() ?: 0

                actualizarTextosHora()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "No se pudo cargar recordatorios: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun guardarEnFirestoreYProgramarAlarmas() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Agua
        val intervaloAgua = when (rgAgua.checkedRadioButtonId) {
            R.id.rbAgua30 -> 30
            R.id.rbAgua120 -> 120
            else -> 60
        }

        // Peso
        val frecuenciaPesoDias = when (rgPeso.checkedRadioButtonId) {
            R.id.rbPeso15 -> 15
            R.id.rbPeso30 -> 30
            else -> 7
        }

        val aguaEnabled = checkAguaEnabled.isChecked
        val pesoEnabled = checkPesoEnabled.isChecked

        val data = hashMapOf(
            "aguaEnabled" to aguaEnabled,
            "aguaIntervaloMinutos" to intervaloAgua,
            "pesoEnabled" to pesoEnabled,
            "pesoFrecuenciaDias" to frecuenciaPesoDias,
            "horaInicio" to horaInicio,
            "minutoInicio" to minutoInicio,
            "horaFin" to horaFin,
            "minutoFin" to minutoFin,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("reminders")

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                // Guardamos también en SharedPreferences para que los receivers puedan leer
                guardarEnPrefs(
                    aguaEnabled = aguaEnabled,
                    intervaloAguaMin = intervaloAgua,
                    pesoEnabled = pesoEnabled,
                    frecuenciaPesoDias = frecuenciaPesoDias,
                    horaInicio = horaInicio,
                    minutoInicio = minutoInicio,
                    horaFin = horaFin,
                    minutoFin = minutoFin
                )

                // Programar / cancelar alarmas de sistema
                programarAlarmaAgua(aguaEnabled, intervaloAgua)
                programarAlarmaPeso(pesoEnabled, frecuenciaPesoDias)

                Toast.makeText(
                    requireContext(),
                    "Recordatorios guardados",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al guardar recordatorios: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun guardarEnPrefs(
        aguaEnabled: Boolean,
        intervaloAguaMin: Int,
        pesoEnabled: Boolean,
        frecuenciaPesoDias: Int,
        horaInicio: Int,
        minutoInicio: Int,
        horaFin: Int,
        minutoFin: Int
    ) {
        val prefs = requireContext().getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_AGUA_ENABLED, aguaEnabled)
            .putInt(KEY_AGUA_INTERVALO_MIN, intervaloAguaMin)
            .putBoolean(KEY_PESO_ENABLED, pesoEnabled)
            .putInt(KEY_PESO_FREQ_DIAS, frecuenciaPesoDias)
            .putInt(KEY_HORA_INICIO, horaInicio)
            .putInt(KEY_MINUTO_INICIO, minutoInicio)
            .putInt(KEY_HORA_FIN, horaFin)
            .putInt(KEY_MINUTO_FIN, minutoFin)
            .apply()
    }

    // --------------------------------------------------------------------
    // Programación de alarmas
    // --------------------------------------------------------------------

    private fun programarAlarmaAgua(aguaEnabled: Boolean, intervaloMin: Int) {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_AGUA,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!aguaEnabled) {
            // Cancelar si está desactivado
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, horaInicio)
            set(Calendar.MINUTE, minutoInicio)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, se programa para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intervaloMs = intervaloMin * 60 * 1000L

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervaloMs,
            pendingIntent
        )
    }

    private fun programarAlarmaPeso(pesoEnabled: Boolean, frecuenciaDias: Int) {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WeightReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PESO,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!pesoEnabled) {
            // Cancelar si está desactivado
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, horaInicio)
            set(Calendar.MINUTE, minutoInicio)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, que sea a partir de mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intervaloMs = frecuenciaDias * 24L * 60L * 60L * 1000L

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervaloMs,
            pendingIntent
        )
    }

    companion object {
        // SharedPreferences para recordatorios
        private const val PREFS_REMINDERS = "reminder_prefs"
        private const val KEY_AGUA_ENABLED = "aguaEnabled"
        private const val KEY_AGUA_INTERVALO_MIN = "aguaIntervaloMinutos"
        private const val KEY_PESO_ENABLED = "pesoEnabled"
        private const val KEY_PESO_FREQ_DIAS = "pesoFrecuenciaDias"
        private const val KEY_HORA_INICIO = "horaInicio"
        private const val KEY_MINUTO_INICIO = "minutoInicio"
        private const val KEY_HORA_FIN = "horaFin"
        private const val KEY_MINUTO_FIN = "minutoFin"

        // Request codes para PendingIntent
        private const val REQUEST_CODE_AGUA = 1001
        private const val REQUEST_CODE_PESO = 1002
    }
}
