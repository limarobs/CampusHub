package br.com.uri.campushub.feature.events.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDraftValidatorTest {
    private val validator = EventDraftValidator()

    @Test
    fun `creates a draft from valid organizer input`() {
        val result = validator.validate(validInput())

        assertTrue(result.isValid)
        assertEquals(80, result.draft?.capacity)
        assertEquals("Semana Acadêmica", result.draft?.title)
    }

    @Test
    fun `rejects an invalid public image url`() {
        val result = validator.validate(validInput(coverImageUrl = "imagem-local.png"))

        assertFalse(result.isValid)
        assertEquals("Informe uma URL pública de imagem válida.", result.coverImageUrlError)
    }

    @Test
    fun `rejects an end time before the start`() {
        val result = validator.validate(validInput(endsAt = "18/11/2026 08:00"))

        assertFalse(result.isValid)
        assertEquals("O encerramento deve ser posterior ao início.", result.endsAtError)
    }

    @Test
    fun `requires a positive capacity`() {
        val result = validator.validate(validInput(capacity = "0"))

        assertFalse(result.isValid)
        assertEquals("Informe uma capacidade maior que zero.", result.capacityError)
    }

    private fun validInput(
        endsAt: String = "18/11/2026 18:00",
        capacity: String = "80",
        coverImageUrl: String = "https://images.example.com/semana-academica.jpg"
    ) = EventDraftInput(
        title = "Semana Acadêmica",
        description = "Palestras e oficinas.",
        category = "Acadêmico",
        location = "Auditório Central",
        address = "Rua Universidade, 100",
        startsAt = "18/11/2026 09:00",
        endsAt = endsAt,
        capacity = capacity,
        coverImageUrl = coverImageUrl
    )
}
