package br.com.uri.campushub.feature.auth.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.home.ui.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

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

            if (!hasValidInputs(emailField, passwordField, emailValue, passwordValue)) {
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

    private fun hasValidInputs(
        emailField: EditText,
        passwordField: EditText,
        emailValue: String,
        passwordValue: String
    ): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailField.error = "Informe um e-mail válido."
            emailField.requestFocus()
            return false
        }

        if (passwordValue.isBlank()) {
            passwordField.error = "Informe sua senha."
            passwordField.requestFocus()
            return false
        }

        return true
    }
}
