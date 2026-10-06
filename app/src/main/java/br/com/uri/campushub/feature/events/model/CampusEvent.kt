package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val category: String = "Sem categoria",
    val startsAt: Timestamp? = null,
    val endsAt: Timestamp? = null
)
