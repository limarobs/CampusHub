package br.com.uri.campushub.feature.events.model

enum class RegistrationStatus(val firestoreValue: String) {
    CONFIRMED("CONFIRMED"),
    WAITLISTED("WAITLISTED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromFirestore(value: String?): RegistrationStatus =
            entries.firstOrNull { it.firestoreValue == value } ?: CONFIRMED
    }
}
