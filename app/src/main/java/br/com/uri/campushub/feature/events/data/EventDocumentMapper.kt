package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.model.EventStatus
import br.com.uri.campushub.feature.events.model.EventRegistration
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toCampusEvent(): CampusEvent = CampusEvent(
    id = id,
    title = getString("title").orEmpty(),
    description = getString("description").orEmpty(),
    date = getString("date").orEmpty(),
    location = getString("location").orEmpty(),
    address = getString("address").orEmpty(),
    latitude = (get("latitude") as? Number)?.toDouble(),
    longitude = (get("longitude") as? Number)?.toDouble(),
    category = getString("category") ?: "Sem categoria",
    startsAt = getTimestamp("startsAt"),
    endsAt = getTimestamp("endsAt"),
    organizerId = getString("organizerId").orEmpty(),
    status = EventStatus.fromFirestore(getString("status")),
    capacity = (get("capacity") as? Number)?.toInt()?.takeIf { it > 0 },
    confirmedCount = ((get("confirmedCount") as? Number)?.toInt() ?: 0).coerceAtLeast(0),
    waitlistCount = ((get("waitlistCount") as? Number)?.toInt() ?: 0).coerceAtLeast(0),
    coverImageUrl = getString("coverImageUrl").orEmpty()
)

fun DocumentSnapshot.toEventRegistration(): EventRegistration = EventRegistration(
    eventId = getString("eventId") ?: id,
    attendeeId = getString("attendeeId").orEmpty(),
    attendeeName = getString("attendeeName").orEmpty(),
    attendeeEmail = getString("attendeeEmail").orEmpty(),
    status = RegistrationStatus.fromFirestore(getString("status")),
    registeredAt = getTimestamp("registeredAt"),
    confirmedAt = getTimestamp("confirmedAt"),
    waitlistedAt = getTimestamp("waitlistedAt"),
    checkedInAt = getTimestamp("checkedInAt"),
    qrToken = getString("qrToken").orEmpty()
)
