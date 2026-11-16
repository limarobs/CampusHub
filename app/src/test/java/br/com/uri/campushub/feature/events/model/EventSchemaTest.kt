package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class EventSchemaTest {

    @Test
    fun `keeps legacy events published and without a capacity limit`() {
        val event = event()

        assertEquals(EventStatus.PUBLISHED, event.status)
        assertNull(event.remainingCapacity)
        assertFalse(event.isFull)
    }

    @Test
    fun `calculates available capacity without returning a negative value`() {
        val event = event(capacity = 10, confirmedCount = 13)

        assertEquals(0, event.remainingCapacity)
        assertTrue(event.isFull)
    }

    @Test
    fun `maps unrecognized event statuses to published for compatibility`() {
        assertEquals(EventStatus.PUBLISHED, EventStatus.fromFirestore("ARCHIVED"))
    }

    @Test
    fun `only allows an unchecked confirmed registration to check in`() {
        val confirmed = registration()
        val waitlisted = registration(status = RegistrationStatus.WAITLISTED)
        val checkedIn = registration(checkedInAt = Timestamp(Date()))

        assertTrue(confirmed.canCheckIn)
        assertFalse(waitlisted.canCheckIn)
        assertFalse(checkedIn.canCheckIn)
    }

    @Test
    fun `maps unrecognized registration statuses to confirmed for legacy records`() {
        assertEquals(RegistrationStatus.CONFIRMED, RegistrationStatus.fromFirestore(null))
    }

    private fun event(
        capacity: Int? = null,
        confirmedCount: Int = 0
    ) = CampusEvent(
        id = "event-1",
        title = "Semana Acadêmica",
        description = "Descrição",
        date = "18/11/2026",
        location = "URI",
        capacity = capacity,
        confirmedCount = confirmedCount
    )

    private fun registration(
        status: RegistrationStatus = RegistrationStatus.CONFIRMED,
        checkedInAt: Timestamp? = null
    ) = EventRegistration(
        eventId = "event-1",
        attendeeId = "student-1",
        status = status,
        checkedInAt = checkedInAt
    )
}
