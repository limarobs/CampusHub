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
}
