package br.com.uri.campushub.feature.profile.model

enum class UserRole(
    val firestoreValue: String,
    val label: String
) {
    STUDENT("STUDENT", "Aluno"),
    ORGANIZER("ORGANIZER", "Organizador");

    companion object {
        fun fromFirestore(value: String?): UserRole =
            entries.firstOrNull { it.firestoreValue == value } ?: STUDENT
    }
}
