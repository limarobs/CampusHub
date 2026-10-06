package br.com.uri.campushub.feature.auth.ui

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class PasswordRecoveryActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_recovery)

        val emailField = findViewById<TextInputEditText>(R.id.email)
        val sendButton = findViewById<MaterialButton>(R.id.btnSendRecoveryEmail)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        sendButton.setOnClickListener {
            val email = emailField.text?.toString()?.trim().orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Informe um e-mail válido."
                emailField.requestFocus()
                return@setOnClickListener
            }

            sendButton.isEnabled = false
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                sendButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Enviamos um link de redefinição para seu e-mail.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Não foi possível enviar o e-mail de recuperação.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
