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
import com.urasweb.aqualife.R

class MeasureFragment : Fragment() {

    private lateinit var rgLongitud: RadioGroup
    private lateinit var rbLongitudM: RadioButton
    private lateinit var rbLongitudFt: RadioButton

    private lateinit var rgVolumen: RadioGroup
    private lateinit var rbVolumenMl: RadioButton
    private lateinit var rbVolumenOz: RadioButton

    private lateinit var rgPesoUnidad: RadioGroup
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
        rbLongitudM = view.findViewById(R.id.rbLongitudM)
        rbLongitudFt = view.findViewById(R.id.rbLongitudFt)

        rgVolumen = view.findViewById(R.id.rgVolumen)
        rbVolumenMl = view.findViewById(R.id.rbVolumenMl)
        rbVolumenOz = view.findViewById(R.id.rbVolumenOz)

        rgPesoUnidad = view.findViewById(R.id.rgPesoUnidad)
        rbPesoKg = view.findViewById(R.id.rbPesoKg)
        rbPesoLb = view.findViewById(R.id.rbPesoLb)

        btnGuardar = view.findViewById(R.id.btnGuardarUnidades)

        btnGuardar.setOnClickListener { guardarUnidades() }

        cargarDesdeFirestore()
    }

    private fun cargarDesdeFirestore() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid)
            .collection("settings")
            .document("units")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener

                when (snapshot.getString("longitud") ?: "m") {
                    "ft" -> rbLongitudFt.isChecked = true
                    else -> rbLongitudM.isChecked = true
                }

                when (snapshot.getString("volumen") ?: "ml") {
                    "oz" -> rbVolumenOz.isChecked = true
                    else -> rbVolumenMl.isChecked = true
                }

                when (snapshot.getString("peso") ?: "kg") {
                    "lb" -> rbPesoLb.isChecked = true
                    else -> rbPesoKg.isChecked = true
                }
            }
    }

    private fun guardarUnidades() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val longitud = if (rgLongitud.checkedRadioButtonId == R.id.rbLongitudFt) "ft" else "m"
        val volumen = if (rgVolumen.checkedRadioButtonId == R.id.rbVolumenOz) "oz" else "ml"
        val peso = if (rgPesoUnidad.checkedRadioButtonId == R.id.rbPesoLb) "lb" else "kg"

        val data = hashMapOf(
            "longitud" to longitud,
            "volumen" to volumen,
            "peso" to peso,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(user.uid)
            .collection("settings")
            .document("units")
            .set(data)
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
