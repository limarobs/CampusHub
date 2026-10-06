package br.com.uri.campushub.feature.events.model

import com.google.firebase.Timestamp

data class EventComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: Timestamp?
)
