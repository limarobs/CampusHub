package br.com.uri.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        val nameField = findViewById<TextInputEditText>(R.id.name)
        val emailField = findViewById<TextInputEditText>(R.id.email)
        val saveButton = findViewById<MaterialButton>(R.id.btnSave)

        nameField.setText(currentUser.displayName)
        emailField.setText(currentUser.email)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val displayName = nameField.text?.toString()?.trim().orEmpty()

            if (displayName.isBlank()) {
                nameField.error = "Informe seu nome."
                nameField.requestFocus()
                return@setOnClickListener
            }

            saveButton.isEnabled = false
            updateProfile(displayName, saveButton)
        }
    }

    private fun updateProfile(displayName: String, saveButton: MaterialButton) {
        val currentUser = auth.currentUser ?: run {
            navigateToLogin()
            return
        }
        val profileUpdates = userProfileChangeRequest {
            this.displayName = displayName
        }

        currentUser.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
            if (!profileTask.isSuccessful) {
                saveButton.isEnabled = true
                Toast.makeText(
                    this,
                    "Não foi possível atualizar seu perfil.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnCompleteListener
            }

            UserProfileRepository.update(currentUser, displayName)
                .addOnCompleteListener { firestoreTask ->
                    saveButton.isEnabled = true

                    val message = if (firestoreTask.isSuccessful) {
                        "Perfil atualizado."
                    } else {
                        "Nome atualizado, mas não foi possível sincronizar o perfil."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
