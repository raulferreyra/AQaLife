package com.urasweb.aqualife.ui.reminders

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
import com.urasweb.aqualife.R
import java.util.Calendar

class ReminderFragment : Fragment() {

    private lateinit var rgAgua: RadioGroup
    private lateinit var rbAgua30: RadioButton
    private lateinit var rbAgua60: RadioButton
    private lateinit var rbAgua120: RadioButton
    private lateinit var checkAguaEnabled: CheckBox

    private lateinit var rgPeso: RadioGroup
    private lateinit var rbPeso7: RadioButton
    private lateinit var rbPeso15: RadioButton
    private lateinit var rbPeso30: RadioButton
    private lateinit var checkPesoEnabled: CheckBox

    private lateinit var txtHoraInicio: TextView
    private lateinit var txtHoraFin: TextView
    private lateinit var btnCambiarInicio: Button
    private lateinit var btnCambiarFin: Button
    private lateinit var btnGuardar: Button

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

        btnCambiarInicio.setOnClickListener { mostrarTimePicker(true) }
        btnCambiarFin.setOnClickListener { mostrarTimePicker(false) }
        btnGuardar.setOnClickListener { guardarReminders() }

        cargarDesdeFirestore()
        actualizarTextosHora()
    }

    private fun mostrarTimePicker(esInicio: Boolean) {
        val cal = Calendar.getInstance()
        val hora = if (esInicio) horaInicio else horaFin
        val minuto = if (esInicio) minutoInicio else minutoFin

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
            hora,
            minuto,
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

        val remindersRef =
            db.collection("users").document(user.uid).collection("reminders")

        // Recordatorio de agua
        remindersRef.document("agua").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val intervalo = snapshot.getLong("intervaloMinutos")?.toInt() ?: 60
                    when (intervalo) {
                        30 -> rbAgua30.isChecked = true
                        120 -> rbAgua120.isChecked = true
                        else -> rbAgua60.isChecked = true
                    }
                    val enabled = snapshot.getBoolean("enabled") ?: true
                    checkAguaEnabled.isChecked = enabled

                    snapshot.getLong("horaInicio")?.toInt()?.let { horaInicio = it }
                    snapshot.getLong("horaFin")?.toInt()?.let { horaFin = it }
                    actualizarTextosHora()
                }
            }

        // Recordatorio de peso
        remindersRef.document("peso").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val frecuencia = snapshot.getString("frecuenciaPeso") ?: "semanal"
                    when (frecuencia) {
                        "quincenal" -> rbPeso15.isChecked = true
                        "mensual" -> rbPeso30.isChecked = true
                        else -> rbPeso7.isChecked = true
                    }
                    val enabled = snapshot.getBoolean("enabled") ?: false
                    checkPesoEnabled.isChecked = enabled
                }
            }
    }

    private fun guardarReminders() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val remindersRef =
            db.collection("users").document(user.uid).collection("reminders")

        // Agua
        val intervaloAgua = when (rgAgua.checkedRadioButtonId) {
            R.id.rbAgua30 -> 30
            R.id.rbAgua120 -> 120
            else -> 60
        }

        val aguaData = hashMapOf(
            "tipo" to "agua",
            "intervaloMinutos" to intervaloAgua,
            "enabled" to checkAguaEnabled.isChecked,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // Peso
        val frecuenciaPeso = when (rgPeso.checkedRadioButtonId) {
            R.id.rbPeso15 -> "quincenal"
            R.id.rbPeso30 -> "mensual"
            else -> "semanal"
        }

        val pesoData = hashMapOf(
            "tipo" to "peso",
            "frecuenciaPeso" to frecuenciaPeso,
            "enabled" to checkPesoEnabled.isChecked,
            "horaInicio" to horaInicio,
            "horaFin" to horaFin,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        remindersRef.document("agua").set(aguaData)
            .addOnSuccessListener {
                remindersRef.document("peso").set(pesoData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Recordatorios guardados",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error guardando recordatorio de peso: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error guardando recordatorio de agua: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
