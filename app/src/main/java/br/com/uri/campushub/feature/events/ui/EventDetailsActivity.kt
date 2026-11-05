package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth

class EventDetailsActivity : AppCompatActivity() {
    private lateinit var registrationButton: MaterialButton
    private lateinit var favoriteButton: MaterialButton
    private lateinit var ratingBar: RatingBar
    private lateinit var saveRatingButton: MaterialButton
    private lateinit var ratingSummary: TextView
    private lateinit var ratingEligibility: TextView
    private lateinit var commentsController: EventCommentsController
    private lateinit var mediaController: EventMediaController
    private lateinit var authorName: String
    private lateinit var userId: String
    private lateinit var event: CampusEvent
    private val detailsViewModel: EventDetailsViewModel by viewModels()
    private val detailsPolicy = EventDetailsPolicy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        userId = currentUser.uid
        authorName = currentUser.displayName ?: currentUser.email ?: "Aluno"
        event = CampusEvent(
            id = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            date = intent.getStringExtra(EXTRA_DATE).orEmpty(),
            location = intent.getStringExtra(EXTRA_LOCATION).orEmpty(),
            address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty(),
            latitude = coordinateFromExtra(EXTRA_LATITUDE),
            longitude = coordinateFromExtra(EXTRA_LONGITUDE),
            category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Sem categoria",
            startsAt = timestampFromExtras(EXTRA_STARTS_AT_SECONDS, EXTRA_STARTS_AT_NANOSECONDS),
            endsAt = timestampFromExtras(EXTRA_ENDS_AT_SECONDS, EXTRA_ENDS_AT_NANOSECONDS)
        )

        setContentView(R.layout.activity_event_details)
        bindEventViews()
        bindDetailsControls()
        initializeControllers()
        observeDetailsState()
        detailsViewModel.start(userId, event)
        commentsController.load()
    }

    private fun bindEventViews() {
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvEventDetailsTitle).text = event.title
        findViewById<TextView>(R.id.tvEventDetailsDescription).text = event.description
        findViewById<TextView>(R.id.tvEventDetailsDate).text = event.date
        findViewById<TextView>(R.id.tvEventDetailsLocation).text = event.location
        findViewById<TextView>(R.id.tvEventDetailsAddress).text =
            event.address.ifBlank { "Endereço não informado." }
        EventMapRenderer.render(findViewById(android.R.id.content), event)
    }

    private fun bindDetailsControls() {
        registrationButton = findViewById(R.id.btnRegistration)
        registrationButton.setOnClickListener { detailsViewModel.toggleRegistration() }
        favoriteButton = findViewById(R.id.btnFavorite)
        favoriteButton.setOnClickListener { detailsViewModel.toggleFavorite() }
        ratingBar = findViewById(R.id.ratingBar)
        saveRatingButton = findViewById(R.id.btnSaveRating)
        ratingSummary = findViewById(R.id.tvRatingSummary)
        ratingEligibility = findViewById(R.id.tvRatingEligibility)
        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.setOnClickListener { detailsViewModel.saveRating(ratingBar.rating.toInt()) }
    }

    private fun initializeControllers() {
        val commentInput = findViewById<TextInputEditText>(R.id.etNewComment)
        val addCommentButton = findViewById<MaterialButton>(R.id.btnAddComment)
        commentsController = EventCommentsController(
            this, event.id, userId, authorName, detailsPolicy, commentInput, addCommentButton,
            findViewById<ProgressBar>(R.id.progressComments),
            findViewById<LinearLayout>(R.id.commentsContainer),
            findViewById(R.id.tvEmptyComments),
            findViewById(R.id.tvCommentsError)
        )
        addCommentButton.setOnClickListener { commentsController.add() }

        mediaController = EventMediaController(this, event.id, userId, authorName, detailsPolicy)
        mediaController.bind()
    }

    private fun observeDetailsState() {
        detailsViewModel.uiState.observe(this) { state ->
            registrationButton.text = state.registrationText
            registrationButton.isEnabled = state.registrationEnabled
            favoriteButton.text = state.favoriteText
            favoriteButton.isEnabled = state.favoriteEnabled
            ratingSummary.text = state.ratingSummary
            ratingEligibility.text = state.ratingEligibility
            ratingBar.rating = state.rating
            ratingBar.isEnabled = state.ratingEnabled
            saveRatingButton.text = state.ratingButtonText
            saveRatingButton.isEnabled = state.ratingButtonEnabled
        }
        detailsViewModel.message.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::mediaController.isInitialized) mediaController.release()
        super.onDestroy()
    }

    private fun timestampFromExtras(secondsKey: String, nanosecondsKey: String): Timestamp? {
        val seconds = intent.getLongExtra(secondsKey, NO_TIMESTAMP)
        return if (seconds == NO_TIMESTAMP) null
        else Timestamp(seconds, intent.getIntExtra(nanosecondsKey, 0))
    }

    private fun coordinateFromExtra(key: String): Double? =
        intent.getDoubleExtra(key, Double.NaN).takeUnless(Double::isNaN)

    private fun navigateToLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "extra_event_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_LATITUDE = "extra_latitude"
        private const val EXTRA_LONGITUDE = "extra_longitude"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_STARTS_AT_SECONDS = "extra_starts_at_seconds"
        private const val EXTRA_STARTS_AT_NANOSECONDS = "extra_starts_at_nanoseconds"
        private const val EXTRA_ENDS_AT_SECONDS = "extra_ends_at_seconds"
        private const val EXTRA_ENDS_AT_NANOSECONDS = "extra_ends_at_nanoseconds"
        private const val NO_TIMESTAMP = Long.MIN_VALUE

        fun createIntent(context: Context, event: CampusEvent): Intent =
            Intent(context, EventDetailsActivity::class.java).apply {
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_TITLE, event.title)
                putExtra(EXTRA_DESCRIPTION, event.description)
                putExtra(EXTRA_DATE, event.date)
                putExtra(EXTRA_LOCATION, event.location)
                putExtra(EXTRA_ADDRESS, event.address)
                putExtra(EXTRA_LATITUDE, event.latitude ?: Double.NaN)
                putExtra(EXTRA_LONGITUDE, event.longitude ?: Double.NaN)
                putExtra(EXTRA_CATEGORY, event.category)
                putExtra(EXTRA_STARTS_AT_SECONDS, event.startsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_STARTS_AT_NANOSECONDS, event.startsAt?.nanoseconds ?: 0)
                putExtra(EXTRA_ENDS_AT_SECONDS, event.endsAt?.seconds ?: NO_TIMESTAMP)
                putExtra(EXTRA_ENDS_AT_NANOSECONDS, event.endsAt?.nanoseconds ?: 0)
            }
    }
}
