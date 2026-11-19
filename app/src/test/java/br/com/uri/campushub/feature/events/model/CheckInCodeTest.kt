package br.com.uri.campushub.feature.events.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInCodeTest {

    @Test
    fun `creates and parses a QR payload for the expected event`() {
        val payload = CheckInCode.createQrPayload(
            eventId = "event-123",
            attendeeId = "student-456",
            token = "86ecebc8-1234-4321-abcd-123456789abc"
        )

        val result = CheckInCode.parseQrPayload(payload, expectedEventId = "event-123")

        assertEquals("event-123", result?.eventId)
        assertEquals("student-456", result?.attendeeId)
        assertEquals("86ecebc8-1234-4321-abcd-123456789abc", result?.token)
    }

    @Test
    fun `rejects a QR payload from another event`() {
        val payload = CheckInCode.createQrPayload("event-a", "student-456", "token-123")

        assertNull(CheckInCode.parseQrPayload(payload, expectedEventId = "event-b"))
    }

    @Test
    fun `rejects malformed QR payloads`() {
        assertNull(CheckInCode.parseQrPayload("CAMPUSHUB_CHECKIN|event-123|student-456", "event-123"))
        assertNull(CheckInCode.parseQrPayload("OTHER|event-123|student-456|token-123", "event-123"))
        assertNull(CheckInCode.parseQrPayload("CAMPUSHUB_CHECKIN|event-123||token-123", "event-123"))
    }

    @Test
    fun `creates a stable eight character manual code from the token`() {
        assertEquals(
            "86ECEBC8",
            CheckInCode.manualCodeFromToken("86ecebc8-1234-4321-abcd-123456789abc")
        )
    }

    @Test
    fun `normalizes and validates a manual code`() {
        assertEquals("86ECEBC8", CheckInCode.normalizeManualCode(" 86ecebc8 "))
        assertTrue(CheckInCode.isValidManualCode(" 86ecebc8 "))
        assertFalse(CheckInCode.isValidManualCode("86ECEBC"))
        assertFalse(CheckInCode.isValidManualCode("86ECEBCG"))
    }
}
