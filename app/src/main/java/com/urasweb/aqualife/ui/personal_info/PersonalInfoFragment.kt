package com.urasweb.aqualife.ui.personalinfo

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.urasweb.aqualife.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class PersonalInfoFragment : Fragment() {

    // Layouts / inputs
    private lateinit var layoutNombre: TextInputLayout
    private lateinit var layoutApellido: TextInputLayout
    private lateinit var layoutFechaNac: TextInputLayout

    private lateinit var inputNombre: TextInputEditText
    private lateinit var inputApellido: TextInputEditText
    private lateinit var inputFechaNac: TextInputEditText
    private lateinit var inputMetaAgua: TextInputEditText

    private lateinit var rgSexo: RadioGroup
    private lateinit var rbMasculino: RadioButton
    private lateinit var rbFemenino: RadioButton

    private lateinit var btnGuardar: Button

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personal_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutNombre = view.findViewById(R.id.layoutNombre)
        layoutApellido = view.findViewById(R.id.layoutApellido)
        layoutFechaNac = view.findViewById(R.id.layoutFechaNac)

        inputNombre = view.findViewById(R.id.inputNombre)
        inputApellido = view.findViewById(R.id.inputApellido)
        inputFechaNac = view.findViewById(R.id.inputFechaNac)
        inputMetaAgua = view.findViewById(R.id.inputMetaAgua)

        rgSexo = view.findViewById(R.id.rgSexo)
        rbMasculino = view.findViewById(R.id.rbMasculino)
        rbFemenino = view.findViewById(R.id.rbFemenino)

        btnGuardar = view.findViewById(R.id.btnGuardar)

        // DatePicker para la fecha de nacimiento
        inputFechaNac.setOnClickListener { mostrarDatePicker() }

        // Cargar datos existentes desde Firestore
        cargarDatosUsuario()

        btnGuardar.setOnClickListener {
            guardarDatos()
        }
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        val hoyYear = cal.get(Calendar.YEAR)
        val hoyMonth = cal.get(Calendar.MONTH)
        val hoyDay = cal.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val texto = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                inputFechaNac.setText(texto)
            },
            hoyYear,
            hoyMonth,
            hoyDay
        )

        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun cargarDatosUsuario() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(
                requireContext(),
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(currentUser.uid)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    val nombre = snapshot.getString("nombre")
                    val apellido = snapshot.getString("apellido")
                    val sexo = snapshot.getString("sexo")
                    val fechaNac = snapshot.getDate("fechaNacimiento")
                    val metaMl = snapshot.getLong("metaDiariaMl")

                    inputNombre.setText(nombre ?: "")
                    inputApellido.setText(apellido ?: "")

                    when (sexo) {
                        "M" -> rbMasculino.isChecked = true
                        "F" -> rbFemenino.isChecked = true
                    }

                    fechaNac?.let {
                        inputFechaNac.setText(dateFormat.format(it))
                    }

                    metaMl?.let {
                        inputMetaAgua.setText(it.toString())
                    }
                }

                // Si tenemos peso y talla, recalcular meta y mostrarla
                recalcularMetaYMostrar()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "No se pudo cargar la información personal",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun recalcularMetaYMostrar() {
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val peso = prefs.getFloat(KEY_PESO, 0f)
        val altura = prefs.getFloat(KEY_ALTURA, 0f)

        val fechaStr = inputFechaNac.text?.toString()?.trim()
        val sexo = when (rgSexo.checkedRadioButtonId) {
            rbMasculino.id -> "M"
            rbFemenino.id -> "F"
            else -> null
        }

        if (peso <= 0f || altura <= 0f || fechaStr.isNullOrEmpty() || sexo == null) {
            // No podemos calcular meta todavía
            return
        }

        val fechaNac = try {
            dateFormat.parse(fechaStr)
        } catch (e: ParseException) {
            null
        } ?: return

        val edad = calcularEdad(fechaNac, Date())

        val metaMl = calcularMetaAguaMl(
            pesoKg = peso,
            alturaCm = altura,
            edad = edad,
            sexo = sexo
        )

        inputMetaAgua.setText(metaMl.toString())
    }

    private fun guardarDatos() {
        // Limpia errores
        layoutNombre.error = null
        layoutApellido.error = null
        layoutFechaNac.error = null

        val nombre = inputNombre.text?.toString()?.trim()
        val apellido = inputApellido.text?.toString()?.trim()
        val fechaStr = inputFechaNac.text?.toString()?.trim()

        var hayError = false

        if (nombre.isNullOrEmpty()) {
            layoutNombre.error = "Ingresa tu nombre"
            hayError = true
        }
        if (apellido.isNullOrEmpty()) {
            layoutApellido.error = "Ingresa tu apellido"
            hayError = true
        }
        if (rgSexo.checkedRadioButtonId == -1) {
            Toast.makeText(
                requireContext(),
                "Selecciona tu sexo",
                Toast.LENGTH_SHORT
            ).show()
            hayError = true
        }
        if (fechaStr.isNullOrEmpty()) {
            layoutFechaNac.error = "Ingresa tu fecha de nacimiento"
            hayError = true
        }

        val fechaNac: Date? = if (!fechaStr.isNullOrEmpty()) {
            try {
                dateFormat.parse(fechaStr)
            } catch (e: ParseException) {
                layoutFechaNac.error = "Usa el formato dd/MM/aaaa"
                hayError = true
                null
            }
        } else null

        if (hayError || fechaNac == null) {
            return
        }

        val sexo = when (rgSexo.checkedRadioButtonId) {
            rbMasculino.id -> "M"
            rbFemenino.id -> "F"
            else -> "U"
        }

        // Leemos peso y talla de preferencias (definidos en Setup)
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val peso = prefs.getFloat(KEY_PESO, 0f)
        val altura = prefs.getFloat(KEY_ALTURA, 0f)

        if (peso <= 0f || altura <= 0f) {
            Toast.makeText(
                requireContext(),
                "Configura primero tu peso y talla en la sección inicial",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val edad = calcularEdad(fechaNac, Date())
        val metaMl = calcularMetaAguaMl(
            pesoKg = peso,
            alturaCm = altura,
            edad = edad,
            sexo = sexo
        )

        inputMetaAgua.setText(metaMl.toString())

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(
                requireContext(),
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(currentUser.uid)

        val data = hashMapOf<String, Any>(
            "uid" to currentUser.uid,
            "nombre" to nombre!!,
            "apellido" to apellido!!,
            "sexo" to sexo,
            "fechaNacimiento" to fechaNac,
            "metaDiariaMl" to metaMl,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                // También guardamos meta en prefs por si se usa offline
                prefs.edit()
                    .putFloat(KEY_META_ML, metaMl.toFloat())
                    .apply()

                Toast.makeText(
                    requireContext(),
                    "Información personal actualizada",
                    Toast.LENGTH_SHORT
                ).show()

                // Cerramos pantalla
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al actualizar: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun calcularEdad(fechaNac: Date, hoy: Date): Int {
        val calNac = Calendar.getInstance().apply { time = fechaNac }
        val calHoy = Calendar.getInstance().apply { time = hoy }

        var edad = calHoy.get(Calendar.YEAR) - calNac.get(Calendar.YEAR)
        if (calHoy.get(Calendar.DAY_OF_YEAR) < calNac.get(Calendar.DAY_OF_YEAR)) {
            edad--
        }
        return edad
    }

    /**
     * Fórmula para meta diaria de agua (ml), basada en:
     * - Peso (kg)
     * - Altura (cm)
     * - Edad (años)
     * - Sexo ("M" / "F")
     *
     * Base: factor por kg según edad, con ajuste por sexo y altura.
     */
    private fun calcularMetaAguaMl(
        pesoKg: Float,
        alturaCm: Float,
        edad: Int,
        sexo: String
    ): Int {
        // Factor por edad (ml/kg)
        var factor = when {
            edad < 30 -> 40f
            edad <= 55 -> 35f
            else -> 30f
        }

        // Ajuste ligero por sexo
        if (sexo == "F") {
            factor -= 2f
        }

        var ml = pesoKg * factor

        // Ajuste por altura
        if (alturaCm > 180f) {
            ml += 250f
        } else if (alturaCm < 155f) {
            ml -= 250f
        }

        // Meta mínima 1500 ml
        ml = max(ml, 1500f)

        return ml.roundToInt()
    }

    companion object {
        private const val PREFS_NAME = "imc_prefs"
        private const val KEY_ALTURA = "altura_cm"
        private const val KEY_PESO = "peso_kg"
        private const val KEY_META_ML = "meta_diaria_ml"
    }
}
