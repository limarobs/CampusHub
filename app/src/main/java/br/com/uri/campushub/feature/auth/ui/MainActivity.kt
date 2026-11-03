package br.com.uri.campushub.feature.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.domain.AuthInputValidator
import br.com.uri.campushub.feature.auth.domain.InputField
import br.com.uri.campushub.feature.home.ui.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val inputValidator = AuthInputValidator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupButton = findViewById<Button>(R.id.btnSignup)
        val forgotPasswordButton = findViewById<Button>(R.id.btnForgotPassword)

        loginButton.setOnClickListener {
            val emailValue = emailField.text.toString().trim()
            val passwordValue = passwordField.text.toString()

            val inputError = inputValidator.login(emailValue, passwordValue)
            if (inputError != null) {
                when (inputError.field) {
                    InputField.EMAIL -> emailField
                    InputField.PASSWORD -> passwordField
                    InputField.NAME -> emailField
                }.apply {
                    error = inputError.message
                    requestFocus()
                }
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { task ->
                    loginButton.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login OK!", Toast.LENGTH_SHORT).show()
                        openWelcomeScreen()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Não foi possível fazer login.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, PasswordRecoveryActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser != null) {
            openWelcomeScreen()
        }
    }

    private fun openWelcomeScreen() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }
}
