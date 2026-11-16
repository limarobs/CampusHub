package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = "Sem categoria",
    val startsAt: Timestamp? = null,
    val endsAt: Timestamp? = null,
    val organizerId: String = "",
    val status: EventStatus = EventStatus.PUBLISHED,
    val capacity: Int? = null,
    val confirmedCount: Int = 0,
    val waitlistCount: Int = 0,
    val coverImageUrl: String = ""
) {
    val remainingCapacity: Int?
        get() = capacity?.let { (it - confirmedCount).coerceAtLeast(0) }

    val isFull: Boolean
        get() = remainingCapacity == 0
}
