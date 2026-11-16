package br.com.uri.campushub.feature.events.model

enum class EventStatus(val firestoreValue: String) {
    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED"),
    CLOSED("CLOSED");

    companion object {
        fun fromFirestore(value: String?): EventStatus =
            entries.firstOrNull { it.firestoreValue == value } ?: PUBLISHED
    }
}
