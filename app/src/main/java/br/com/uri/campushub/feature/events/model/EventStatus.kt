package br.com.uri.campushub.feature.events.model

enum class EventStatus(val firestoreValue: String, val label: String) {
    DRAFT("DRAFT", "Rascunho"),
    PUBLISHED("PUBLISHED", "Publicado"),
    CLOSED("CLOSED", "Encerrado");

    companion object {
        fun fromFirestore(value: String?): EventStatus =
            entries.firstOrNull { it.firestoreValue == value } ?: PUBLISHED
    }
}
