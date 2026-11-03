package br.com.uri.campushub.feature.events.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDetailsPolicyTest {

    private val policy = EventDetailsPolicy()

    @Test
    fun `shows enabled registration for an upcoming event`() {
        val control = policy.registrationControl(isRegistered = false, hasEventEnded = false)

        assertEquals("Inscrever-se", control.text)
        assertTrue(control.enabled)
    }

    @Test
    fun `blocks registration for an ended event`() {
        val control = policy.registrationControl(isRegistered = false, hasEventEnded = true)

        assertEquals("Evento encerrado", control.text)
        assertFalse(control.enabled)
    }

    @Test
    fun `keeps cancellation available after the event ends`() {
        val control = policy.registrationControl(isRegistered = true, hasEventEnded = true)

        assertEquals("Cancelar inscrição", control.text)
        assertTrue(control.enabled)
    }

    @Test
    fun `shows the correct favorite action for each state`() {
        assertEquals("Adicionar aos favoritos", policy.favoriteButtonText(isFavorite = false))
        assertEquals("Remover dos favoritos", policy.favoriteButtonText(isFavorite = true))
    }

    @Test
    fun `blocks ratings before the event has ended`() {
        val eligibility = policy.ratingEligibility(
            hasEventEnded = false,
            isRegistrationLoaded = true,
            isRegistered = true
        )

        assertFalse(eligibility.canRate)
        assertEquals("As avaliações ficam disponíveis após o encerramento do evento.", eligibility.text)
    }

    @Test
    fun `waits for registration state before enabling ratings`() {
        val eligibility = policy.ratingEligibility(
            hasEventEnded = true,
            isRegistrationLoaded = false,
            isRegistered = false
        )

        assertFalse(eligibility.canRate)
        assertEquals("Verificando sua inscrição para liberar a avaliação...", eligibility.text)
    }

    @Test
    fun `blocks ratings for a student who is not registered`() {
        val eligibility = policy.ratingEligibility(
            hasEventEnded = true,
            isRegistrationLoaded = true,
            isRegistered = false
        )

        assertFalse(eligibility.canRate)
        assertEquals("Apenas alunos inscritos podem avaliar este evento.", eligibility.text)
    }

    @Test
    fun `allows ratings for registered students after the event ends`() {
        val eligibility = policy.ratingEligibility(
            hasEventEnded = true,
            isRegistrationLoaded = true,
            isRegistered = true
        )

        assertTrue(eligibility.canRate)
        assertEquals("Escolha uma nota de 1 a 5.", eligibility.text)
    }

    @Test
    fun `accepts only ratings from one to five`() {
        assertFalse(policy.isValidRating(0))
        assertTrue(policy.isValidRating(1))
        assertTrue(policy.isValidRating(5))
        assertFalse(policy.isValidRating(6))
    }
}
