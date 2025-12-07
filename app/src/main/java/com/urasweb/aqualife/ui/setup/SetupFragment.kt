package com.urasweb.aqualife.ui.setup

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.urasweb.aqualife.R
import com.urasweb.aqualife.data.repository.AquaRepository
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SetupFragment : Fragment() {

    // Inputs
    private lateinit var layoutAltura: TextInputLayout
    private lateinit var layoutPeso: TextInputLayout
    private lateinit var layoutFechaNac: TextInputLayout

    private lateinit var inputAltura: TextInputEditText
    private lateinit var inputPeso: TextInputEditText
    private lateinit var inputFechaNac: TextInputEditText

    // Sexo
    private lateinit var rgSexo: RadioGroup
    private lateinit var rbMasculino: RadioButton
    private lateinit var rbFemenino: RadioButton

    // Botón
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

        // Layouts
        layoutAltura = view.findViewById(R.id.layoutAltura)
        layoutPeso = view.findViewById(R.id.layoutPeso)
        layoutFechaNac = view.findViewById(R.id.layoutFechaNac)

        // Inputs
        inputAltura = view.findViewById(R.id.inputAltura)
        inputPeso = view.findViewById(R.id.inputPeso)
        inputFechaNac = view.findViewById(R.id.inputFechaNac)

        // Sexo
        rgSexo = view.findViewById(R.id.rgSexo)
        rbMasculino = view.findViewById(R.id.rbMasculino)
        rbFemenino = view.findViewById(R.id.rbFemenino)

        // Botón
        btnGuardar = view.findViewById(R.id.btnGuardar)

        // ─────────────────────────────────────────────
        // PRECARGAR DATOS SI YA EXISTEN EN PREFERENCIAS
        // ─────────────────────────────────────────────
        val prefs = requireContext()
            .getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)

        val alturaGuardada = prefs.getFloat("altura_cm", 0f)
        if (alturaGuardada > 0f) {
            inputAltura.setText(alturaGuardada.toString())
        }

        val pesoGuardado = prefs.getFloat("peso_kg", 0f)
        if (pesoGuardado > 0f) {
            inputPeso.setText(pesoGuardado.toString())
        }

        val fechaGuardada = prefs.getString("fecha_nac", null)
        if (!fechaGuardada.isNullOrEmpty()) {
            inputFechaNac.setText(fechaGuardada)
        }

        when (prefs.getString("sexo", null)) {
            "M" -> rbMasculino.isChecked = true
            "F" -> rbFemenino.isChecked = true
        }
        // ─────────────────────────────────────────────

        // DatePicker para la fecha
        inputFechaNac.setOnClickListener {
            mostrarDatePicker()
        }

        btnGuardar.setOnClickListener {
            guardarDatosYVolverAlDashboard()
        }
    }

    private fun mostrarDatePicker() {
        val hoy = Calendar.getInstance()
        val year = hoy.get(Calendar.YEAR)
        val month = hoy.get(Calendar.MONTH)
        val day = hoy.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val texto = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)
                inputFechaNac.setText(texto)
            },
            year,
            month,
            day
        )
        dialog.datePicker.maxDate = hoy.timeInMillis
        dialog.show()
    }

    private fun guardarDatosYVolverAlDashboard() {
        // Limpia errores previos
        layoutAltura.error = null
        layoutPeso.error = null
        layoutFechaNac.error = null

        val alturaStr = inputAltura.text?.toString()?.trim()
        val pesoStr = inputPeso.text?.toString()?.trim()
        val fechaStr = inputFechaNac.text?.toString()?.trim()

        var alturaCm: Float? = null
        var pesoKg: Float? = null
        var fechaNacDate: Date? = null

        var hayError = false
        var primerCampoConError: View? = null

        // ------ Validar ALTURA ------
        if (alturaStr.isNullOrEmpty()) {
            layoutAltura.error = "Ingresa tu altura"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputAltura
        } else {
            try {
                alturaCm = alturaStr.replace(",", ".").toFloat()
                if (alturaCm !in ALTURA_MIN..ALTURA_MAX) {
                    layoutAltura.error = "La altura debe estar entre $ALTURA_MIN y $ALTURA_MAX cm"
                    hayError = true
                    primerCampoConError = primerCampoConError ?: inputAltura
                }
            } catch (e: NumberFormatException) {
                layoutAltura.error = "La altura debe ser un número válido"
                hayError = true
                primerCampoConError = primerCampoConError ?: inputAltura
            }
        }

        // ------ Validar PESO ------
        if (pesoStr.isNullOrEmpty()) {
            layoutPeso.error = "Ingresa tu peso"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputPeso
        } else {
            try {
                pesoKg = pesoStr.replace(",", ".").toFloat()
                if (pesoKg !in PESO_MIN..PESO_MAX) {
                    layoutPeso.error = "El peso debe estar entre $PESO_MIN y $PESO_MAX kg"
                    hayError = true
                    primerCampoConError = primerCampoConError ?: inputPeso
                }
            } catch (e: NumberFormatException) {
                layoutPeso.error = "El peso debe ser un número válido"
                hayError = true
                primerCampoConError = primerCampoConError ?: inputPeso
            }
        }

        // ------ Validar FECHA ------
        if (fechaStr.isNullOrEmpty()) {
            layoutFechaNac.error = "Ingresa tu fecha de nacimiento"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputFechaNac
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            try {
                fechaNacDate = sdf.parse(fechaStr)
                val hoy = Date()

                if (fechaNacDate == null) {
                    layoutFechaNac.error = "Fecha inválida"
                    hayError = true
                    primerCampoConError = primerCampoConError ?: inputFechaNac
                } else if (fechaNacDate!!.after(hoy)) {
                    layoutFechaNac.error = "La fecha no puede ser mayor a hoy"
                    hayError = true
                    primerCampoConError = primerCampoConError ?: inputFechaNac
                } else {
                    val edad = calcularEdad(fechaNacDate!!, hoy)
                    if (edad !in EDAD_MIN..EDAD_MAX) {
                        layoutFechaNac.error =
                            "La edad debe estar entre $EDAD_MIN y $EDAD_MAX años"
                        hayError = true
                        primerCampoConError = primerCampoConError ?: inputFechaNac
                    }
                }
            } catch (e: ParseException) {
                layoutFechaNac.error = "Usa el formato dd/MM/aaaa"
                hayError = true
                primerCampoConError = primerCampoConError ?: inputFechaNac
            }
        }

        // ------ Validar SEXO ------
        if (rgSexo.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Selecciona tu sexo", Toast.LENGTH_SHORT).show()
            if (!hayError) {
                hayError = true
                primerCampoConError = rgSexo
            }
        }

        if (hayError) {
            primerCampoConError?.requestFocus()
            Toast.makeText(
                requireContext(),
                "Por favor corrige los campos marcados",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Datos válidos
        val alturaRedondeada = redondear2Dec(alturaCm!!)
        val pesoRedondeado = redondear2Dec(pesoKg!!)
        val sexo = when (rgSexo.checkedRadioButtonId) {
            rbMasculino.id -> "M"
            rbFemenino.id -> "F"
            else -> "U"
        }
        val fechaMillis = fechaNacDate!!.time

        // 1) Guardar en SharedPreferences (incluye bandera de Setup)
        val prefs = requireContext().getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("altura_cm", alturaRedondeada)
            .putFloat("peso_kg", pesoRedondeado)
            .putString("fecha_nac", fechaStr)
            .putString("sexo", sexo)
            .putBoolean("setup_completed", true)
            .apply()

        // 2) Guardar registro IMC inicial en Room y marcar para sync
        lifecycleScope.launch {
            try {
                AquaRepository.saveInitialSetupImc(
                    alturaCm = alturaRedondeada,
                    pesoKg = pesoRedondeado,
                    perimetroAbdominalCm = 0.0
                )

                Toast.makeText(
                    requireContext(),
                    "Datos guardados correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir al Dashboard/Home
                findNavController().navigate(R.id.nav_home)
            } catch (e: IllegalStateException) {
                Toast.makeText(
                    requireContext(),
                    "Error: usuario no inicializado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun redondear2Dec(valor: Float): Float {
        return kotlin.math.round(valor * 100f) / 100f
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

    companion object {
        private const val ALTURA_MIN = 100f
        private const val ALTURA_MAX = 250f
        private const val PESO_MIN = 30f
        private const val PESO_MAX = 250f
        private const val EDAD_MIN = 5
        private const val EDAD_MAX = 120
    }
}
