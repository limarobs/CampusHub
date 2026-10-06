package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var registrationButton: MaterialButton
    private lateinit var favoriteButton: MaterialButton
    private lateinit var userId: String
    private lateinit var event: CampusEvent
    private var isRegistered = false
    private var isRegistrationStateLoaded = false
    private var isFavorite = false
    private var isFavoriteStateLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        event = CampusEvent(
            id = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            date = intent.getStringExtra(EXTRA_DATE).orEmpty(),
            location = intent.getStringExtra(EXTRA_LOCATION).orEmpty(),
            category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Sem categoria",
            startsAt = timestampFromExtras(EXTRA_STARTS_AT_SECONDS, EXTRA_STARTS_AT_NANOSECONDS),
            endsAt = timestampFromExtras(EXTRA_ENDS_AT_SECONDS, EXTRA_ENDS_AT_NANOSECONDS)
        )

        setContentView(R.layout.activity_event_details)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvEventDetailsTitle).text =
            event.title
        findViewById<TextView>(R.id.tvEventDetailsDescription).text =
            event.description
        findViewById<TextView>(R.id.tvEventDetailsDate).text =
            event.date
        findViewById<TextView>(R.id.tvEventDetailsLocation).text =
            event.location

        registrationButton = findViewById(R.id.btnRegistration)
        registrationButton.setOnClickListener {
            if (!isRegistrationStateLoaded) {
                loadRegistrationState()
            } else if (isRegistered) {
                cancelRegistration()
            } else {
                registerForEvent()
            }
        }

        favoriteButton = findViewById(R.id.btnFavorite)
        favoriteButton.setOnClickListener {
            if (!isFavoriteStateLoaded) {
                loadFavoriteState()
            } else if (isFavorite) {
                removeFavorite()
            } else {
                addFavorite()
            }
        }

        loadRegistrationState()
        loadFavoriteState()
    }

    private fun loadRegistrationState() {
        isRegistrationStateLoaded = false
        registrationButton.isEnabled = false
        registrationButton.text = "Verificando inscrição..."

        EventRegistrationRepository.getRegistration(userId, event.id)
            .addOnCompleteListener { task ->
                registrationButton.isEnabled = true

                if (!task.isSuccessful) {
                    registrationButton.text = "Tentar novamente"
                    Toast.makeText(
                        this,
                        "Não foi possível verificar sua inscrição.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }

                isRegistered = task.result?.exists() == true
                isRegistrationStateLoaded = true
                updateRegistrationButton()
            }
    }

    private fun registerForEvent() {
        registrationButton.isEnabled = false
        registrationButton.text = "Inscrevendo..."

        EventRegistrationRepository.register(userId, event).addOnCompleteListener { task ->
            registrationButton.isEnabled = true

            if (task.isSuccessful) {
                isRegistered = true
                updateRegistrationButton()
                Toast.makeText(this, "Inscrição realizada.", Toast.LENGTH_SHORT).show()
            } else {
                updateRegistrationButton()
                Toast.makeText(
                    this,
                    "Não foi possível realizar a inscrição.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cancelRegistration() {
        registrationButton.isEnabled = false
        registrationButton.text = "Cancelando inscrição..."

        EventRegistrationRepository.cancel(userId, event.id).addOnCompleteListener { task ->
            registrationButton.isEnabled = true

            if (task.isSuccessful) {
                isRegistered = false
                updateRegistrationButton()
                Toast.makeText(this, "Inscrição cancelada.", Toast.LENGTH_SHORT).show()
            } else {
                updateRegistrationButton()
                Toast.makeText(
                    this,
                    "Não foi possível cancelar a inscrição.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateRegistrationButton() {
        registrationButton.text = if (isRegistered) {
            "Cancelar inscrição"
        } else {
            "Inscrever-se"
        }
    }

    private fun loadFavoriteState() {
        isFavoriteStateLoaded = false
        favoriteButton.isEnabled = false
        favoriteButton.text = "Verificando favoritos..."

        EventFavoriteRepository.getFavorite(userId, event.id).addOnCompleteListener { task ->
            favoriteButton.isEnabled = true

            if (!task.isSuccessful) {
                favoriteButton.text = "Tentar novamente"
                Toast.makeText(
                    this,
                    "NÃ£o foi possÃ­vel verificar seus favoritos.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnCompleteListener
            }

            isFavorite = task.result?.exists() == true
            isFavoriteStateLoaded = true
            updateFavoriteButton()
        }
    }

    private fun addFavorite() {
        favoriteButton.isEnabled = false
        favoriteButton.text = "Adicionando aos favoritos..."

        EventFavoriteRepository.favorite(userId, event).addOnCompleteListener { task ->
            favoriteButton.isEnabled = true

            if (task.isSuccessful) {
                isFavorite = true
                updateFavoriteButton()
                Toast.makeText(this, "Evento adicionado aos favoritos.", Toast.LENGTH_SHORT).show()
            } else {
                updateFavoriteButton()
                Toast.makeText(
                    this,
                    "NÃ£o foi possÃ­vel adicionar aos favoritos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeFavorite() {
        favoriteButton.isEnabled = false
        favoriteButton.text = "Removendo dos favoritos..."

        EventFavoriteRepository.unfavorite(userId, event.id).addOnCompleteListener { task ->
            favoriteButton.isEnabled = true

            if (task.isSuccessful) {
                isFavorite = false
                updateFavoriteButton()
                Toast.makeText(this, "Evento removido dos favoritos.", Toast.LENGTH_SHORT).show()
            } else {
                updateFavoriteButton()
                Toast.makeText(
                    this,
                    "NÃ£o foi possÃ­vel remover dos favoritos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateFavoriteButton() {
        favoriteButton.text = if (isFavorite) {
            "Remover dos favoritos"
        } else {
            "Adicionar aos favoritos"
        }
    }

    private fun timestampFromExtras(secondsKey: String, nanosecondsKey: String): Timestamp? {
        val seconds = intent.getLongExtra(secondsKey, NO_TIMESTAMP)
        if (seconds == NO_TIMESTAMP) {
            return null
        }

        return Timestamp(seconds, intent.getIntExtra(nanosecondsKey, 0))
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "extra_event_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_STARTS_AT_SECONDS = "extra_starts_at_seconds"
        private const val EXTRA_STARTS_AT_NANOSECONDS = "extra_starts_at_nanoseconds"
        private const val EXTRA_ENDS_AT_SECONDS = "extra_ends_at_seconds"
        private const val EXTRA_ENDS_AT_NANOSECONDS = "extra_ends_at_nanoseconds"
        private const val NO_TIMESTAMP = Long.MIN_VALUE

        fun createIntent(context: Context, event: CampusEvent): Intent {
            return Intent(context, EventDetailsActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_TITLE, event.title)
                putExtra(EXTRA_DESCRIPTION, event.description)
                putExtra(EXTRA_DATE, event.date)
                putExtra(EXTRA_LOCATION, event.location)
                putExtra(EXTRA_CATEGORY, event.category)
                putExtra(EXTRA_STARTS_AT_SECONDS, event.startsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_STARTS_AT_NANOSECONDS, event.startsAt?.nanoseconds ?: 0)
                putExtra(EXTRA_ENDS_AT_SECONDS, event.endsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_ENDS_AT_NANOSECONDS, event.endsAt?.nanoseconds ?: 0)
            }
        }
    }
}
