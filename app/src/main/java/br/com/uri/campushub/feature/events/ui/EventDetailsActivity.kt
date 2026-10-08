package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.auth.ui.MainActivity
import br.com.uri.campushub.feature.events.data.EventCommentRepository
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.data.EventRatingRepository
import br.com.uri.campushub.feature.events.model.EventComment
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.text.DateFormat
import java.util.Date

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var registrationButton: MaterialButton
    private lateinit var favoriteButton: MaterialButton
    private lateinit var ratingBar: RatingBar
    private lateinit var saveRatingButton: MaterialButton
    private lateinit var ratingSummary: TextView
    private lateinit var ratingEligibility: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var addCommentButton: MaterialButton
    private lateinit var commentsProgress: ProgressBar
    private lateinit var commentsContainer: LinearLayout
    private lateinit var emptyCommentsMessage: TextView
    private lateinit var commentsErrorMessage: TextView
    private lateinit var authorName: String
    private lateinit var userId: String
    private lateinit var event: CampusEvent
    private var isRegistered = false
    private var isRegistrationStateLoaded = false
    private var isFavorite = false
    private var isFavoriteStateLoaded = false
    private var isUserRatingLoaded = false

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

        ratingBar = findViewById(R.id.ratingBar)
        saveRatingButton = findViewById(R.id.btnSaveRating)
        ratingSummary = findViewById(R.id.tvRatingSummary)
        ratingEligibility = findViewById(R.id.tvRatingEligibility)
        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.setOnClickListener { saveRating() }

        commentInput = findViewById(R.id.etNewComment)
        addCommentButton = findViewById(R.id.btnAddComment)
        commentsProgress = findViewById(R.id.progressComments)
        commentsContainer = findViewById(R.id.commentsContainer)
        emptyCommentsMessage = findViewById(R.id.tvEmptyComments)
        commentsErrorMessage = findViewById(R.id.tvCommentsError)
        addCommentButton.setOnClickListener { addComment() }

        loadRegistrationState()
        loadFavoriteState()
        loadRatingsSummary()
        loadComments()
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
                    updateRatingEligibility()
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
                updateRatingEligibility()
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
                updateRatingEligibility()
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
                updateRatingEligibility()
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
                    "Não foi possível verificar seus favoritos.",
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
                    "Não foi possível adicionar aos favoritos.",
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
                    "Não foi possível remover dos favoritos.",
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

    private fun loadRatingsSummary() {
        ratingSummary.text = "Carregando avaliações..."
        EventRatingRepository.getRatings(event.id).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                ratingSummary.text = "Não foi possível carregar as avaliações."
                return@addOnCompleteListener
            }

            val ratings = task.result?.documents.orEmpty()
                .mapNotNull { document -> document.getLong("rating")?.toInt() }
                .filter { rating -> rating in 1..5 }

            ratingSummary.text = if (ratings.isEmpty()) {
                "Ainda não há avaliações para este evento."
            } else {
                val average = ratings.average()
                val countText = if (ratings.size == 1) "avaliação" else "avaliações"
                "Média: %.1f de 5 (%d %s)".format(average, ratings.size, countText)
            }
        }
    }

    private fun updateRatingEligibility() {
        if (!hasEventEnded()) {
            ratingEligibility.text = "As avaliações ficam disponíveis após o encerramento do evento."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        if (!isRegistrationStateLoaded) {
            ratingEligibility.text = "Verificando sua inscrição para liberar a avaliação..."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        if (!isRegistered) {
            ratingEligibility.text = "Apenas alunos inscritos podem avaliar este evento."
            ratingBar.isEnabled = false
            saveRatingButton.isEnabled = false
            return
        }

        ratingEligibility.text = "Escolha uma nota de 1 a 5."
        loadUserRating()
    }

    private fun loadUserRating() {
        isUserRatingLoaded = false
        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.text = "Carregando sua avaliação..."

        EventRatingRepository.getRating(event.id, userId).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                ratingEligibility.text = "Não foi possível verificar sua avaliação."
                saveRatingButton.text = "Tentar novamente"
                saveRatingButton.isEnabled = true
                saveRatingButton.setOnClickListener { loadUserRating() }
                return@addOnCompleteListener
            }

            val currentRating = task.result?.getLong("rating")?.toInt()
            if (currentRating != null && currentRating in 1..5) {
                ratingBar.rating = currentRating?.toFloat() ?: 0f
                saveRatingButton.text = "Atualizar avaliação"
            } else {
                ratingBar.rating = 0f
                saveRatingButton.text = "Publicar avaliação"
            }

            isUserRatingLoaded = true
            ratingBar.isEnabled = true
            saveRatingButton.isEnabled = true
            saveRatingButton.setOnClickListener { saveRating() }
        }
    }

    private fun saveRating() {
        if (!isUserRatingLoaded) {
            loadUserRating()
            return
        }

        val rating = ratingBar.rating.toInt()
        if (rating !in 1..5) {
            Toast.makeText(this, "Escolha uma nota de 1 a 5.", Toast.LENGTH_SHORT).show()
            return
        }

        ratingBar.isEnabled = false
        saveRatingButton.isEnabled = false
        saveRatingButton.text = "Salvando avaliação..."
        EventRatingRepository.saveRating(event.id, userId, rating).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Avaliação salva.", Toast.LENGTH_SHORT).show()
                loadRatingsSummary()
                loadUserRating()
            } else {
                Toast.makeText(
                    this,
                    "Não foi possível salvar sua avaliação.",
                    Toast.LENGTH_SHORT
                ).show()
                loadUserRating()
            }
        }
    }

    private fun hasEventEnded(): Boolean {
        return event.endsAt?.toDate()?.before(Date()) == true
    }

    private fun loadComments() {
        commentsProgress.visibility = View.VISIBLE
        commentsContainer.removeAllViews()
        emptyCommentsMessage.visibility = View.GONE
        commentsErrorMessage.visibility = View.GONE

        EventCommentRepository.getComments(event.id).addOnCompleteListener { task ->
            commentsProgress.visibility = View.GONE

            if (!task.isSuccessful) {
                commentsErrorMessage.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            val comments = task.result?.documents.orEmpty().map(::toEventComment)
            if (comments.isEmpty()) {
                emptyCommentsMessage.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            comments.forEach(::addCommentCard)
        }
    }

    private fun addComment() {
        val content = commentInput.text?.toString()?.trim().orEmpty()
        if (content.isBlank()) {
            commentInput.error = "Digite um comentário antes de publicar."
            return
        }

        addCommentButton.isEnabled = false
        addCommentButton.text = "Publicando comentário..."
        EventCommentRepository.addComment(event.id, userId, authorName, content)
            .addOnCompleteListener { task ->
                addCommentButton.isEnabled = true
                addCommentButton.text = "Publicar comentário"

                if (task.isSuccessful) {
                    commentInput.text?.clear()
                    loadComments()
                } else {
                    Toast.makeText(
                        this,
                        "Não foi possível publicar o comentário.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun toEventComment(document: DocumentSnapshot): EventComment {
        return EventComment(
            id = document.id,
            authorId = document.getString("authorId").orEmpty(),
            authorName = document.getString("authorName") ?: "Aluno",
            content = document.getString("content").orEmpty(),
            createdAt = document.getTimestamp("createdAt")
        )
    }

    private fun addCommentCard(comment: EventComment) {
        val card = layoutInflater.inflate(R.layout.item_event_comment, commentsContainer, false)
        card.findViewById<TextView>(R.id.tvCommentAuthor).text = comment.authorName
        card.findViewById<TextView>(R.id.tvCommentDate).text = formatCommentDate(comment.createdAt)
        card.findViewById<TextView>(R.id.tvCommentContent).text = comment.content

        if (comment.authorId == userId) {
            val ownerActions = card.findViewById<LinearLayout>(R.id.commentOwnerActions)
            ownerActions.visibility = View.VISIBLE
            card.findViewById<MaterialButton>(R.id.btnEditComment).setOnClickListener {
                showEditCommentDialog(comment)
            }
            card.findViewById<MaterialButton>(R.id.btnDeleteComment).setOnClickListener {
                showDeleteCommentDialog(comment)
            }
        }

        commentsContainer.addView(card)
    }

    private fun showEditCommentDialog(comment: EventComment) {
        val editText = EditText(this).apply {
            setText(comment.content)
            setSelectAllOnFocus(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(48, 0, 48, 0)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(editText)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val content = editText.text.toString().trim()
                if (content.isBlank()) {
                    Toast.makeText(this, "O comentário não pode ficar vazio.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                EventCommentRepository.updateComment(event.id, comment.id, content)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            loadComments()
                        } else {
                            Toast.makeText(
                                this,
                                "Não foi possível editar o comentário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .show()
    }

    private fun showDeleteCommentDialog(comment: EventComment) {
        AlertDialog.Builder(this)
            .setTitle("Excluir comentário?")
            .setMessage("Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                EventCommentRepository.deleteComment(event.id, comment.id)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            loadComments()
                        } else {
                            Toast.makeText(
                                this,
                                "Não foi possível excluir o comentário.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .show()
    }

    private fun formatCommentDate(timestamp: Timestamp?): String {
        if (timestamp == null) {
            return "Agora"
        }

        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(timestamp.toDate().time))
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
