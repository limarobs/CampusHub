package br.com.uri.campushub.feature.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRatingRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.model.CampusEvent

data class EventDetailsUiState(
    val registrationText: String = "Verificando inscrição...",
    val registrationEnabled: Boolean = false,
    val favoriteText: String = "Verificando favoritos...",
    val favoriteEnabled: Boolean = false,
    val ratingSummary: String = "Carregando avaliações...",
    val ratingEligibility: String = "Verificando sua inscrição para liberar a avaliação...",
    val rating: Float = 0f,
    val ratingEnabled: Boolean = false,
    val ratingButtonText: String = "Carregando sua avaliação...",
    val ratingButtonEnabled: Boolean = false
)

class EventDetailsViewModel : ViewModel() {

    private val _uiState = MutableLiveData(EventDetailsUiState())
    val uiState: LiveData<EventDetailsUiState> = _uiState

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private lateinit var userId: String
    private lateinit var event: CampusEvent
    private var isRegistered = false
    private var isRegistrationLoaded = false
    private var isFavorite = false
    private var isFavoriteLoaded = false
    private var isUserRatingLoaded = false
    private var isLoadingUserRating = false

    fun start(userId: String, event: CampusEvent) {
        this.userId = userId
        this.event = event
        loadRegistration()
        loadFavorite()
        loadRatingsSummary()
    }

    fun toggleRegistration() {
        if (!isRegistrationLoaded) loadRegistration()
        else if (isRegistered) cancelRegistration() else register()
    }

    fun toggleFavorite() {
        if (!isFavoriteLoaded) loadFavorite()
        else if (isFavorite) removeFavorite() else addFavorite()
    }

    fun saveRating(rating: Int) {
        if (!isUserRatingLoaded) {
            loadUserRating()
            return
        }
        if (rating !in 1..5) {
            notify("Escolha uma nota de 1 a 5.")
            return
        }

        publish { copy(ratingEnabled = false, ratingButtonEnabled = false, ratingButtonText = "Salvando avaliação...") }
        EventRatingRepository.saveRating(event.id, userId, rating).addOnCompleteListener { task ->
            if (task.isSuccessful) notify("Avaliação salva.") else notify("Não foi possível salvar a avaliação.")
            isUserRatingLoaded = false
            loadRatingsSummary()
            loadUserRating()
        }
    }

    private fun loadRegistration() {
        isRegistrationLoaded = false
        publish { copy(registrationEnabled = false, registrationText = "Verificando inscrição...") }
        EventRegistrationRepository.getRegistration(userId, event.id).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                publish { copy(registrationEnabled = true, registrationText = "Tentar novamente") }
                notify("Não foi possível verificar sua inscrição.")
                updateRatingEligibility()
                return@addOnCompleteListener
            }
            isRegistered = task.result?.exists() == true
            isRegistrationLoaded = true
            publish {
                copy(
                    registrationEnabled = true,
                    registrationText = if (isRegistered) "Cancelar inscrição" else "Inscrever-se"
                )
            }
            updateRatingEligibility()
        }
    }

    private fun register() {
        publish { copy(registrationEnabled = false, registrationText = "Inscrevendo...") }
        EventRegistrationRepository.register(userId, event).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isRegistered = true
                notify("Inscrição realizada.")
                publish { copy(registrationEnabled = true, registrationText = "Cancelar inscrição") }
                updateRatingEligibility()
            } else {
                publish { copy(registrationEnabled = true, registrationText = "Inscrever-se") }
                notify("Não foi possível realizar a inscrição.")
            }
        }
    }

    private fun cancelRegistration() {
        publish { copy(registrationEnabled = false, registrationText = "Cancelando inscrição...") }
        EventRegistrationRepository.cancel(userId, event.id).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isRegistered = false
                notify("Inscrição cancelada.")
                publish { copy(registrationEnabled = true, registrationText = "Inscrever-se") }
                updateRatingEligibility()
            } else {
                publish { copy(registrationEnabled = true, registrationText = "Cancelar inscrição") }
                notify("Não foi possível cancelar a inscrição.")
            }
        }
    }

    private fun loadFavorite() {
        isFavoriteLoaded = false
        publish { copy(favoriteEnabled = false, favoriteText = "Verificando favoritos...") }
        EventFavoriteRepository.getFavorite(userId, event.id).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                publish { copy(favoriteEnabled = true, favoriteText = "Tentar novamente") }
                notify("Não foi possível verificar seus favoritos.")
                return@addOnCompleteListener
            }
            isFavorite = task.result?.exists() == true
            isFavoriteLoaded = true
            publish {
                copy(
                    favoriteEnabled = true,
                    favoriteText = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos"
                )
            }
        }
    }

    private fun addFavorite() {
        publish { copy(favoriteEnabled = false, favoriteText = "Adicionando aos favoritos...") }
        EventFavoriteRepository.favorite(userId, event).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isFavorite = true
                notify("Evento adicionado aos favoritos.")
                publish { copy(favoriteEnabled = true, favoriteText = "Remover dos favoritos") }
            } else {
                publish { copy(favoriteEnabled = true, favoriteText = "Adicionar aos favoritos") }
                notify("Não foi possível adicionar aos favoritos.")
            }
        }
    }

    private fun removeFavorite() {
        publish { copy(favoriteEnabled = false, favoriteText = "Removendo dos favoritos...") }
        EventFavoriteRepository.unfavorite(userId, event.id).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isFavorite = false
                notify("Evento removido dos favoritos.")
                publish { copy(favoriteEnabled = true, favoriteText = "Adicionar aos favoritos") }
            } else {
                publish { copy(favoriteEnabled = true, favoriteText = "Remover dos favoritos") }
                notify("Não foi possível remover dos favoritos.")
            }
        }
    }

    private fun loadRatingsSummary() {
        publish { copy(ratingSummary = "Carregando avaliações...") }
        EventRatingRepository.getRatings(event.id).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                publish { copy(ratingSummary = "Não foi possível carregar as avaliações.") }
                return@addOnCompleteListener
            }
            val ratings = task.result?.documents.orEmpty()
                .mapNotNull { it.getLong("rating")?.toInt() }
                .filter { it in 1..5 }
            val summary = if (ratings.isEmpty()) "Ainda não há avaliações para este evento."
            else "Média: %.1f de 5 (%d %s)".format(
                ratings.average(), ratings.size, if (ratings.size == 1) "avaliação" else "avaliações"
            )
            publish { copy(ratingSummary = summary) }
        }
    }

    private fun updateRatingEligibility() {
        when {
            !hasEventEnded() -> publish {
                copy(
                    ratingEligibility = "As avaliações ficam disponíveis após o encerramento do evento.",
                    ratingEnabled = false,
                    ratingButtonEnabled = false
                )
            }
            !isRegistrationLoaded -> publish {
                copy(
                    ratingEligibility = "Verificando sua inscrição para liberar a avaliação...",
                    ratingEnabled = false,
                    ratingButtonEnabled = false
                )
            }
            !isRegistered -> publish {
                copy(
                    ratingEligibility = "Apenas alunos inscritos podem avaliar este evento.",
                    ratingEnabled = false,
                    ratingButtonEnabled = false
                )
            }
            else -> {
                publish { copy(ratingEligibility = "Escolha uma nota de 1 a 5.") }
                loadUserRating()
            }
        }
    }

    private fun loadUserRating() {
        if (isLoadingUserRating || !hasEventEnded() || !isRegistrationLoaded || !isRegistered) return
        isLoadingUserRating = true
        isUserRatingLoaded = false
        publish { copy(ratingEnabled = false, ratingButtonEnabled = false, ratingButtonText = "Carregando sua avaliação...") }
        EventRatingRepository.getRating(event.id, userId).addOnCompleteListener { task ->
            isLoadingUserRating = false
            if (!task.isSuccessful) {
                publish {
                    copy(
                        ratingEligibility = "Não foi possível verificar sua avaliação.",
                        ratingButtonText = "Tentar novamente",
                        ratingButtonEnabled = true
                    )
                }
                return@addOnCompleteListener
            }
            val rating = task.result?.getLong("rating")?.toInt()?.takeIf { it in 1..5 } ?: 0
            isUserRatingLoaded = true
            publish {
                copy(
                    rating = rating.toFloat(),
                    ratingEnabled = true,
                    ratingButtonEnabled = true,
                    ratingButtonText = if (rating == 0) "Publicar avaliação" else "Atualizar avaliação"
                )
            }
        }
    }

    private fun publish(change: EventDetailsUiState.() -> EventDetailsUiState) {
        _uiState.value = change(_uiState.value ?: EventDetailsUiState())
    }

    private fun notify(text: String) {
        _message.value = text
    }

    private fun hasEventEnded() = event.endsAt?.toDate()?.before(java.util.Date()) == true
}
