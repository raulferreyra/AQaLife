package com.urasweb.aqualife.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.urasweb.aqualife.R

class LoginFragment : Fragment() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Views
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // OJO: estos IDs deben existir en fragment_login.xml.
        // Si tus campos se llaman distinto, cambia SOLO estas líneas.
        emailEditText = view.findViewById(R.id.username)          // EditText de Email
        passwordEditText = view.findViewById(R.id.password)       // EditText de Password
        loginButton = view.findViewById(R.id.login)               // Botón SIGN IN OR REGISTER
        loadingProgressBar = view.findViewById(R.id.loading)      // ProgressBar

        // Acción "done" del teclado
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin()
                true
            } else {
                false
            }
        }

        // Click en el botón
        loginButton.setOnClickListener {
            intentarLogin()
        }
    }

    private fun intentarLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty()) {
            emailEditText.error = "Ingresa tu correo"
            return
        }
        if (password.length < 6) {
            passwordEditText.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        loginButton.isEnabled = false
        loadingProgressBar.visibility = View.VISIBLE

        // 1. Intentar iniciar sesión
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    // 2. Si el usuario no existe, lo creamos
                    if (task.exception is FirebaseAuthInvalidUserException) {
                        crearUsuarioEnFirebase(email, password)
                    } else {
                        mostrarError(task.exception?.message ?: "Error al iniciar sesión")
                    }
                }
            }
    }

    private fun crearUsuarioEnFirebase(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val data = hashMapOf(
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("users").document(uid).set(data)
                            .addOnSuccessListener {
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                mostrarError("Usuario creado, pero fallo al guardar datos: ${e.message}")
                            }
                    } else {
                        mostrarError("No se pudo obtener el usuario creado")
                    }
                } else {
                    mostrarError(task.exception?.message ?: "Error al registrar usuario")
                }
            }
    }

    private fun onLoginSuccess() {
        loadingProgressBar.visibility = View.GONE
        loginButton.isEnabled = true

        Toast.makeText(requireContext(), "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()

        // Por ahora te llevo directo al Dashboard.
        // Cambia nav_home por nav_setup si quieres forzar completar datos primero.
        findNavController().navigate(R.id.nav_home)
    }

    private fun mostrarError(mensaje: String) {
        loadingProgressBar.visibility = View.GONE
        loginButton.isEnabled = true
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }
}
