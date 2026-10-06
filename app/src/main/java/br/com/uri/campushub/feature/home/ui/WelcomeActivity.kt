package br.com.uri.campushub.feature.home.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.ui.EventsActivity
import br.com.uri.campushub.feature.events.ui.MyFavoritesActivity
import br.com.uri.campushub.feature.events.ui.MyEventsActivity
import br.com.uri.campushub.feature.profile.ui.ProfileActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            navigateToLogin()
            return
        }

        findViewById<TextView>(R.id.tvUserName).text = currentUser.displayName ?: "Aluno"
        findViewById<TextView>(R.id.tvUserEmail).text =
            currentUser.email ?: "E-mail não disponível"

        findViewById<Button>(R.id.btnEvents).setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        findViewById<Button>(R.id.btnMyEvents).setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }

        findViewById<Button>(R.id.btnMyFavorites).setOnClickListener {
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

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
