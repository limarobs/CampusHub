package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.data.CheckInRepository
import br.com.uri.campushub.feature.events.data.EventEditorRepository
import br.com.uri.campushub.feature.events.data.toCampusEvent
import br.com.uri.campushub.feature.events.data.toEventRegistration
import br.com.uri.campushub.feature.events.model.EventRegistration
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class EventAttendanceActivity : AppCompatActivity() {

    private lateinit var eventId: String
    private lateinit var progress: ProgressBar
    private lateinit var content: View
    private lateinit var error: View
    private lateinit var registrationsContainer: LinearLayout
    private lateinit var emptyState: View
    private var refreshOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        if (eventId.isBlank() || FirebaseAuth.getInstance().currentUser == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_event_attendance)
        progress = findViewById(R.id.progressAttendance)
        content = findViewById(R.id.attendanceContent)
        error = findViewById(R.id.attendanceError)
        registrationsContainer = findViewById(R.id.registrationsContainer)
        emptyState = findViewById(R.id.emptyRegistrations)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnRetryAttendance).setOnClickListener { loadAttendance() }
        findViewById<MaterialButton>(R.id.btnStartCheckIn).setOnClickListener {
            refreshOnResume = true
            startActivity(CheckInScannerActivity.createIntent(this, eventId))
        }
        loadAttendance()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized && refreshOnResume) {
            refreshOnResume = false
            loadAttendance()
        }
    }

    private fun loadAttendance() {
        progress.visibility = View.VISIBLE
        content.visibility = View.GONE
        error.visibility = View.GONE

        EventEditorRepository.getEvent(eventId).addOnCompleteListener { eventTask ->
            if (!eventTask.isSuccessful || eventTask.result?.exists() != true) {
                showError()
                return@addOnCompleteListener
            }

            val event = eventTask.result!!.toCampusEvent()
            if (event.organizerId != FirebaseAuth.getInstance().currentUser?.uid) {
                showError()
                return@addOnCompleteListener
            }

            bindEvent(event)
            CheckInRepository.getEventRegistrations(eventId).addOnCompleteListener { registrationsTask ->
                if (!registrationsTask.isSuccessful) {
                    showError()
                    return@addOnCompleteListener
                }
                val registrations = registrationsTask.result?.documents.orEmpty()
                    .map { it.toEventRegistration() }
                    .sortedWith(compareBy<EventRegistration> { it.status != RegistrationStatus.CONFIRMED }
                        .thenBy { it.registeredAt })
                bindRegistrations(registrations)
                progress.visibility = View.GONE
                content.visibility = View.VISIBLE
            }
        }
    }

    private fun bindEvent(event: br.com.uri.campushub.feature.events.model.CampusEvent) {
        findViewById<TextView>(R.id.tvAttendanceEventTitle).text = event.title
        findViewById<TextView>(R.id.tvAttendanceEventInfo).text = "${event.date} • ${event.location}"
        val capacity = event.capacity?.toString() ?: "Sem limite"
        findViewById<TextView>(R.id.tvCapacityValue).text = "${event.confirmedCount} / $capacity"
        findViewById<TextView>(R.id.tvWaitlistValue).text = event.waitlistCount.toString()
    }

    private fun bindRegistrations(registrations: List<EventRegistration>) {
        val checkedIn = registrations.count { it.checkedInAt != null }
        findViewById<TextView>(R.id.tvCheckInValue).text = checkedIn.toString()
        findViewById<TextView>(R.id.tvRegistrationListSubtitle).text =
            "${registrations.count { it.status == RegistrationStatus.CONFIRMED }} confirmados • ${registrations.count { it.status == RegistrationStatus.WAITLISTED }} na espera"

        registrationsContainer.removeAllViews()
        emptyState.visibility = if (registrations.isEmpty()) View.VISIBLE else View.GONE
        registrations.forEach { registration ->
            val item = layoutInflater.inflate(R.layout.item_event_registration, registrationsContainer, false)
            item.findViewById<TextView>(R.id.tvAttendeeName).text =
                registration.attendeeName.ifBlank { "Aluno sem nome" }
            item.findViewById<TextView>(R.id.tvAttendeeEmail).text = registration.attendeeEmail
            item.findViewById<TextView>(R.id.tvRegistrationState).text = when (registration.status) {
                RegistrationStatus.CONFIRMED -> "Inscrição confirmada"
                RegistrationStatus.WAITLISTED -> "Lista de espera"
                RegistrationStatus.CANCELLED -> "Cancelada"
            }
            item.findViewById<TextView>(R.id.tvCheckInState).text = if (registration.checkedInAt == null) {
                "Check-in pendente"
            } else {
                "Check-in realizado"
            }
            registrationsContainer.addView(item)
        }
    }

    private fun showError() {
        progress.visibility = View.GONE
        content.visibility = View.GONE
        error.visibility = View.VISIBLE
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"

        fun createIntent(context: Context, eventId: String): Intent =
            Intent(context, EventAttendanceActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}
