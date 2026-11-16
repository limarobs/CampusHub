package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

data class EventDraft(
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val address: String,
    val startsAt: Timestamp,
    val endsAt: Timestamp?,
    val capacity: Int,
    val coverImageUrl: String
) {
    val date: String
        get() = DATE_OUTPUT_FORMATTER.format(startsAt.toLocalDateTime())

    fun toFirestoreData(organizerId: String, includeOrganizer: Boolean): Map<String, Any?> =
        buildMap {
            put("title", title)
            put("description", description)
            put("date", date)
            put("location", location)
            put("address", address)
            put("category", category)
            put("startsAt", startsAt)
            put("endsAt", endsAt)
            put("capacity", capacity)
            put("coverImageUrl", coverImageUrl)
            if (includeOrganizer) {
                put("organizerId", organizerId)
                put("status", EventStatus.DRAFT.firestoreValue)
                put("confirmedCount", 0)
                put("waitlistCount", 0)
            }
        }

    companion object {
        val DATE_INPUT_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

        private val DATE_OUTPUT_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))

        fun parseDate(value: String): Timestamp =
            LocalDateTime.parse(value.trim(), DATE_INPUT_FORMATTER).toTimestamp()

        fun formatDate(value: Timestamp?): String =
            value?.toLocalDateTime()?.let(DATE_INPUT_FORMATTER::format).orEmpty()

        private fun LocalDateTime.toTimestamp(): Timestamp =
            Timestamp(Date.from(atZone(ZoneId.systemDefault()).toInstant()))

        private fun Timestamp.toLocalDateTime(): LocalDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanoseconds.toLong()), ZoneId.systemDefault())
    }
}
