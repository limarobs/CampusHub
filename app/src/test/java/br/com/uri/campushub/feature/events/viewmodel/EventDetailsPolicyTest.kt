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

    @Test
    fun `trims a valid comment before publishing`() {
        assertEquals("Comentario", policy.commentContent("  Comentario  "))
    }

    @Test
    fun `rejects empty comments`() {
        assertEquals(null, policy.commentContent(null))
        assertEquals(null, policy.commentContent("   \n\t  "))
    }

    @Test
    fun `allows publication management only for its author`() {
        assertTrue(policy.canManagePublication("student-1", "student-1"))
        assertFalse(policy.canManagePublication("student-1", "student-2"))
    }

    @Test
    fun `accepts coordinates within map limits including boundaries`() {
        assertTrue(policy.hasValidCoordinates(-90.0, -180.0))
        assertTrue(policy.hasValidCoordinates(90.0, 180.0))
    }

    @Test
    fun `rejects missing or out of range coordinates`() {
        assertFalse(policy.hasValidCoordinates(null, 0.0))
        assertFalse(policy.hasValidCoordinates(0.0, null))
        assertFalse(policy.hasValidCoordinates(90.1, 0.0))
        assertFalse(policy.hasValidCoordinates(0.0, -180.1))
    }

    @Test
    fun `formats audio elapsed time`() {
        assertEquals("0:00", policy.formatAudioTime(0))
        assertEquals("0:01", policy.formatAudioTime(1_999))
        assertEquals("1:01", policy.formatAudioTime(61_000))
        assertEquals("0:00", policy.formatAudioTime(-1))
    }
}
