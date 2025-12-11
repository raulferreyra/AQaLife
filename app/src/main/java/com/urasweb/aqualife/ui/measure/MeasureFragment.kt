package com.urasweb.aqualife.ui.measures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.urasweb.aqualife.R

class MeasureFragment : Fragment() {

    private lateinit var rgLongitud: RadioGroup
    private lateinit var rbLongitudCm: RadioButton
    private lateinit var rbLongitudFt: RadioButton

    private lateinit var rgVolumen: RadioGroup
    private lateinit var rbVolumenMl: RadioButton
    private lateinit var rbVolumenOz: RadioButton

    private lateinit var rgPeso: RadioGroup
    private lateinit var rbPesoKg: RadioButton
    private lateinit var rbPesoLb: RadioButton

    private lateinit var btnGuardar: Button

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_measure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgLongitud = view.findViewById(R.id.rgLongitud)
        rbLongitudCm = view.findViewById(R.id.rbLongitudM)   // usaremos "cm" como valor interno
        rbLongitudFt = view.findViewById(R.id.rbLongitudFt)

        rgVolumen = view.findViewById(R.id.rgVolumen)
        rbVolumenMl = view.findViewById(R.id.rbVolumenMl)
        rbVolumenOz = view.findViewById(R.id.rbVolumenOz)

        rgPeso = view.findViewById(R.id.rgPesoUnidad)
        rbPesoKg = view.findViewById(R.id.rbPesoKg)
        rbPesoLb = view.findViewById(R.id.rbPesoLb)

        btnGuardar = view.findViewById(R.id.btnGuardarUnidades)

        // Defaults en UI: sistema métrico
        aplicarValoresPorDefectoUI()

        btnGuardar.setOnClickListener { guardarEnFirestore() }

        // Cargar config si ya existe
        cargarDesdeFirestore()
    }

    private fun aplicarValoresPorDefectoUI() {
        rbLongitudCm.isChecked = true    // "cm"
        rbVolumenMl.isChecked = true     // "ml"
        rbPesoKg.isChecked = true        // "kg"
    }

    private fun cargarDesdeFirestore() {
        val user = auth.currentUser ?: return

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("units")

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // No hay doc, nos quedamos con los defaults
                    return@addOnSuccessListener
                }

                val lengthUnit = snapshot.getString("lengthUnit") ?: "cm"
                val volumeUnit = snapshot.getString("volumeUnit") ?: "ml"
                val weightUnit = snapshot.getString("weightUnit") ?: "kg"

                // Longitud
                when (lengthUnit) {
                    "ft" -> rbLongitudFt.isChecked = true
                    else -> rbLongitudCm.isChecked = true
                }

                // Volumen
                when (volumeUnit) {
                    "oz" -> rbVolumenOz.isChecked = true
                    else -> rbVolumenMl.isChecked = true
                }

                // Peso
                when (weightUnit) {
                    "lb" -> rbPesoLb.isChecked = true
                    else -> rbPesoKg.isChecked = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "No se pudo cargar unidades: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun guardarEnFirestore() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val lengthUnit = if (rgLongitud.checkedRadioButtonId == R.id.rbLongitudFt) "ft" else "cm"
        val volumeUnit = if (rgVolumen.checkedRadioButtonId == R.id.rbVolumenOz) "oz" else "ml"
        val weightUnit = if (rgPeso.checkedRadioButtonId == R.id.rbPesoLb) "lb" else "kg"

        val data = hashMapOf(
            "lengthUnit" to lengthUnit,
            "volumeUnit" to volumeUnit,
            "weightUnit" to weightUnit,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("units")

        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Unidades guardadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error al guardar unidades: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
