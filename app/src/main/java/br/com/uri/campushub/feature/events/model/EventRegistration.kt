package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp

data class EventRegistration(
    val eventId: String,
    val attendeeId: String,
    val attendeeName: String = "",
    val attendeeEmail: String = "",
    val status: RegistrationStatus = RegistrationStatus.CONFIRMED,
    val registeredAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,
    val waitlistedAt: Timestamp? = null,
    val checkedInAt: Timestamp? = null,
    val qrToken: String = ""
) {
    val canCheckIn: Boolean
        get() = status == RegistrationStatus.CONFIRMED && checkedInAt == null
}
