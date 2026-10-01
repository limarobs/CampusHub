package br.com.uri.campushub

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class SignupActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val nicknameField = findViewById<EditText>(R.id.nickname)
        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val signupButton = findViewById<Button>(R.id.btnSignup)
        val backButton = findViewById<Button>(R.id.btnBack)

        signupButton.setOnClickListener {
            val nicknameValue = nicknameField.text.toString().trim()
            val emailValue = emailField.text.toString().trim()
            val passwordValue = passwordField.text.toString()

            if (!hasValidInputs(
                    nicknameField,
                    emailField,
                    passwordField,
                    nicknameValue,
                    emailValue,
                    passwordValue
                )
            ) {
                return@setOnClickListener
            }

            signupButton.isEnabled = false

            auth.createUserWithEmailAndPassword(emailValue, passwordValue)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveNickname(nicknameValue, signupButton)
                    } else {
                        signupButton.isEnabled = true
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage
                                ?: "Não foi possível criar a conta.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun hasValidInputs(
        nicknameField: EditText,
        emailField: EditText,
        passwordField: EditText,
        nicknameValue: String,
        emailValue: String,
        passwordValue: String
    ): Boolean {
        if (nicknameValue.isBlank()) {
            nicknameField.error = "Informe seu apelido."
            nicknameField.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailField.error = "Informe um e-mail válido."
            emailField.requestFocus()
            return false
        }

        if (passwordValue.length < 6) {
            passwordField.error = "A senha deve ter pelo menos 6 caracteres."
            passwordField.requestFocus()
            return false
        }

        return true
    }

    private fun saveNickname(nicknameValue: String, signupButton: Button) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            signupButton.isEnabled = true
            Toast.makeText(
                this,
                "Não foi possível salvar o apelido.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val profileUpdates = userProfileChangeRequest {
            displayName = nicknameValue
        }

        currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
            signupButton.isEnabled = true

            val message = if (task.isSuccessful) {
                "Cadastro OK!"
            } else {
                "Conta criada, mas não foi possível salvar o apelido."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
        }
    }
}
