package br.com.uri.campushub.feature.events.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class MyFavoritesActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var progressBar: ProgressBar
    private lateinit var favoritesScrollView: View
    private lateinit var favoritesContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        setContentView(R.layout.activity_my_favorites)

        progressBar = findViewById(R.id.progressMyFavorites)
        favoritesScrollView = findViewById(R.id.myFavoritesScrollView)
        favoritesContainer = findViewById(R.id.myFavoritesContainer)
        emptyState = findViewById(R.id.emptyMyFavoritesState)
        errorMessage = findViewById(R.id.tvMyFavoritesError)
        retryButton = findViewById(R.id.btnRetryMyFavorites)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        retryButton.setOnClickListener { loadFavorites() }
    }

    override fun onResume() {
        super.onResume()

        if (::favoritesContainer.isInitialized) {
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        progressBar.visibility = View.VISIBLE
        favoritesScrollView.visibility = View.GONE
        emptyState.visibility = View.GONE
        errorMessage.visibility = View.GONE
        retryButton.visibility = View.GONE

        EventFavoriteRepository.getFavorites(userId).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE

            if (!task.isSuccessful) {
                errorMessage.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            val events = task.result?.documents.orEmpty().map(::toCampusEvent)
            if (events.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            favoritesContainer.removeAllViews()
            events.forEach(::addEventCard)
            favoritesScrollView.visibility = View.VISIBLE
        }
    }

    private fun toCampusEvent(document: DocumentSnapshot): CampusEvent {
        return CampusEvent(
            id = document.getString("eventId") ?: document.id,
            title = document.getString("title").orEmpty(),
            description = document.getString("description").orEmpty(),
            date = document.getString("date").orEmpty(),
            location = document.getString("location").orEmpty(),
            category = document.getString("category") ?: "Sem categoria",
            startsAt = document.getTimestamp("startsAt"),
            endsAt = document.getTimestamp("endsAt")
        )
    }

    private fun addEventCard(event: CampusEvent) {
        val card = layoutInflater.inflate(R.layout.item_event, favoritesContainer, false)
        card.findViewById<TextView>(R.id.tvEventTitle).text = event.title
        card.findViewById<TextView>(R.id.tvEventDescription).text = event.description
        card.findViewById<TextView>(R.id.tvEventCategoryStatus).text = event.category
        card.findViewById<TextView>(R.id.tvEventDate).text = event.date
        card.findViewById<TextView>(R.id.tvEventLocation).text = event.location
        card.setOnClickListener {
            startActivity(EventDetailsActivity.createIntent(this, event))
        }
        favoritesContainer.addView(card)
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }
}
