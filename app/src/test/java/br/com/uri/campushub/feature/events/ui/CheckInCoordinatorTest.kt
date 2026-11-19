package br.com.uri.campushub.feature.events.ui

import br.com.uri.campushub.feature.events.model.CheckInCode
import br.com.uri.campushub.feature.events.model.EventRegistration
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckInCoordinatorTest {
    private val coordinator = CheckInCoordinator()

    @Test
    fun `creates a check-in candidate only for a QR from the current event`() {
        val payload = CheckInCode.createQrPayload("event-1", "student-1", "token-1")

        val candidate = coordinator.candidateFromQr(payload, "event-1")

        assertEquals("student-1", candidate?.attendeeId)
        assertEquals("token-1", candidate?.qrToken)
        assertNull(coordinator.candidateFromQr(payload, "event-2"))
    }

    @Test
    fun `finds only a confirmed registration for a manual code`() {
        val token = "86ecebc8-1234-4321-abcd-123456789abc"
        val registrations = listOf(
            EventRegistration("event-1", "waitlisted", status = RegistrationStatus.WAITLISTED, qrToken = token),
            EventRegistration("event-1", "confirmed", status = RegistrationStatus.CONFIRMED, qrToken = token)
        )

        val candidate = coordinator.candidateFromManualCode("86ECEBC8", registrations)

        assertEquals("confirmed", candidate?.attendeeId)
        assertEquals(token, candidate?.qrToken)
    }

    @Test
    fun `rejects an invalid manual code before querying registrations`() {
        assertNull(coordinator.manualCode("not-a-code"))
        assertEquals("86ECEBC8", coordinator.manualCode(" 86ecebc8 "))
    }
}
