package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventEditorRepository
import br.com.uri.campushub.feature.events.data.toCampusEvent
import br.com.uri.campushub.feature.events.model.EventDraft
import br.com.uri.campushub.feature.events.viewmodel.EventDraftInput
import br.com.uri.campushub.feature.events.viewmodel.EventDraftValidation
import br.com.uri.campushub.feature.events.viewmodel.EventDraftValidator
import br.com.uri.campushub.feature.profile.data.UserProfileRepository
import br.com.uri.campushub.feature.profile.model.UserRole
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventEditorActivity : AppCompatActivity() {

    private lateinit var userId: String
    private var eventId: String? = null
    private val validator = EventDraftValidator()

    private lateinit var titleField: TextInputEditText
    private lateinit var descriptionField: TextInputEditText
    private lateinit var categoryField: TextInputEditText
    private lateinit var locationField: TextInputEditText
    private lateinit var addressField: TextInputEditText
    private lateinit var startsAtField: TextInputEditText
    private lateinit var endsAtField: TextInputEditText
    private lateinit var capacityField: TextInputEditText
    private lateinit var coverImageUrlField: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorMessage: TextView
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        setContentView(R.layout.activity_event_editor)
        bindViews()
        requireOrganizer()
    }

    private fun bindViews() {
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        titleField = findViewById(R.id.etEventTitle)
        descriptionField = findViewById(R.id.etEventDescription)
        categoryField = findViewById(R.id.etEventCategory)
        locationField = findViewById(R.id.etEventLocation)
        addressField = findViewById(R.id.etEventAddress)
        startsAtField = findViewById(R.id.etEventStartsAt)
        endsAtField = findViewById(R.id.etEventEndsAt)
        capacityField = findViewById(R.id.etEventCapacity)
        coverImageUrlField = findViewById(R.id.etEventCoverImageUrl)
        saveButton = findViewById(R.id.btnSaveDraft)
        progressBar = findViewById(R.id.progressEventEditor)
        errorMessage = findViewById(R.id.tvEventEditorError)
        subtitle = findViewById(R.id.tvEventEditorSubtitle)

        findViewById<TextView>(R.id.tvEventEditorTitle).text =
            if (eventId == null) "Novo evento" else "Editar evento"
        saveButton.setOnClickListener { saveDraft() }
        saveButton.isEnabled = false
        configureDateTimeField(startsAtField, startDateTime())
        configureDateTimeField(endsAtField, endDateTime())

        if (eventId == null) {
            startsAtField.setText(formatDateTime(startDateTime()))
            endsAtField.setText(formatDateTime(endDateTime()))
        }
    }

    private fun requireOrganizer() {
        progressBar.visibility = View.VISIBLE
        UserProfileRepository.getRole(userId).addOnCompleteListener { task ->
            val role = if (task.isSuccessful) UserProfileRepository.roleFrom(task.result) else UserRole.STUDENT
            if (role != UserRole.ORGANIZER) {
                Toast.makeText(this, "Somente organizadores podem gerenciar eventos.", Toast.LENGTH_LONG).show()
                finish()
                return@addOnCompleteListener
            }

            if (eventId == null) {
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
            } else {
                loadEvent(requireNotNull(eventId))
            }
        }
    }

    private fun loadEvent(id: String) {
        EventEditorRepository.getEvent(id).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (!task.isSuccessful || task.result?.exists() != true) {
                showError("Não foi possível carregar o evento para edição.")
                return@addOnCompleteListener
            }

            val event = task.result!!.toCampusEvent()
            if (event.organizerId != userId) {
                Toast.makeText(this, "Você não pode editar este evento.", Toast.LENGTH_LONG).show()
                finish()
                return@addOnCompleteListener
            }

            titleField.setText(event.title)
            descriptionField.setText(event.description)
            categoryField.setText(event.category)
            locationField.setText(event.location)
            addressField.setText(event.address)
            startsAtField.setText(EventDraft.formatDate(event.startsAt))
            endsAtField.setText(EventDraft.formatDate(event.endsAt))
            capacityField.setText(event.capacity?.toString().orEmpty())
            coverImageUrlField.setText(event.coverImageUrl)
            subtitle.text = "Altere as informações e salve novamente como rascunho."
            saveButton.isEnabled = true
        }
    }

    private fun saveDraft() {
        val validation = validator.validate(
            EventDraftInput(
                title = titleField.text?.toString().orEmpty(),
                description = descriptionField.text?.toString().orEmpty(),
                category = categoryField.text?.toString().orEmpty(),
                location = locationField.text?.toString().orEmpty(),
                address = addressField.text?.toString().orEmpty(),
                startsAt = startsAtField.text?.toString().orEmpty(),
                endsAt = endsAtField.text?.toString().orEmpty(),
                capacity = capacityField.text?.toString().orEmpty(),
                coverImageUrl = coverImageUrlField.text?.toString().orEmpty()
            )
        )
        renderValidation(validation)
        val draft = validation.draft ?: return

        setSaving(true)
        val existingEventId = eventId
        if (existingEventId == null) {
            createDraft(draft)
        } else {
            updateDraft(existingEventId, draft)
        }
    }

    private fun createDraft(draft: EventDraft) {
        EventEditorRepository.createDraft(userId, draft).addOnCompleteListener { task ->
            setSaving(false)
            if (!task.isSuccessful) {
                showError("Não foi possível salvar o rascunho. Tente novamente.")
                return@addOnCompleteListener
            }

            Toast.makeText(this, "Rascunho salvo.", Toast.LENGTH_SHORT).show()
            eventId = task.result
            findViewById<TextView>(R.id.tvEventEditorTitle).text = "Editar evento"
            subtitle.text = "Rascunho salvo. Você pode continuar editando os dados."
        }
    }

    private fun updateDraft(id: String, draft: EventDraft) {
        EventEditorRepository.updateDraft(id, userId, draft).addOnCompleteListener { task ->
            setSaving(false)
            if (!task.isSuccessful) {
                showError("Não foi possível salvar o rascunho. Tente novamente.")
                return@addOnCompleteListener
            }

            Toast.makeText(this, "Alterações salvas.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderValidation(validation: EventDraftValidation) {
        findViewById<TextInputLayout>(R.id.tilEventTitle).error = validation.titleError
        findViewById<TextInputLayout>(R.id.tilEventDescription).error = validation.descriptionError
        findViewById<TextInputLayout>(R.id.tilEventCategory).error = validation.categoryError
        findViewById<TextInputLayout>(R.id.tilEventLocation).error = validation.locationError
        findViewById<TextInputLayout>(R.id.tilEventAddress).error = validation.addressError
        findViewById<TextInputLayout>(R.id.tilEventStartsAt).error = validation.startsAtError
        findViewById<TextInputLayout>(R.id.tilEventEndsAt).error = validation.endsAtError
        findViewById<TextInputLayout>(R.id.tilEventCapacity).error = validation.capacityError
        findViewById<TextInputLayout>(R.id.tilEventCoverImageUrl).error = validation.coverImageUrlError
    }

    private fun configureDateTimeField(field: TextInputEditText, fallback: Calendar) {
        field.setOnClickListener {
            val selected = parseDateTime(field.text?.toString()) ?: fallback
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selected.set(year, month, dayOfMonth)
                    TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selected.set(Calendar.MINUTE, minute)
                            field.setText(formatDateTime(selected))
                        },
                        selected.get(Calendar.HOUR_OF_DAY),
                        selected.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun startDateTime(): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun endDateTime(): Calendar = startDateTime().apply {
        set(Calendar.HOUR_OF_DAY, 11)
    }

    private fun parseDateTime(value: String?): Calendar? = runCatching {
        Calendar.getInstance().apply {
            time = dateTimeFormatter.parse(value.orEmpty()) ?: return null
        }
    }.getOrNull()

    private fun formatDateTime(calendar: Calendar): String = dateTimeFormatter.format(calendar.time)

    private fun setSaving(isSaving: Boolean) {
        progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
        saveButton.isEnabled = !isSaving
        if (isSaving) errorMessage.visibility = View.GONE
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"
        private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

        fun createIntent(context: Context): Intent = Intent(context, EventEditorActivity::class.java)

        fun editIntent(context: Context, eventId: String): Intent =
            Intent(context, EventEditorActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}
