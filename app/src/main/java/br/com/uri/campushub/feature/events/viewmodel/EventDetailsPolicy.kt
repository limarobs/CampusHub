package br.com.uri.campushub.feature.events.viewmodel

data class RegistrationControl(
    val text: String,
    val enabled: Boolean
)

data class RatingEligibility(
    val text: String,
    val canRate: Boolean
)

class EventDetailsPolicy {

    fun registrationControl(isRegistered: Boolean, hasEventEnded: Boolean): RegistrationControl {
        return when {
            isRegistered -> RegistrationControl("Cancelar inscrição", true)
            hasEventEnded -> RegistrationControl("Evento encerrado", false)
            else -> RegistrationControl("Inscrever-se", true)
        }
    }

    fun favoriteButtonText(isFavorite: Boolean): String {
        return if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos"
    }

    fun ratingEligibility(
        hasEventEnded: Boolean,
        isRegistrationLoaded: Boolean,
        isRegistered: Boolean
    ): RatingEligibility {
        return when {
            !hasEventEnded -> RatingEligibility(
                "As avaliações ficam disponíveis após o encerramento do evento.",
                false
            )
            !isRegistrationLoaded -> RatingEligibility(
                "Verificando sua inscrição para liberar a avaliação...",
                false
            )
            !isRegistered -> RatingEligibility(
                "Apenas alunos inscritos podem avaliar este evento.",
                false
            )
            else -> RatingEligibility("Escolha uma nota de 1 a 5.", true)
        }
    }

    fun isValidRating(rating: Int): Boolean = rating in 1..5

    fun commentContent(content: CharSequence?): String? {
        return content?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun canManagePublication(authorId: String, currentUserId: String): Boolean {
        return authorId == currentUserId
    }

    fun hasValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
        return latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    fun formatAudioTime(milliseconds: Long): String {
        val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
