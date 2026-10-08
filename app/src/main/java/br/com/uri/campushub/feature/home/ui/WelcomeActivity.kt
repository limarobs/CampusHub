package br.com.uri.campushub.feature.home.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.ui.EventsActivity
import br.com.uri.campushub.feature.events.ui.MyFavoritesActivity
import br.com.uri.campushub.feature.events.ui.MyEventsActivity
import br.com.uri.campushub.feature.profile.ui.ProfileActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var registeredEventsSummary: TextView
    private lateinit var favoritesSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        findViewById<TextView>(R.id.tvUserName).text = currentUser.displayName ?: "Aluno"
        findViewById<TextView>(R.id.tvUserEmail).text =
            currentUser.email ?: "E-mail não disponível"

        registeredEventsSummary = findViewById(R.id.tvRegisteredEventsSummary)
        favoritesSummary = findViewById(R.id.tvFavoritesSummary)

        findViewById<Button>(R.id.btnEvents).setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        findViewById<Button>(R.id.btnMyEvents).setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }

        findViewById<Button>(R.id.btnMyFavorites).setOnClickListener {
            startActivity(Intent(this, MyFavoritesActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMyEventsSummary).setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMyFavoritesSummary).setOnClickListener {
            startActivity(Intent(this, MyFavoritesActivity::class.java))
        }

        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            navigateToLogin()
        }
    }

    override fun onResume() {
        super.onResume()

        if (::userId.isInitialized) {
            loadUserSummary()
        }
    }

    private fun loadUserSummary() {
        registeredEventsSummary.text = "Carregando..."
        favoritesSummary.text = "Carregando..."

        EventRegistrationRepository.getRegistrations(userId).addOnCompleteListener { task ->
            registeredEventsSummary.text = if (task.isSuccessful) {
                formatCount(task.result?.size() ?: 0, "evento inscrito", "eventos inscritos")
            } else {
                "Indisponível"
            }
        }

        EventFavoriteRepository.getFavorites(userId).addOnCompleteListener { task ->
            favoritesSummary.text = if (task.isSuccessful) {
                formatCount(task.result?.size() ?: 0, "favorito", "favoritos")
            } else {
                "Indisponível"
            }
        }
    }

    private fun formatCount(count: Int, singular: String, plural: String): String {
        return "$count ${if (count == 1) singular else plural}"
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
