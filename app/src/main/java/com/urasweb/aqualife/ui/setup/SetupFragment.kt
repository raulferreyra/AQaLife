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
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.urasweb.aqualife.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [SetupFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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
                // dd/MM/yyyy
                val texto = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)
                inputFechaNac.setText(texto)
            },
            year,
            month,
            day
        )

        // Opcional: limitar fechas (ej. desde 1900 hasta hoy)
        dialog.datePicker.maxDate = hoy.timeInMillis

        dialog.show()
    }

    private fun guardarDatosYVolverAlDashboard() {
        // Limpiar errores previos
        layoutAltura.error = null
        layoutPeso.error = null
        layoutFechaNac.error = null

        var hayError = false
        var primerCampoConError: View? = null

        val alturaStr = inputAltura.text?.toString()?.trim()
        val pesoStr = inputPeso.text?.toString()?.trim()
        val fechaStr = inputFechaNac.text?.toString()?.trim()

        // ------ Validar ALTURA ------
        val alturaCm = alturaStr?.toFloatOrNull()
        if (alturaStr.isNullOrEmpty()) {
            layoutAltura.error = "Ingresa tu talla en centímetros"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputAltura
        } else if (alturaCm == null) {
            layoutAltura.error = "La talla debe ser un número válido"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputAltura
        } else if (alturaCm !in ALTURA_MIN..ALTURA_MAX) {
            layoutAltura.error = "La talla debe estar entre $ALTURA_MIN y $ALTURA_MAX cm"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputAltura
        }

        // ------ Validar PESO ------
        val pesoKg = pesoStr?.toFloatOrNull()
        if (pesoStr.isNullOrEmpty()) {
            layoutPeso.error = "Ingresa tu peso en kilogramos"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputPeso
        } else if (pesoKg == null) {
            layoutPeso.error = "El peso debe ser un número válido"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputPeso
        } else if (pesoKg !in PESO_MIN..PESO_MAX) {
            layoutPeso.error = "El peso debe estar entre $PESO_MIN y $PESO_MAX kg"
            hayError = true
            primerCampoConError = primerCampoConError ?: inputPeso
        }

        // ------ Validar FECHA ------
        var fechaNacDate: Date? = null
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
                } else if (fechaNacDate.after(hoy)) {
                    layoutFechaNac.error = "La fecha no puede ser mayor a hoy"
                    hayError = true
                    primerCampoConError = primerCampoConError ?: inputFechaNac
                } else {
                    // Validar rango de edad (opcional: 5 a 120 años)
                    val edad = calcularEdad(fechaNacDate, hoy)
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

        // Si llegamos aquí, los datos son válidos
        val alturaRedondeada = redondear2Dec(alturaCm!!)
        val pesoRedondeado = redondear2Dec(pesoKg!!)

        val sexo = when (rgSexo.checkedRadioButtonId) {
            rbMasculino.id -> "M"
            rbFemenino.id -> "F"
            else -> "U" // Por si acaso
        }

        val prefs = requireContext().getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("altura_cm", alturaRedondeada)
            .putFloat("peso_kg", pesoRedondeado)
            .putString("fecha_nac", fechaStr)
            .putString("sexo", sexo)
            .apply()

        Toast.makeText(requireContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT)
            .show()

        // Volver al Dashboard (HomeFragment)
        findNavController().navigateUp()
    }

    private fun redondear2Dec(valor: Float): Float {
        return kotlin.math.round(valor * 100f) / 100f
    }

    private fun calcularEdad(fechaNac: Date, hoy: Date): Int {
        val calNac = Calendar.getInstance().apply { time = fechaNac }
        val calHoy = Calendar.getInstance().apply { time = hoy }

        var edad = calHoy.get(Calendar.YEAR) - calNac.get(Calendar.YEAR)

        // Ajuste si aún no cumplió años este año
        if (calHoy.get(Calendar.DAY_OF_YEAR) < calNac.get(Calendar.DAY_OF_YEAR)) {
            edad--
        }
        return edad
    }

    companion object {
        // Rangos que puedes ajustar a tu gusto
        private const val ALTURA_MIN = 100f  // cm
        private const val ALTURA_MAX = 250f  // cm
        private const val PESO_MIN = 30f     // kg
        private const val PESO_MAX = 250f    // kg

        private const val EDAD_MIN = 5       // años
        private const val EDAD_MAX = 120     // años
    }
}