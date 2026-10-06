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
import br.com.uri.campushub.feature.events.data.EventRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class EventsActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var eventsScrollView: View
    private lateinit var eventsContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_events)

        progressBar = findViewById(R.id.progressEvents)
        eventsScrollView = findViewById(R.id.eventsScrollView)
        eventsContainer = findViewById(R.id.eventsContainer)
        emptyState = findViewById(R.id.emptyState)
        errorMessage = findViewById(R.id.tvEventsError)
        retryButton = findViewById(R.id.btnRetryEvents)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        retryButton.setOnClickListener {
            loadEvents()
        }

        loadEvents()
    }

    private fun loadEvents() {
        progressBar.visibility = View.VISIBLE
        eventsScrollView.visibility = View.GONE
        emptyState.visibility = View.GONE
        errorMessage.visibility = View.GONE
        retryButton.visibility = View.GONE

        EventRepository.getAvailableEvents().addOnCompleteListener { task ->
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

            eventsContainer.removeAllViews()
            events.forEach(::addEventCard)
            eventsScrollView.visibility = View.VISIBLE
        }
    }

    private fun toCampusEvent(document: DocumentSnapshot): CampusEvent {
        return CampusEvent(
            id = document.id,
            title = document.getString("title").orEmpty(),
            description = document.getString("description").orEmpty(),
            date = document.getString("date").orEmpty(),
            location = document.getString("location").orEmpty()
        )
    }

    private fun addEventCard(event: CampusEvent) {
        val card = layoutInflater.inflate(R.layout.item_event, eventsContainer, false)
        card.findViewById<TextView>(R.id.tvEventTitle).text = event.title
        card.findViewById<TextView>(R.id.tvEventDescription).text = event.description
        card.findViewById<TextView>(R.id.tvEventDate).text = event.date
        card.findViewById<TextView>(R.id.tvEventLocation).text = event.location
        card.setOnClickListener {
            startActivity(EventDetailsActivity.createIntent(this, event))
        }
        eventsContainer.addView(card)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
