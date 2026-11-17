package br.com.uri.campushub.feature.events.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventEditorRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.data.toCampusEvent
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.model.EventStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class MyEventsActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var progressBar: ProgressBar
    private lateinit var eventsScrollView: View
    private lateinit var eventsContainer: LinearLayout
    private lateinit var emptyState: View
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: MaterialButton
    private val showOrganizedEvents: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_ORGANIZED_EVENTS, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        setContentView(R.layout.activity_my_events)

        progressBar = findViewById(R.id.progressMyEvents)
        eventsScrollView = findViewById(R.id.myEventsScrollView)
        eventsContainer = findViewById(R.id.myEventsContainer)
        emptyState = findViewById(R.id.emptyMyEventsState)
        errorMessage = findViewById(R.id.tvMyEventsError)
        retryButton = findViewById(R.id.btnRetryMyEvents)
        configureContent()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        retryButton.setOnClickListener {
            loadMyEvents()
        }
    }

    override fun onResume() {
        super.onResume()

        if (::eventsContainer.isInitialized) {
            loadMyEvents()
        }
    }

    private fun loadMyEvents() {
        progressBar.visibility = View.VISIBLE
        eventsScrollView.visibility = View.GONE
        emptyState.visibility = View.GONE
        errorMessage.visibility = View.GONE
        retryButton.visibility = View.GONE

        val eventsRequest = if (showOrganizedEvents) {
            EventEditorRepository.getOrganizedEvents(userId)
        } else {
            EventRegistrationRepository.getRegistrations(userId)
        }

        eventsRequest.addOnCompleteListener { task ->
            progressBar.visibility = View.GONE

            if (!task.isSuccessful) {
                errorMessage.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            val events = task.result?.documents.orEmpty().map { document ->
                if (showOrganizedEvents) document.toCampusEvent() else toRegisteredEvent(document)
            }

            if (events.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            eventsContainer.removeAllViews()
            events.forEach(::addEventCard)
            eventsScrollView.visibility = View.VISIBLE
        }
    }

    private fun toRegisteredEvent(document: DocumentSnapshot): CampusEvent {
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
        val card = layoutInflater.inflate(R.layout.item_event, eventsContainer, false)
        card.findViewById<TextView>(R.id.tvEventTitle).text = event.title
        card.findViewById<TextView>(R.id.tvEventDescription).text = event.description
        card.findViewById<TextView>(R.id.tvEventCategoryStatus).text = if (showOrganizedEvents) {
            "${event.category} • ${event.status.label}"
        } else {
            event.category
        }
        card.findViewById<TextView>(R.id.tvEventDate).text = event.date
        card.findViewById<TextView>(R.id.tvEventLocation).text = event.location
        configureStatusAction(card.findViewById(R.id.btnEventStatusAction), event)
        card.setOnClickListener {
            if (showOrganizedEvents) {
                startActivity(EventEditorActivity.editIntent(this, event.id))
            } else {
                startActivity(EventDetailsActivity.createIntent(this, event))
            }
        }
        eventsContainer.addView(card)
    }

    private fun configureStatusAction(button: MaterialButton, event: CampusEvent) {
        if (!showOrganizedEvents || event.status == EventStatus.CLOSED) {
            button.visibility = View.GONE
            return
        }

        button.visibility = View.VISIBLE
        button.text = if (event.status == EventStatus.DRAFT) "Publicar evento" else "Encerrar evento"
        button.setOnClickListener {
            if (event.status == EventStatus.PUBLISHED) {
                confirmClosingEvent(event, button)
            } else {
                updateEventStatus(event, EventStatus.PUBLISHED, button)
            }
        }
    }

    private fun confirmClosingEvent(event: CampusEvent, button: MaterialButton) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Encerrar evento?")
            .setMessage("${event.title} deixará de aceitar novas inscrições.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Encerrar") { _, _ ->
                updateEventStatus(event, EventStatus.CLOSED, button)
            }
            .show()
    }

    private fun updateEventStatus(event: CampusEvent, status: EventStatus, button: MaterialButton) {
        button.isEnabled = false
        EventEditorRepository.updateStatus(event.id, status).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Evento ${status.label.lowercase()}.", Toast.LENGTH_SHORT).show()
                loadMyEvents()
            } else {
                button.isEnabled = true
                Toast.makeText(this, "Não foi possível atualizar o evento.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configureContent() {
        if (!showOrganizedEvents) return

        findViewById<TextView>(R.id.tvMyEventsTitle).text = "Eventos organizados"
        findViewById<TextView>(R.id.tvMyEventsSubtitle).text =
            "Rascunhos e eventos sob sua responsabilidade. Toque para editar."
        findViewById<TextView>(R.id.tvEmptyMyEventsTitle).text = "Nenhum evento criado"
        findViewById<TextView>(R.id.tvEmptyMyEventsDescription).text =
            "Crie um evento para começar a organizar sua programação."
        errorMessage.text = "Não foi possível carregar seus eventos."
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    companion object {
        private const val EXTRA_SHOW_ORGANIZED_EVENTS = "show_organized_events"

        fun organizedEventsIntent(context: android.content.Context): Intent =
            Intent(context, MyEventsActivity::class.java)
                .putExtra(EXTRA_SHOW_ORGANIZED_EVENTS, true)
    }
}
