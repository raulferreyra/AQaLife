package com.urasweb.aqualife.ui.imc_new

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.urasweb.aqualife.R
import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.repository.AquaRepository
import com.urasweb.aqualife.sync.SyncManager
import kotlinx.coroutines.launch
import kotlin.math.round

class IMCNewFragment : Fragment() {

    private lateinit var layoutPeso: TextInputLayout
    private lateinit var layoutPerimetro: TextInputLayout
    private lateinit var inputPeso: TextInputEditText
    private lateinit var inputPerimetro: TextInputEditText
    private lateinit var btnGuardar: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_imc_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutPeso = view.findViewById(R.id.layoutPeso)
        layoutPerimetro = view.findViewById(R.id.layoutPerimetro)
        inputPeso = view.findViewById(R.id.inputPeso)
        inputPerimetro = view.findViewById(R.id.inputPerimetro)
        btnGuardar = view.findViewById(R.id.btnGuardarImc)

        // Precargar último peso / perímetro si los guardamos en prefs
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val ultimoPeso = prefs.getFloat(KEY_ULTIMO_PESO, 0f)
        if (ultimoPeso > 0f) {
            inputPeso.setText(ultimoPeso.toString())
        }

        val ultimoPerimetro = prefs.getFloat(KEY_ULTIMO_PERIMETRO, 0f)
        if (ultimoPerimetro > 0f) {
            inputPerimetro.setText(ultimoPerimetro.toString())
        }

        btnGuardar.setOnClickListener {
            guardarNuevoImc()
        }
    }

    private fun guardarNuevoImc() {
        layoutPeso.error = null
        layoutPerimetro.error = null

        val pesoStr = inputPeso.text?.toString()?.trim()
        val perimetroStr = inputPerimetro.text?.toString()?.trim()

        var pesoKg: Float? = null
        var perimetroAbdominal: Float? = null
        var hayError = false

        // Validar peso
        if (pesoStr.isNullOrEmpty()) {
            layoutPeso.error = "Ingresa tu peso"
            hayError = true
        } else {
            try {
                val p = pesoStr.replace(",", ".").toFloat()
                if (p !in PESO_MIN..PESO_MAX) {
                    layoutPeso.error = "El peso debe estar entre $PESO_MIN y $PESO_MAX kg"
                    hayError = true
                } else {
                    pesoKg = p
                }
            } catch (e: NumberFormatException) {
                layoutPeso.error = "Peso inválido"
                hayError = true
            }
        }

        // Validar perímetro abdominal
        if (perimetroStr.isNullOrEmpty()) {
            layoutPerimetro.error = "Ingresa tu perímetro abdominal"
            hayError = true
        } else {
            try {
                val pa = perimetroStr.replace(",", ".").toFloat()
                if (pa !in PERIMETRO_MIN..PERIMETRO_MAX) {
                    layoutPerimetro.error =
                        "El perímetro debe estar entre $PERIMETRO_MIN y $PERIMETRO_MAX cm"
                    hayError = true
                } else {
                    perimetroAbdominal = pa
                }
            } catch (e: NumberFormatException) {
                layoutPerimetro.error = "Perímetro inválido"
                hayError = true
            }
        }

        if (hayError || pesoKg == null || perimetroAbdominal == null) {
            Toast.makeText(
                requireContext(),
                "Corrige los campos marcados",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val appContext = requireContext().applicationContext
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Necesitamos la talla desde Setup
        val alturaCm = prefs.getFloat(KEY_ALTURA, 0f)
        if (alturaCm <= 0f) {
            Toast.makeText(
                requireContext(),
                "Configura primero tu altura en la sección inicial",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val tallaM = alturaCm / 100f

        lifecycleScope.launch {
            try {
                // Inicializar DB / repo
                AquaDatabase.init(appContext)
                val db = AquaDatabase.getInstance()
                AquaRepository.init(db)

                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(
                        requireContext(),
                        "Usuario no autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                AquaRepository.setCurrentUser(currentUser.uid)

                AquaRepository.addImcMeasurement(
                    pesoKg = pesoKg!!,
                    tallaM = tallaM,
                    perimetroAbdominalCm = perimetroAbdominal!!
                )

                // Guardamos últimos valores en prefs
                prefs.edit()
                    .putFloat(KEY_ULTIMO_PESO, pesoKg!!)
                    .putFloat(KEY_ULTIMO_PERIMETRO, perimetroAbdominal!!)
                    .apply()

                // Disparamos sincronización inmediata
                SyncManager.triggerImmediateSync(appContext)

                Toast.makeText(
                    requireContext(),
                    "Nuevo registro de IMC guardado",
                    Toast.LENGTH_SHORT
                ).show()

                // Volver atrás (o navegar donde tú quieras)
                requireActivity().onBackPressedDispatcher.onBackPressed()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al guardar IMC: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "imc_prefs"
        private const val KEY_ALTURA = "altura_cm"
        private const val KEY_ULTIMO_PESO = "ultimo_peso_kg"
        private const val KEY_ULTIMO_PERIMETRO = "ultimo_perimetro_cm"

        private const val PESO_MIN = 30f
        private const val PESO_MAX = 250f
        private const val PERIMETRO_MIN = 40f
        private const val PERIMETRO_MAX = 200f
    }
}
