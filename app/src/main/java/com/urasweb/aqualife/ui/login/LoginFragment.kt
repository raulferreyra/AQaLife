package com.urasweb.aqualife.ui.login

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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.urasweb.aqualife.R

class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
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

        // ViewModel
        loginViewModel = ViewModelProvider(
            this,
            LoginViewModelFactory()
        )[LoginViewModel::class.java]

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias a vistas (ajusta los IDs si en tu XML son distintos)
        emailEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login)
        loadingProgressBar = view.findViewById(R.id.loading)

        // Observa el estado del formulario (habilita/deshabilita botón, muestra errores)
        loginViewModel.loginFormState.observe(viewLifecycleOwner) { loginFormState ->
            if (loginFormState == null) return@observe

            loginButton.isEnabled = loginFormState.isDataValid

            loginFormState.usernameError?.let { errorRes ->
                emailEditText.error = getString(errorRes)
            }

            loginFormState.passwordError?.let { errorRes ->
                passwordEditText.error = getString(errorRes)
            }
        }

        // Observa el resultado del login
        loginViewModel.loginResult.observe(viewLifecycleOwner) { loginResult ->
            loginResult ?: return@observe

            loadingProgressBar.visibility = View.GONE

            loginResult.error?.let { errorRes ->
                showLoginFailed(errorRes)
            }

            loginResult.success?.let { loggedInUserView ->
                updateUiWithUser(loggedInUserView)

                // Después del login OK => ir a Setup primero
                findNavController().navigate(R.id.nav_setup)
            }
        }

        // TextWatcher para validar mientras se escribe
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                loginViewModel.loginDataChanged(
                    emailEditText.text?.toString() ?: "",
                    passwordEditText.text?.toString() ?: ""
                )
            }
        }

        emailEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)

        // Tecla "Done" del teclado en el campo password
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && loginButton.isEnabled) {
                val email = emailEditText.text?.toString()?.trim() ?: ""
                val password = passwordEditText.text?.toString() ?: ""
                loginWithFirebase(email, password)
                true
            } else {
                false
            }
        }

        // Click en el botón
        loginButton.setOnClickListener {
            val email = emailEditText.text?.toString()?.trim() ?: ""
            val password = passwordEditText.text?.toString() ?: ""

            loadingProgressBar.visibility = View.VISIBLE
            loginWithFirebase(email, password)
        }
    }

    /**
     * Login con Firebase. Si el usuario no existe, lo crea.
     */
    private fun loginWithFirebase(email: String, password: String) {
        // Primero intentamos login
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userEmail = auth.currentUser?.email ?: email
                    loginViewModel.onLoginSuccess(userEmail)
                } else {
                    val exception = task.exception

                    // Si el usuario no existe, intentamos registrarlo
                    if (exception is FirebaseAuthInvalidUserException) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { createTask ->
                                if (createTask.isSuccessful) {
                                    val userEmail = auth.currentUser?.email ?: email
                                    loginViewModel.onLoginSuccess(userEmail)
                                } else {
                                    loginViewModel.onLoginError()
                                    showFirebaseError(createTask.exception)
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
            is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña inválidos."
            is FirebaseAuthInvalidUserException -> "Usuario no encontrado."
            else -> exception?.localizedMessage ?: "Error de autenticación."
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
