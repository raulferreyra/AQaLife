package com.urasweb.aqualife.ui.login

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.urasweb.aqualife.R
import com.urasweb.aqualife.data.repository.AquaRepository

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory()
        )[LoginViewModel::class.java]

        auth = FirebaseAuth.getInstance()

        // ─────────────────────────────────────────────
        // AUTO-LOGIN: si ya hay usuario, saltar Login
        // ─────────────────────────────────────────────
        val currentUser = auth.currentUser
        if (currentUser != null) {
            AquaRepository.setCurrentUser(currentUser.uid)

            val prefs = requireContext()
                .getSharedPreferences("imc_prefs", Context.MODE_PRIVATE)
            val setupCompleted = prefs.getBoolean("setup_completed", false)

            if (setupCompleted) {
                // Ya hizo Setup → ir al Dashboard/Home
                findNavController().navigate(R.id.nav_home)
            } else {
                // Tiene sesión, pero falta Setup
                findNavController().navigate(R.id.nav_setup)
            }
            return
        }
        // ─────────────────────────────────────────────

        emailEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login)
        loadingProgressBar = view.findViewById(R.id.loading)

        // Habilitar / deshabilitar botón según validación
        loginViewModel.loginFormState.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            loginButton.isEnabled = state.isDataValid

            state.usernameError?.let { errorRes ->
                emailEditText.error = getString(errorRes)
            }

            state.passwordError?.let { errorRes ->
                passwordEditText.error = getString(errorRes)
            }
        }

        // Resultado de login
        loginViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe

            loadingProgressBar.visibility = View.GONE

            result.error?.let { errorRes ->
                showLoginFailed(errorRes)
            }

            if (result.success != null) {
                // Set current user in repository
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    AquaRepository.setCurrentUser(firebaseUser.uid)
                }

                // Continue navigation
                findNavController().navigate(R.id.nav_setup)
            }

        }

        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable?) {
                loginViewModel.loginDataChanged(
                    emailEditText.text?.toString() ?: "",
                    passwordEditText.text?.toString() ?: ""
                )
            }
        }

        emailEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)

        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && loginButton.isEnabled) {
                val email = emailEditText.text?.toString()?.trim() ?: ""
                val password = passwordEditText.text?.toString() ?: ""
                loadingProgressBar.visibility = View.VISIBLE
                loginWithFirebase(email, password)
                true
            } else {
                false
            }
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text?.toString()?.trim() ?: ""
            val password = passwordEditText.text?.toString() ?: ""

            loadingProgressBar.visibility = View.VISIBLE
            loginWithFirebase(email, password)
        }
    }

    private fun loginWithFirebase(email: String, password: String) {
        // Crear usuario; si ya existe, intentar login
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { createTask ->
                if (createTask.isSuccessful) {
                    val userEmail = auth.currentUser?.email ?: email
                    loginViewModel.onLoginSuccess(userEmail)
                } else {
                    val exception = createTask.exception

                    if (exception is FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { signInTask ->
                                if (signInTask.isSuccessful) {
                                    val userEmail = auth.currentUser?.email ?: email
                                    loginViewModel.onLoginSuccess(userEmail)
                                } else {
                                    loginViewModel.onLoginError()
                                    showFirebaseError(signInTask.exception)
                                }
                            }
                    } else {
                        loginViewModel.onLoginError()
                        showFirebaseError(exception)
                    }
                }
            }
    }

    private fun showFirebaseError(exception: Exception?) {
        val msg = when (exception) {
            is FirebaseAuthInvalidCredentialsException ->
                "Credenciales inválidas. Verificar correo y contraseña."
            is FirebaseAuthInvalidUserException ->
                "Usuario no encontrado."
            else -> {
                val type = exception?.javaClass?.simpleName ?: "Unknown"
                val detail = exception?.localizedMessage ?: "Authentication error."
                "$type: $detail"
            }
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome) + " " + model.displayName
        Toast.makeText(requireContext(), welcome, Toast.LENGTH_LONG).show()
    }

    private fun showLoginFailed(errorString: Int) {
        Toast.makeText(requireContext(), getString(errorString), Toast.LENGTH_SHORT).show()
    }
}
